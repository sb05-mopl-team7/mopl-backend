import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const USERNAME = __ENV.USERNAME;
const PASSWORD = __ENV.PASSWORD;
const ENABLE_WRITE_TEST = __ENV.ENABLE_WRITE_TEST === 'true';

const thinkTimeMin = Number(__ENV.THINK_TIME_MIN || 0.5);
const thinkTimeMax = Number(__ENV.THINK_TIME_MAX || 1.5);

const authFailureRate = new Rate('auth_failure_rate');
const workflowFailureRate = new Rate('workflow_failure_rate');

let session;

export const options = {
  scenarios: {
    api_user_journey: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 10 },
        { duration: '2m', target: 20 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<600', 'p(99)<1200'],
    checks: ['rate>0.98'],
    auth_failure_rate: ['rate<0.01'],
    workflow_failure_rate: ['rate<0.02'],
  },
};

function randomSleep() {
  const wait = Math.random() * (thinkTimeMax - thinkTimeMin) + thinkTimeMin;
  sleep(wait);
}

function signIn() {
  const payload = {
    username: USERNAME,
    password: PASSWORD,
  };

  const res = http.post(`${BASE_URL}/api/auth/sign-in`, payload, {
    tags: { name: 'auth_sign_in' },
  });

  const ok = check(res, {
    'sign-in status is 200': (r) => r.status === 200,
    'sign-in has access token': (r) => !!r.json('accessToken'),
    'sign-in has user id': (r) => !!r.json('userDto.id'),
  });

  authFailureRate.add(!ok);

  if (!ok) {
    return null;
  }

  return {
    accessToken: res.json('accessToken'),
    userId: String(res.json('userDto.id')),
  };
}

function fetchCsrfToken(authHeaders) {
  const res = http.get(`${BASE_URL}/api/auth/csrf-token`, {
    headers: authHeaders,
    tags: { name: 'auth_csrf_token' },
  });

  const ok = check(res, {
    'csrf-token status is 200': (r) => r.status === 200,
    'csrf-token exists': (r) => !!r.json('token'),
  });

  authFailureRate.add(!ok);
  return ok ? String(res.json('token')) : null;
}

function ensureSession() {
  if (session) {
    return session;
  }

  if (!USERNAME || !PASSWORD) {
    throw new Error('USERNAME and PASSWORD env vars are required for api-scenario.js');
  }

  const login = signIn();
  if (!login) {
    return null;
  }

  session = {
    ...login,
    csrfToken: null,
  };

  if (ENABLE_WRITE_TEST) {
    const csrfToken = fetchCsrfToken({
      Authorization: `Bearer ${session.accessToken}`,
    });

    if (!csrfToken) {
      return null;
    }

    session.csrfToken = csrfToken;
  }

  return session;
}

function authHeaders(accessToken) {
  return {
    Authorization: `Bearer ${accessToken}`,
  };
}

export default function () {
  const current = ensureSession();
  if (!current) {
    workflowFailureRate.add(true);
    sleep(1);
    return;
  }

  let flowOk = true;

  group('read journey', () => {
    const headers = authHeaders(current.accessToken);

    const meRes = http.get(`${BASE_URL}/api/users/${current.userId}`, {
      headers,
      tags: { name: 'users_me' },
    });

    flowOk =
      check(meRes, {
        'users/{id} status is 200': (r) => r.status === 200,
      }) && flowOk;

    const playlistsRes = http.get(`${BASE_URL}/api/playlists?limit=20`, {
      headers,
      tags: { name: 'playlists_list' },
    });

    flowOk =
      check(playlistsRes, {
        'playlists list status is 200': (r) => r.status === 200,
      }) && flowOk;

    const contentsRes = http.get(`${BASE_URL}/api/contents?limit=20`, {
      headers,
      tags: { name: 'contents_list' },
    });

    flowOk =
      check(contentsRes, {
        'contents list status is 200': (r) => r.status === 200,
      }) && flowOk;
  });

  if (ENABLE_WRITE_TEST) {
    group('write journey (playlist create/delete)', () => {
      const headers = {
        ...authHeaders(current.accessToken),
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': current.csrfToken,
      };

      const createRes = http.post(
        `${BASE_URL}/api/playlists`,
        JSON.stringify({
          title: `k6-playlist-${__VU}-${Date.now()}`,
          description: 'k6 generated playlist',
        }),
        {
          headers,
          tags: { name: 'playlists_create' },
        }
      );

      const createOk = check(createRes, {
        'playlists create status is 200': (r) => r.status === 200,
        'playlists create returns id': (r) => !!r.json('id'),
      });
      flowOk = createOk && flowOk;

      if (!createOk) {
        return;
      }

      const playlistId = String(createRes.json('id'));

      const detailRes = http.get(`${BASE_URL}/api/playlists/${playlistId}`, {
        headers: authHeaders(current.accessToken),
        tags: { name: 'playlists_detail' },
      });

      flowOk =
        check(detailRes, {
          'playlists detail status is 200': (r) => r.status === 200,
        }) && flowOk;

      const deleteRes = http.del(`${BASE_URL}/api/playlists/${playlistId}`, null, {
        headers: {
          ...authHeaders(current.accessToken),
          'X-XSRF-TOKEN': current.csrfToken,
        },
        tags: { name: 'playlists_delete' },
      });

      flowOk =
        check(deleteRes, {
          'playlists delete status is 204': (r) => r.status === 204,
        }) && flowOk;
    });
  }

  workflowFailureRate.add(!flowOk);
  randomSleep();
}
