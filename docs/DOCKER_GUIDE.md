# Docker Guide

This guide covers building, running, and managing the Receipt Scanner application using Docker and Docker Compose.

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows / macOS) or Docker Engine (Linux)
- Docker Compose v2 (bundled with Docker Desktop)
- A `.env` file in the project root — see [Environment Setup](ENVIRONMENT_SETUP.md)

---

## Services

All services share the `receipt-scanner-net` bridge network.

| Service | Container Name | Image | Port |
|---|---|---|---|
| Application | `receipt-scanner-app` | Custom (multi-stage build) | `8080` |
| Database | `receipt-scanner-db` | `postgres:17` | `5432` |
| SonarQube | `receipt-scanner-sonarqube` | `sonarqube:community` | `9000` |
| Portainer | `receipt-scanner-portainer` | `portainer/portainer-ce` | `9443`, `9001` |

---

## Quick Start Scripts

The easiest way to start all services:

**Windows:**
```bat
run-docker-app.bat
```

**Linux / macOS:**
```bash
chmod +x run-docker-app.sh
./run-docker-app.sh
```

Both scripts:
1. Verify that `.env` exists in the project root
2. Run `docker compose ... up -d --build`
3. Report success or failure

---

## Manual Commands

All commands below are run from the **project root** (`c:\...\receipt-scanner`).

### Start all services

```bash
docker compose -f docker/docker-compose.yaml --env-file .env up -d --build
```

### Start only the database

```bash
docker compose -f docker/docker-compose.yaml --env-file .env up -d postgres
```

### Rebuild and restart only the app

```bash
docker compose -f docker/docker-compose.yaml --env-file .env up -d --build app
```

### Stop all services

```bash
docker compose -f docker/docker-compose.yaml --env-file .env down
```

### Stop and remove volumes (wipes database data)

```bash
docker compose -f docker/docker-compose.yaml --env-file .env down -v
```

### View logs

```bash
# All services
docker compose -f docker/docker-compose.yaml --env-file .env logs -f

# Application only
docker compose -f docker/docker-compose.yaml --env-file .env logs -f app

# Last 100 lines
docker compose -f docker/docker-compose.yaml --env-file .env logs app --tail 100
```

### Check container status

```bash
docker compose -f docker/docker-compose.yaml --env-file .env ps
```

---

## Dockerfile

The application uses a **multi-stage build** to keep the final image small:

```
Stage 1 — builder (eclipse-temurin:25-jdk)
  1. Copy Maven wrapper + pom.xml  →  download dependencies (cached layer)
  2. Copy source code
  3. mvnw package -DskipTests     →  produces target/*.jar

Stage 2 — runtime (eclipse-temurin:25-jre)
  1. Copy only app.jar from builder
  2. EXPOSE 8080
  3. ENTRYPOINT java -jar app.jar
```

This means:
- The final image contains only the JRE and the JAR — no JDK, no Maven, no sources.
- Dependency downloads are in a separate layer and are cached unless `pom.xml` changes.

---

## Volumes

| Volume Name | Mount Point | Purpose |
|---|---|---|
| `postgres_data` | `/var/lib/postgresql/data` | PostgreSQL data persistence |
| `uploads_data` | `/app/uploads` | Receipt image file storage |
| `sonarqube_data` | `/opt/sonarqube/data` | SonarQube analysis data |
| `sonarqube_extensions` | `/opt/sonarqube/extensions` | SonarQube plugins |
| `sonarqube_logs` | `/opt/sonarqube/logs` | SonarQube logs |
| `portainer_data` | `/data` | Portainer configuration |

---

## Health Checks

The `postgres` service has a built-in health check:

```yaml
healthcheck:
  test: pg_isready -U <user> -d <db>
  interval: 10s
  timeout: 5s
  retries: 5
```

The `app` service uses `depends_on: postgres: condition: service_healthy`, so it will not start until PostgreSQL is ready.

---

## SonarQube Setup

After first boot, SonarQube requires initialization (~1 minute):

1. Open http://localhost:9000
2. Login with default credentials: `admin` / `admin`
3. Change the password when prompted
4. Create a new project and generate an analysis token

To analyze the project:
```bash
./mvnw sonar:sonar \
  -Dsonar.projectKey=receipt-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<your-token>
```

---

## Portainer Setup

1. Open https://localhost:9443
2. Create an admin account on first visit
3. Connect to the **local** Docker environment

---

## .dockerignore

The `.dockerignore` at the project root prevents the following from being included in the Docker build context:

| Excluded | Reason |
|---|---|
| `target/` | Build output — rebuilt inside container |
| `.git/` | Version control history not needed |
| `logs/`, `uploads/` | Runtime data — managed via volumes |
| `.env`, `.env.*` | Secrets must never be baked into the image |
| `mvnw.cmd` | Windows-specific — not needed in Linux container |
| `docker/` | Prevents recursive inclusion of the compose file |

---

## Troubleshooting

### App container keeps restarting

Check the logs:
```bash
docker compose -f docker/docker-compose.yaml --env-file .env logs app --tail 100
```

Common causes:
- Database not ready (check if `postgres` is `healthy`)
- Missing environment variable (check `.env` values)
- Port `8080` already in use on the host

### Cannot connect to database

Verify that the database container is healthy:
```bash
docker inspect receipt-scanner-db --format "{{.State.Health.Status}}"
```

### Permission denied on uploads volume (Linux)

```bash
sudo chown -R 1000:1000 ./uploads
```
