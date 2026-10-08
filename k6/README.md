# k6 performance tests

Load tests for the user API, written for [Grafana k6](https://k6.io).

```
k6/
├── lib/
│   ├── config.js     base URL, thresholds, endpoints
│   ├── client.js     one function per endpoint, each tagged with an `op`
│   └── data.js       unique test data generator
├── smoke.js          does CRUD work at all?        ~10s
├── load.js           normal traffic, held steady   ~30s
├── stress.js         ramp until it breaks          ~5m
└── spike.js          sudden burst, then recovery   ~1m30s
```

Start the app first — every script needs it running:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw spring-boot:run
```

## Running

On this machine k6 is not installed, so run it through Docker.

**Do not use `--network host`.** Docker Desktop runs containers inside its own
VM, so `localhost` inside the container is not this WSL distro and the app is
unreachable. Pass the WSL IP instead:

```bash
WSL_IP=$(hostname -I | awk '{print $1}')

docker run --rm -v "$PWD/k6:/scripts" -w /scripts \
  -e BASE_URL="http://$WSL_IP:8080" \
  grafana/k6 run smoke.js
```

That IP changes when WSL restarts, which is why it is computed each time rather
than hard-coded. Nothing about this is k6-specific — it is how Docker Desktop
and WSL talk, or fail to.

To install k6 natively instead (then plain `k6 run smoke.js` works, and no
`BASE_URL` is needed because `localhost` is correct), follow
<https://grafana.com/docs/k6/latest/set-up/install-k6/>.

Point at another host with `-e BASE_URL=http://deployed-host:8080`.

## The scripts

| Script | Question it answers | Shape |
|---|---|---|
| `smoke.js` | Does the whole CRUD cycle work? | 1 VU, 10s |
| `load.js` | Is it fast enough at expected traffic? | 10 readers + 5 writers, 30s |
| `stress.js` | At what point does it degrade? | ramp 0→20→50→100 VUs |
| `spike.js` | Does it survive a burst, and recover? | jump to 200 VUs, drop, hold |

Run `smoke.js` first. If it fails, the others only repeat it more slowly.

### Why load.js has two scenarios

Real traffic is mostly reads. Testing only writes gives a number that does not
match production. `readers` and `writers` run at the same time, and each
operation is tagged so the summary can report them separately:

```
{ op:create_user }...: avg=10.32ms p(95)=26.73ms
{ op:list_users }....: avg=7.91ms  p(95)=21.53ms
```

Without the `op` tag every request lands in one blended average, and a slow
endpoint hides behind fast ones.

## Thresholds

k6 exits with a non-zero code when a threshold is breached, which is what makes
these usable as a CI gate. They are split in `lib/config.js`:

* `CORRECTNESS_THRESHOLDS` — check success rate, failed request rate. Used by
  every script, smoke included.
* `LATENCY_THRESHOLDS` — p95/p99 response times. Used by `load.js` only.

The split is deliberate. The first request to a cold JVM takes about 1.5s
(JIT plus framework initialisation). In a short smoke run of ~60 requests that
single request *is* the p99, so a latency threshold there fails for a reason
that has nothing to do with the API being slow. Latency percentiles only mean
something once there are enough warm requests behind them.

`stress.js` and `spike.js` set their own looser thresholds on purpose — they are
tripwires for "something is badly wrong", not performance targets.

## Test data

`lib/data.js` builds emails from k6's `__VU` and `__ITER` built-ins, so parallel
virtual users never collide without asking the server first.

Every write flow deletes the user it created. The repository is an in-memory
`ConcurrentHashMap` and `GET /users` returns the entire store, so anything left
behind makes every later list call return a bigger body. Leaving data behind
would turn a load test into a test of "how fast does an ever-growing array
serialise".

This also means results depend on how much data is already in the store when you
start. Restart the app for a clean baseline before comparing runs.

## Baseline measured on this machine

App and k6 sharing one WSL instance, so treat these as a sanity reference rather
than a capacity number:

| Script | Result |
|---|---|
| `smoke.js` | 110/110 checks, p95 12.5ms |
| `load.js` | 880 requests, 0% failed, p95 14.8ms, p99 45.4ms |
| `spike.js` | 200 VUs, 24,294 requests, 0% failed, p95 24.2ms |

Every threshold passed. The in-memory store is not the bottleneck here — at
these volumes the HTTP and JSON layers dominate.

## Adding a script

Reuse `lib/client.js` so the `op` tags stay consistent, and add tags for
anything you want broken out in the summary. For example, to test the 404 path
at volume you would need `http.setResponseCallback(http.expectedStatuses(...,
404))` at the top, otherwise those 404s count as failed requests and trip
`http_req_failed`.
