// Load test: expected traffic, held steady.
// Two scenarios run at once, because real traffic is mostly reads with a
// minority of writes — testing only writes gives a misleading number.
//
//   k6 run k6/load.js
import { check, sleep } from 'k6';
import http from 'k6/http';
import { CORRECTNESS_THRESHOLDS, LATENCY_THRESHOLDS } from './lib/config.js';
import * as api from './lib/client.js';
import { randomUser, changedUser } from './lib/data.js';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 404));

export const options = {
  scenarios: {
    // 2/3 of the traffic: people looking at data.
    readers: {
      executor: 'constant-vus',
      exec: 'readFlow',
      vus: 10,
      duration: '30s',
    },
    // 1/3: people changing data.
    writers: {
      executor: 'constant-vus',
      exec: 'writeFlow',
      vus: 5,
      duration: '30s',
    },
  },
  thresholds: {
    ...CORRECTNESS_THRESHOLDS,
    ...LATENCY_THRESHOLDS,
    // Per-operation budgets. The `op` tag comes from lib/client.js.
    'http_req_duration{op:create_user}': ['p(95)<300'],
    'http_req_duration{op:list_users}': ['p(95)<400'],
  },
};

export function readFlow() {
  check(api.listUsers(), {
    'list -> 200': (r) => r.status === 200,
  });
  sleep(1);
}

export function writeFlow() {
  const created = api.createUser(randomUser());
  check(created, { 'create -> 201': (r) => r.status === 201 });

  const id = api.idOf(created);
  if (id === null) {
    return;
  }

  check(api.getUser(id), { 'get -> 200': (r) => r.status === 200 });
  check(api.updateUser(id, changedUser()), { 'update -> 200': (r) => r.status === 200 });

  // Always clean up. The store is in-memory, so anything left behind makes
  // GET /users return a bigger body on every later iteration.
  check(api.deleteUser(id), { 'delete -> 204': (r) => r.status === 204 });

  sleep(1);
}
