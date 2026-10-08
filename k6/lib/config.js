// Shared settings for every k6 script.
// Override the target with:  k6 run -e BASE_URL=http://other-host:8080 k6/load.js

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const JSON_HEADERS = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

// Correctness gates. Apply to every script, smoke included.
// k6 exits non-zero when one of these is breached, which is what makes the
// scripts usable in CI.
export const CORRECTNESS_THRESHOLDS = {
  checks: ['rate>0.99'], // <1% of assertions may fail
  http_req_failed: ['rate<0.01'], // <1% of requests may fail
};

// Latency budgets. Only meaningful under sustained load, which is why they are
// separate: the very first request to a cold JVM takes over a second (JIT plus
// framework init), and in a short smoke run that single request *is* the p99.
// Measuring latency needs enough warm requests for the percentiles to mean
// something.
export const LATENCY_THRESHOLDS = {
  http_req_duration: ['p(95)<300', 'p(99)<800'],
};

// Where the app lives. Kept in one place so the scripts stay readable.
export const ENDPOINTS = {
  users: `${BASE_URL}/users`,
  user: (id) => `${BASE_URL}/users/${id}`,
};
