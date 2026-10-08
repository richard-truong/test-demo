// Spike test: traffic multiplies in seconds, then drops back.
// Answers two different questions — does the app survive the burst, and does
// it recover once the burst is over? The second one is what usually fails.
//
//   k6 run k6/spike.js
import { check, sleep } from 'k6';
import http from 'k6/http';
import * as api from './lib/client.js';
import { randomUser } from './lib/data.js';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 404));

export const options = {
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 5 }, // warm up
        { duration: '10s', target: 200 }, // the spike
        { duration: '30s', target: 200 }, // hold it
        { duration: '10s', target: 5 }, // drop back
        { duration: '30s', target: 5 }, // recovery: watch p95 fall again
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    // Read-only flow, so this can be tighter than the stress test.
    'http_req_duration{op:list_users}': ['p(95)<800'],
  },
};

export default function () {
  check(api.listUsers(), { 'list -> 200': (r) => r.status === 200 });

  const created = api.createUser(randomUser());
  check(created, { 'create -> 201': (r) => r.status === 201 });

  // Delete it again. If the spike left thousands of users behind, every
  // list call would return a bigger body than the one before it, and the
  // recovery phase would be measuring that growth instead of recovery.
  const id = api.idOf(created);
  if (id !== null) {
    check(api.deleteUser(id), { 'delete -> 204': (r) => r.status === 204 });
  }

  sleep(1);
}
