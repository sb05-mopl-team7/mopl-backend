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
