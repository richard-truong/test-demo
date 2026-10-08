// Smoke test: does the whole CRUD cycle work at all?
// Tiny load, one virtual user. Run this first — if it fails, the other
// scripts only tell you the same thing, slower.
//
//   k6 run k6/smoke.js
import { check, sleep } from 'k6';
import http from 'k6/http';
import { CORRECTNESS_THRESHOLDS } from './lib/config.js';
import * as api from './lib/client.js';
import { randomUser, changedUser } from './lib/data.js';

// A 404 is the correct answer for "user does not exist". Without this, the
// deliberate 404 check below would count as a failed request and break the
// http_req_failed threshold.
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 404));

export const options = {
  vus: 1,
  duration: '10s',
  // Correctness only. Latency budgets live in load.js — see lib/config.js
  // for why a smoke run cannot measure them.
  thresholds: CORRECTNESS_THRESHOLDS,
};

export default function () {
  // --- CREATE ---------------------------------------------------------
  const created = api.createUser(randomUser());
  check(created, {
    'POST /users -> 201': (r) => r.status === 201,
    'POST /users returns an id': (r) => api.idOf(r) !== null,
    'POST /users echoes the name': (r) => r.json('name') !== undefined,
  });

  const id = api.idOf(created);
  if (id === null) {
    return; // nothing to clean up, every later call would 404
  }

  // --- READ -----------------------------------------------------------
  check(api.getUser(id), {
    'GET /users/{id} -> 200': (r) => r.status === 200,
    'GET /users/{id} returns the same id': (r) => r.json('id') === id,
  });

  // --- UPDATE ---------------------------------------------------------
  const changed = changedUser();
  check(api.updateUser(id, changed), {
    'PUT /users/{id} -> 200': (r) => r.status === 200,
    'PUT /users/{id} applies the new name': (r) => r.json('name') === changed.name,
  });

  // --- LIST -----------------------------------------------------------
  check(api.listUsers(), {
    'GET /users -> 200': (r) => r.status === 200,
    'GET /users returns an array': (r) => Array.isArray(r.json()),
  });

  // --- DELETE ---------------------------------------------------------
  check(api.deleteUser(id), {
    'DELETE /users/{id} -> 204': (r) => r.status === 204,
  });

  check(api.getUser(id), {
    'GET a deleted user -> 404': (r) => r.status === 404,
  });

  sleep(1);
}
