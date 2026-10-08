// Stress test: keep adding load in steps until something breaks.
// The goal is not to pass — it is to find the knee, the point where response
// time starts climbing faster than the number of users.
//
//   k6 run k6/stress.js
import { check, sleep } from 'k6';
import http from 'k6/http';
import * as api from './lib/client.js';
import { randomUser, changedUser } from './lib/data.js';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 404));

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 }, // step 1
        { duration: '1m', target: 20 }, // hold, so the numbers settle
        { duration: '30s', target: 50 }, // step 2
        { duration: '1m', target: 50 },
        { duration: '30s', target: 100 }, // step 3
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 }, // ramp down
      ],
      gracefulRampDown: '15s',
    },
  },
  // Deliberately looser than load.js: at high load we expect some degradation,
  // and a threshold here is a tripwire for "something is badly wrong", not a
  // performance target.
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.95'],
  },
};

export default function () {
  const created = api.createUser(randomUser());
  check(created, { 'create -> 201': (r) => r.status === 201 });

  const id = api.idOf(created);
  if (id === null) {
    return;
  }

  check(api.getUser(id), { 'get -> 200': (r) => r.status === 200 });
  check(api.listUsers(), { 'list -> 200': (r) => r.status === 200 });
  check(api.updateUser(id, changedUser()), { 'update -> 200': (r) => r.status === 200 });
  check(api.deleteUser(id), { 'delete -> 204': (r) => r.status === 204 });

  sleep(0.5);
}
