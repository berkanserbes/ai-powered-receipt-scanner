# Architecture

This document describes the overall system architecture, internal layer structure, and request data flow of the Receipt Scanner application.

---

## Overview

Receipt Scanner is a stateless REST API built with Spring Boot 4. It follows a classic layered architecture (Controller → Service → Repository) with a separate security layer handling JWT authentication and rate limiting.

```
┌─────────────────────────────────────────────────────────────────┐
│                          HTTP Client                            │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTPS / HTTP
┌───────────────────────────────▼─────────────────────────────────┐
│                     Filter Chain (Servlet)                      │
│                                                                 │
│  RequestRateLimitFilter → JwtAuthenticationFilter               │
│  → RequestLoggingFilter → Spring Security Filters               │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                       Controller Layer                          │
│                AuthController  │  ReceiptController             │
└──────────────┬────────────────────────────────┬─────────────────┘
               │                                │
┌──────────────▼──────────────┐  ┌──────────────▼──────────────┐
│        AuthService          │  │       ReceiptService        │
│  RefreshTokenService        │  │   FileStorageService        │
│                             │  │       AIService             │
└──────────────┬──────────────┘  └──────────────┬──────────────┘
               │                                │
┌──────────────▼────────────────────────────────▼──────────────┐
│                       Repository Layer                       │
│   UserRepository  RefreshTokenRepository  ReceiptRepository  │
│   ReceiptItemRepository                                      │
└──────────────────────────────┬───────────────────────────────┘
                               │ JDBC (HikariCP)
┌──────────────────────────────▼────────────────────────────────┐
│                       PostgreSQL 17                           │
└───────────────────────────────────────────────────────────────┘
```

---

## Layer Breakdown

### Filter Layer

Three servlet filters run before the Spring MVC dispatcher:

| Filter | Order | Responsibility |
|---|---|---|
| `RequestRateLimitFilter` | Before `UsernamePasswordAuthenticationFilter` | Sliding-window per-IP rate limiting; returns `429` when exceeded |
| `JwtAuthenticationFilter` | Before `UsernamePasswordAuthenticationFilter` | Parses Bearer token, validates JWT, sets `SecurityContext` |
| `RequestLoggingFilter` | After Spring filters | Logs method, URI, user, and response duration |

### Controller Layer
- Parameter validation (`@Valid`)
- Mapping requests to service calls
- Wrapping results in `ApiResponse<T>` / `PagedResponse<T>`
- Setting appropriate HTTP status codes

**Controllers:**

| Class | Base Path | Purpose |
|---|---|---|
| `AuthController` | `/api/v1/auth` | Register, login, refresh token |
| `ReceiptController` | `/api/v1/receipts` | Upload, analyze, retrieve, download receipts |

### Service Layer

Contains all business logic. Services are unaware of HTTP.

| Class | Responsibility |
|---|---|
| `AuthService` | User registration, login, token refresh orchestration |
| `RefreshTokenService` | Create, verify, revoke refresh tokens |
| `ReceiptService` | Orchestrate file upload → AI analysis → persist receipt |
| `FileStorageService` | Save and read files from the local `uploads/` directory |
| `AIService` | Route analyze request to the configured AI provider |
| `GeminiService` | Call Google Gemini API to extract receipt data |
| `OpenAIService` | Call OpenAI API to extract receipt data |

### Security Layer

| Class | Responsibility |
|---|---|
| `JwtService` | Generate, validate, and parse JWT access tokens |
| `CustomUserDetailsService` | Load `User` entity by username for Spring Security |
| `SecurityConfig` | Configure filter chain, CORS, session policy, access rules |

### Repository Layer

Spring Data JPA repositories with PostgreSQL:

| Repository | Entity |
|---|---|
| `UserRepository` | `User` | 
| `RefreshTokenRepository` | `RefreshToken` |
| `ReceiptRepository` | `Receipt` |
| `ReceiptItemRepository` | `ReceiptItem` |

---

## Request Flow — Receipt Analysis

```
Client
  │
  ├─ POST /api/v1/receipts/analyze (multipart/form-data)
  │
  ▼
RequestRateLimitFilter         — check IP rate limit
  ▼
JwtAuthenticationFilter        — validate Bearer token, set Principal
  ▼
ReceiptController.analyzeReceipt()
  ▼
ReceiptService.analyzeReceipt()
  ├─ FileStorageService.store()         — save file to Cloudinary (if configured) or local disk
  ├─ AIService.analyzeReceipt()
  │     └─ GeminiService / OpenAIService — call external AI API
  ├─ Build Receipt + ReceiptItem entities
  └─ ReceiptRepository.save()
  ▼
ReceiptMapper.toResponse()
  ▼
ApiResponse<ReceiptResponse> → 201 Created
```

---

## Request Flow — Authentication

```
Client
  │
  ├─ POST /api/v1/auth/login { username, password }
  │
  ▼
AuthController.login()
  ▼
AuthService.login()
  ├─ AuthenticationManager.authenticate()   — validates credentials
  ├─ JwtService.generateToken()             — create access token
  ├─ RefreshTokenService.createToken()      — create & persist refresh token
  └─ return LoginResponse { accessToken, refreshToken, tokenType, accessTokenExpiresInMinutes, refreshTokenExpiresInMinutes }
  ▼
200 OK
```

---

## AI Provider Strategy

The AI provider is selected at startup via the `AI_PROVIDER` environment variable:

```
AI_PROVIDER=gemini   → GeminiService (default)
AI_PROVIDER=openai   → OpenAIService
```

`AIService` acts as a facade, delegating to the correct implementation. Switching providers requires only an environment variable change.

---

## Security Model

| Concern | Mechanism |
|---|---|
| Authentication | JWT Bearer token (access token, 15 min default) |
| Session | Stateless — no server-side session |
| Refresh | Rotating refresh tokens (7 days default), stored in DB |
| Authorization | Role-based (`ADMIN`, `USER`) via Spring Security |
| CSRF | Disabled (stateless API) |
| CORS | Configurable via `CorsConfig` |
| Rate Limiting | Per-IP sliding window via `RequestRateLimitFilter` |

---

## Docker Infrastructure

```
Docker Network: receipt-scanner-net (bridge)
│
├── receipt-scanner-db         postgres:17         port 5432
├── receipt-scanner-app        custom image        port 8080
├── receipt-scanner-sonarqube  sonarqube:community port 9000
└── receipt-scanner-portainer  portainer-ce        port 9443
```

See [Docker Guide](DOCKER_GUIDE.md) for details.
