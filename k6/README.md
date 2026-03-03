# k6 Docker Examples

## Prerequisite
- API server is running on host `8080` (`/health` endpoint)
- For macOS Docker Desktop, `host.docker.internal` resolves to host machine

## 1) Smoke Test
```bash
docker run --rm -i \
  -v "$PWD/k6:/work" -w /work \
  -e BASE_URL=http://host.docker.internal:8080 \
  grafana/k6 run smoke.js
```

Or with compose:
```bash
docker compose -f docker-compose-k6.yml run --rm k6 run smoke.js
```

## 2) Load Test
```bash
docker run --rm -i \
  -v "$PWD/k6:/work" -w /work \
  -e BASE_URL=http://host.docker.internal:8080 \
  grafana/k6 run load.js
```

Or with compose:
```bash
docker compose -f docker-compose-k6.yml run --rm k6 run load.js
```

## 3) Stress Test
```bash
docker run --rm -i \
  -v "$PWD/k6:/work" -w /work \
  -e BASE_URL=http://host.docker.internal:8080 \
  grafana/k6 run stress.js
```

Or with compose:
```bash
docker compose -f docker-compose-k6.yml run --rm k6 run stress.js
```

## 4) API User Journey Scenario (auth + business API)
`api-scenario.js` is a realistic user flow script:
- sign in (`/api/auth/sign-in`)
- read APIs (`/api/users/{id}`, `/api/playlists`, `/api/contents`)
- optional write path (`/api/playlists` create/detail/delete)

Run read-only flow:
```bash
docker run --rm -i \
  -v "$PWD/k6:/work" -w /work \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e USERNAME=YOUR_USERNAME \
  -e PASSWORD=YOUR_PASSWORD \
  grafana/k6 run api-scenario.js
```

Run with write flow enabled (CSRF token included automatically):
```bash
docker run --rm -i \
  -v "$PWD/k6:/work" -w /work \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e USERNAME=YOUR_USERNAME \
  -e PASSWORD=YOUR_PASSWORD \
  -e ENABLE_WRITE_TEST=true \
  grafana/k6 run api-scenario.js
```

Useful env vars:
- `THINK_TIME_MIN` (default: `0.5`)
- `THINK_TIME_MAX` (default: `1.5`)

## Optional: Prometheus Remote Write Output
If you want k6 metrics in Prometheus, run with:
```bash
docker run --rm -i \
  -v "$PWD/k6:/work" -w /work \
  -e BASE_URL=http://host.docker.internal:8080 \
  grafana/k6 run --out experimental-prometheus-rw=http://host.docker.internal:9090/api/v1/write load.js
```

Prometheus must enable remote-write receiver. If needed, add this flag to `prometheus` command in `docker-compose-monitoring.yml`:
```yaml
- '--web.enable-remote-write-receiver'
```

## What to Check During Test
- k6 SLI: `http_req_failed`, `http_req_duration(p95/p99)`, `checks`, `workflow_failure_rate`
- API server: CPU, memory, GC pause, thread pool / event loop saturation
- DB/Redis: connection pool usage, query latency, lock wait, timeout/retry
- Error behavior: 4xx/5xx spikes, auth failures, CSRF failures, timeout increase
- Stage transition impact: latency/error jump points when VU increases
