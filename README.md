<div align="center">

# 🧾 Receipt Scanner

### An AI-powered receipt scanning and management REST API

[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

[Features](#-features) • [Quick Start](#-quick-start) • [API Endpoints](#-api-endpoints) • [Tech Stack](#-tech-stack) • [Documentation](#-documentation) • [Contributing](#-contributing)

</div>

---

## 📌 About

Receipt Scanner is a backend REST API that automates the tedious process of manually entering receipt data. Users simply upload a photo or PDF of a receipt, and the application uses an AI model (Google Gemini or OpenAI GPT-4o) to extract structured data — merchant name, transaction date, currency, total amount, and individual line items — which is then persisted to a PostgreSQL database.

The project is designed with a clean layered architecture, stateless JWT-based authentication, role-based access control, and optional Cloudinary support for cloud file storage. It is fully containerized and ready to run with a single Docker Compose command.

---

## 🌟 Features

<table>
<tr>
<td width="50%">

### 🤖 AI Receipt Analysis
- Upload JPEG, PNG or PDF receipt images
- Automatic extraction of merchant name, date, total amount and line items
- Supports Google Gemini and OpenAI GPT-4o
- Switch AI provider with a single env variable

### 🔐 Authentication & Security
- JWT-based stateless authentication
- Rotating refresh tokens (stored in DB)
- Role-based access control (`ADMIN` / `USER`)
- BCrypt password hashing

</td>
<td width="50%">

### 🗄️ File Storage
- Local disk storage out of the box
- Optional Cloudinary integration (set credentials to activate)
- Secure path traversal protection

### 🚀 Performance & Observability
- Per-IP sliding-window rate limiter
- Request/response logging filter
- Interactive Swagger UI at `/swagger-ui.html`
- Docker Compose with SonarQube and Portainer

</td>
</tr>
</table>

---

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- A `.env` file in the project root — see [Environment Setup](docs/ENVIRONMENT_SETUP.md)

### 🐳 Run with Docker (recommended)

**Windows:**
```bat
run-docker-app.bat
```

**Linux / macOS:**
```bash
chmod +x run-docker-app.sh
./run-docker-app.sh
```

Or manually from the project root:
```bash
# 1. Clone the repository
git clone https://github.com/your-username/receipt-scanner.git
cd receipt-scanner

# 2. Create and configure .env
cp .env.example .env
# Edit .env with your values

# 3. Start all services
docker compose -f docker/docker-compose.yaml --env-file .env up -d --build

# 4. Access the application
# API:       http://localhost:8080
# Swagger:   http://localhost:8080/swagger-ui.html
```

### 💻 Local Development

```bash
# 1. Start only the database
docker compose -f docker/docker-compose.yaml --env-file .env up -d postgres

# 2. Run the application
./mvnw spring-boot:run

# 3. Open Swagger UI
# http://localhost:8080/swagger-ui.html
```

---

## 🎯 API Endpoints

### 🔓 Authentication — `/api/v1/auth`

| Method | Path | Description |
|---|---|---|
| `POST` | `/auth/register` | Register a new user |
| `POST` | `/auth/login` | Login and receive JWT tokens |
| `POST` | `/auth/refresh` | Rotate refresh token |

### 🔐 Receipts — `/api/v1/receipts`

| Method | Path | Description |
|---|---|---|
| `POST` | `/receipts/analyze` | Upload and analyze a receipt image |
| `GET` | `/receipts` | List receipts (paginated) |
| `GET` | `/receipts/{id}` | Get receipt details |
| `GET` | `/receipts/{id}/view` | View receipt file inline |
| `GET` | `/receipts/{id}/download` | Download receipt file |

> `ADMIN` can access all users' receipts. `USER` can only access their own.

📖 **Full API documentation:** `http://localhost:8080/swagger-ui.html`

---

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ReceiptServiceTest

# Run with coverage report
./mvnw verify
```

---

## 🛠️ Tech Stack

### Backend
<p>
<img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
<img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security"/>
<img src="https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Data JPA"/>
</p>

### Database
<p>
<img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
<img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white" alt="Hibernate"/>
</p>

### Security & API
<p>
<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT"/>
<img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger"/>
<img src="https://img.shields.io/badge/OpenAPI-6BA539?style=for-the-badge&logo=openapiinitiative&logoColor=white" alt="OpenAPI"/>
</p>

### DevOps & Tools
<p>
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
<img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven"/>
<img src="https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white" alt="SonarQube"/>
<img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white" alt="Git"/>
</p>

---

## 📊 Project Structure

```
receipt-scanner/
├── 📁 src/main/java/com/berkan/receiptscanner/
│   ├── 🔧 config/          # Security, CORS, OpenAPI, data initializer
│   ├── 🎮 controller/      # REST controllers
│   ├── 📦 dto/             # Request / response DTOs
│   ├── 🗂️ entity/          # JPA entities
│   ├── ⚠️ exception/       # Custom exceptions and global handler
│   ├── 🔍 filter/          # JWT, rate limit, logging filters
│   ├── 🗺️ mapper/          # Entity ↔ DTO mappers
│   ├── 💾 repository/      # Spring Data JPA repositories
│   ├── 🔐 security/        # JWT service, UserDetailsService
│   └── 💼 service/         # Business logic
│
├── 🐳 docker/
│   ├── Dockerfile
│   └── docker-compose.yaml
│
├── 📚 docs/
│   ├── ARCHITECTURE.md
│   ├── DATABASE_SCHEMA.md
│   ├── DOCKER_GUIDE.md
│   └── ENVIRONMENT_SETUP.md
│
├── 🪟 run-docker-app.bat
├── 🐧 run-docker-app.sh
└── 📝 pom.xml
```

---

## 📚 Documentation

| Document | Description |
|---|---|
| 📖 [Architecture](docs/ARCHITECTURE.md) | System design, layers, and request flow |
| 🗄️ [Database Schema](docs/DATABASE_SCHEMA.md) | Tables, columns, and relationships |
| 🐳 [Docker Guide](docs/DOCKER_GUIDE.md) | How to build, run, and manage containers |
| ⚙️ [Environment Setup](docs/ENVIRONMENT_SETUP.md) | All environment variables explained |
| 🤝 [Contributing](CONTRIBUTING.md) | How to contribute to the project |

---

## 🤝 Contributing

Contributions are welcome! Please see the [Contributing Guide](CONTRIBUTING.md) for details.

1. 🍴 Fork the repository
2. 🌿 Create your branch (`git checkout -b feature/amazing-feature`)
3. ✅ Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. 📤 Push to the branch (`git push origin feature/amazing-feature`)
5. 🎉 Open a Pull Request

---

## 🔒 Security

| Mechanism | Detail |
|---|---|
| Authentication | JWT Bearer token (15 min access token) |
| Refresh | Rotating refresh tokens (7 days, stored in DB) |
| Authorization | Role-based — `ADMIN` / `USER` |
| Rate Limiting | Per-IP sliding window (120 req/min default) |
| CORS | Configurable via environment variables |
| File Upload | Content-type validation + path traversal protection |

---

## 🌐 Service URLs

| Service | URL |
|---|---|
| Application | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| SonarQube | http://localhost:9000 |
| Portainer | https://localhost:9443 |

---

<div align="center">

### ⭐ Star this repository if you find it helpful!

</div>

