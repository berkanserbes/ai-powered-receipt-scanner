# Environment Setup

This document describes all environment variables used by the Receipt Scanner application, how to configure them, and how to set up your local development environment.

---

## .env File

The application uses a `.env` file at the project root for local and Docker-based configurations.

---

## Full .env Template

```dotenv
# ─── Database ─────────────────────────────────────────────────
DB_HOST=localhost
DB_PORT=5432
DB_NAME=receipt_scanner_db
DB_USERNAME=postgres
DB_PASSWORD=your-db-password

# ─── Server ───────────────────────────────────────────────────
SERVER_PORT=8080

# ─── JWT ──────────────────────────────────────────────────────
JWT_SECRET=YourSuperSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong123456
JWT_ACCESS_TOKEN_EXPIRATION_MINUTES=15
JWT_REFRESH_TOKEN_EXPIRATION_MINUTES=10080

# ─── AI Provider ──────────────────────────────────────────────
# Options: gemini | openai
AI_PROVIDER=gemini

GEMINI_API_KEY=your-gemini-api-key
GEMINI_MODEL=gemini-2.5-flash-lite
GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta

OPENAI_API_KEY=your-openai-api-key
OPENAI_MODEL=gpt-4o
OPENAI_API_URL=https://api.openai.com/v1/chat/completions

# ─── Admin User ───────────────────────────────────────────────
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change-me-on-first-login

# ─── File Storage ─────────────────────────────────────────────
FILE_UPLOAD_DIR=uploads

# ─── Cloudinary (optional — leave blank to use local storage) ─
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CLOUDINARY_FOLDER=receipts

# ─── Security / CORS ──────────────────────────────────────────
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_EXPOSED_HEADERS=X-RateLimit-Limit,X-RateLimit-Window-Seconds
CORS_ALLOW_CREDENTIALS=true
CORS_MAX_AGE_SECONDS=3600

# ─── Security / Rate Limiting ─────────────────────────────────
RATE_LIMIT_MAX_REQUESTS=120
RATE_LIMIT_WINDOW_SECONDS=60

# ─── Logging ──────────────────────────────────────────────────
LOG_LEVEL_ROOT=INFO

# ─── API ──────────────────────────────────────────────────────
API_VERSION=v1
```

---

## Variable Reference

### Database

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host. In Docker Compose, the app container automatically uses `postgres` (the service name) — no manual change needed. |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `receipt_scanner_db` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |

---

### Server

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | Port the application listens on |

---

### JWT

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | *(insecure default)* | HMAC-SHA256 signing secret. Must be at least 256 bits (32 characters). |
| `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES` | `15` | Access token lifetime in minutes |
| `JWT_REFRESH_TOKEN_EXPIRATION_MINUTES` | `10080` | Refresh token lifetime in minutes (default: 7 days) |

**Generating a secure JWT secret:**
```bash
# Linux / macOS
openssl rand -base64 64

# PowerShell
[Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

---

### AI Provider

| Variable | Default | Options | Description |
|---|---|---|---|
| `AI_PROVIDER` | `gemini` | `gemini`, `openai` | Selects which AI backend to use for receipt analysis |

#### Gemini

| Variable | Default | Description |
|---|---|---|
| `GEMINI_API_KEY` | *(empty)* | Google AI Studio API key — required when `AI_PROVIDER=gemini` |
| `GEMINI_MODEL` | `gemini-2.5-flash-lite` | Gemini model name |
| `GEMINI_API_URL` | `https://generativelanguage.googleapis.com/v1beta` | Gemini API base URL |

Get a Gemini API key: https://aistudio.google.com/app/apikey

#### OpenAI

| Variable | Default | Description |
|---|---|---|
| `OPENAI_API_KEY` | *(empty)* | OpenAI API key — required when `AI_PROVIDER=openai` |
| `OPENAI_MODEL` | `gpt-4o` | OpenAI model name |
| `OPENAI_API_URL` | `https://api.openai.com/v1/chat/completions` | OpenAI chat completions endpoint |

