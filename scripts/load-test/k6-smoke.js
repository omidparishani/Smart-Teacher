/**
 * Smart Teacher — k6 smoke test (Phase 12)
 *
 * Prerequisites:
 *   - Backend running (default http://localhost:8080)
 *   - k6 installed: https://grafana.com/docs/k6/latest/set-up/install-k6/
 *
 * Run:
 *   k6 run scripts/load-test/k6-smoke.js
 *
 * Environment:
 *   BASE_URL          — API root (default http://localhost:8080)
 *   ADMIN_USERNAME    — admin login username (optional; skips login rate test if unset)
 *   ADMIN_PASSWORD    — admin login password
 *
 * What it checks:
 *   1. GET /actuator/health — liveness (no auth)
 *   2. POST /api/v1/auth/login — rate-limit path smoke (expects 200 or 401, not 5xx)
 *
 * Thresholds are intentionally loose for local/dev smoke only.
 */
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || '';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || '';

export const options = {
  vus: 2,
  duration: '15s',
  thresholds: {
    http_req_failed: ['rate<0.5'],
    http_req_duration: ['p(95)<3000'],
  },
};

export default function () {
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, {
    'health status 200': (r) => r.status === 200,
    'health body UP': (r) => r.body && r.body.includes('UP'),
  });

  if (ADMIN_USERNAME && ADMIN_PASSWORD) {
    const loginRes = http.post(
      `${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    check(loginRes, {
      'login not 5xx': (r) => r.status < 500,
      'login 200 or 401': (r) => r.status === 200 || r.status === 401 || r.status === 429,
    });
  }

  sleep(1);
}
