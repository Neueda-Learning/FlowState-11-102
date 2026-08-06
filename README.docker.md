# Docker Run Guide (Full Stack – DB + Simulator + Backend + Frontend)

This setup runs four containers — **no local MySQL or simulator required**:

| Container | Image / Build | Port | Purpose |
|---|---|---|---|
| `db` | `mysql:8.0` | `3306` | MySQL database (persistent volume) |
| `simulator` | Built from `FakeErrorSimulators` branch | `8081` | Fake payment gateway |
| `backend` | Built from this repo | `8080` | Spring Boot API |
| `frontend` | Built from this repo | `8088` | Nginx static UI + API proxy |

## Prerequisites

The **FakeErrorSimulators** branch must be checked out as a Git worktree **next to** this project folder:

```bash
# Run once from inside the FlowState-11-102 directory
git worktree add ../FlowState-11-102-simulator origin/FakeErrorSimulators
```

Expected directory layout:
```
C:\Users\Administrator\
  ├── FlowState-11-102\           ← main project (this folder)
  └── FlowState-11-102-simulator\ ← simulator worktree
```

## 1) Configure environment

Copy `.env.example` to `.env`. The defaults work out of the box; change the password if needed.

```bash
copy .env.example .env
```

## 2) Build and run

```bash
docker compose up --build -d
```

Compose starts services in dependency order:
1. `db` starts first; backend waits for its healthcheck to pass before connecting
2. `simulator` starts in parallel with `db`
3. `backend` starts once `db` is healthy and `simulator` is running
4. `frontend` starts last

## 3) Open apps

- **Frontend UI**: `http://localhost:8088`
- **Backend API**: `http://localhost:8080`
- **Simulator**: `http://localhost:8081/gateway/process`

## 4) Logs

```bash
docker compose logs db        --tail=100
docker compose logs simulator --tail=200
docker compose logs backend   --tail=200
docker compose logs frontend  --tail=200
```

## 5) Persistent data

MySQL data is stored in a Docker named volume (`flowstate-db-data`). It survives `docker compose down`. To wipe it:

```bash
docker compose down -v
```

## Common issues

1. **Backend can't reach database on startup** – the `healthcheck` on `db` retries up to 10 times (every 10 s). If startup is still slow on first run (MySQL initialising), wait ~60 s and check `docker compose logs backend`.

2. **Port 3306 already in use** – if you have a local MySQL running, stop it or change the host-side port in `docker-compose.yml` (`"3307:3306"`).

3. **Gateway URL** – `GATEWAY_BASE_URL` defaults to `http://simulator:8081` (container DNS). Override in `.env` only if running the simulator outside Docker.

## Stop

```bash
docker compose down
```