Get an OpenAI API key: https://platform.openai.com/api-keys

---

### Admin User

A default admin account is created automatically on first startup by `DataInitializer` if no admin user exists yet.

| Variable | Default | Description |
|---|---|---|
| `ADMIN_USERNAME` | `admin` | Username for the initial admin account |
| `ADMIN_PASSWORD` | `admin123` | Password for the initial admin account. Change this immediately after first login. |

---

### File Storage

The application supports two storage backends. If all three Cloudinary credentials are provided, Cloudinary is used; otherwise, files are saved to local disk.

| Variable | Default | Description |
|---|---|---|
| `FILE_UPLOAD_DIR` | `uploads` | Local directory for uploaded receipt images. In Docker, this maps to the `uploads_data` volume at `/app/uploads`. Always required even when Cloudinary is enabled (used as fallback directory). |

Accepted file types: `image/jpeg`, `image/png`, `application/pdf`  
Maximum file size: `10 MB` (set in `application.yaml` via `spring.servlet.multipart.max-file-size`)

#### Cloudinary (optional)

When all three of the following are set, uploaded files are stored in Cloudinary instead of local disk:

| Variable | Default | Description |
|---|---|---|
| `CLOUDINARY_CLOUD_NAME` | *(empty)* | Cloudinary cloud name from your dashboard |
| `CLOUDINARY_API_KEY` | *(empty)* | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | *(empty)* | Cloudinary API secret |
| `CLOUDINARY_FOLDER` | `receipts` | Folder name inside your Cloudinary media library where receipts are stored |

Get Cloudinary credentials: https://cloudinary.com/console

> If any of the three credentials are blank, Cloudinary is disabled and local disk storage is used automatically.

---

### Security — CORS

| Variable | Default | Description |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://127.0.0.1:3000` | Comma-separated list of allowed origins. Set to your frontend URL(s) in production. |

The following CORS settings are fixed in `application.yaml` and can be overridden if needed:

| Property key | Default |
|---|---|
| `security.cors.allowed-methods` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` |
| `security.cors.allowed-headers` | `*` |
| `security.cors.exposed-headers` | `X-RateLimit-Limit,X-RateLimit-Window-Seconds` |
| `security.cors.allow-credentials` | `true` |
| `security.cors.max-age-seconds` | `3600` |

---

### Security — Rate Limiting

Per-IP sliding-window rate limiter applied to all requests (except Swagger and OPTIONS).

| Variable | Default | Description |
|---|---|---|
| `RATE_LIMIT_MAX_REQUESTS` | `120` | Maximum number of requests allowed per window per IP |
| `RATE_LIMIT_WINDOW_SECONDS` | `60` | Length of the sliding window in seconds |

When the limit is exceeded the API returns `429 Too Many Requests` with headers:
- `X-RateLimit-Limit` — configured max requests
- `X-RateLimit-Window-Seconds` — configured window size

---

### Logging

| Variable | Default | Description |
|---|---|---|
| `LOG_LEVEL_ROOT` | `INFO` | Root log level. Options: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` |

---

### API

| Variable | Default | Description |
|---|---|---|
| `API_VERSION` | `v1` | API version prefix used in all endpoint paths: `/api/v1/...` |

---

## Local Development (without Docker)

### Requirements

- Java 25
- Maven (or use `./mvnw`)
- PostgreSQL 17 running locally

### Steps

1. Install and start PostgreSQL.
2. Create the database:
   ```sql
   CREATE DATABASE receipt_scanner_db;
   ```
3. Create `.env` at the project root using the template above.
4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Environment Variable Loading

The application uses [spring-dotenv](https://github.com/paulschwarz/spring-dotenv) to automatically load the `.env` file via:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

You do **not** need to export variables manually, just create the `.env` file and run.
