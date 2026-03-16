# Contributing Guide

## Quick Start

1. Fork and clone:
```bash
git clone https://github.com/your-username/receipt-scanner.git
cd receipt-scanner
```

2. Setup environment:
```bash
# Create .env from the template in docs/ENVIRONMENT_SETUP.md
cp .env.example .env
# Edit .env with your configuration
```

3. Start the project:
```bash
docker compose -f docker/docker-compose.yaml --env-file .env up -d --build
```

## Workflow

### 1. Create Branch

```bash
git checkout -b feature/your-feature-name
```

Branch prefixes:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation
- `refactor/` - Code refactoring

### 2. Make Changes

- Follow existing code style
- Keep methods focused and small
- Add comments for complex logic

### 3. Test

```bash
./mvnw test
./mvnw clean compile
```

### 4. Commit

```bash
git add .
git commit -m "feat: add your feature description"
```

Commit prefixes:
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation
- `refactor:` - Refactoring
- `test:` - Tests

### 5. Push

```bash
git push origin feature/your-feature-name
```

### 6. Create Pull Request

Open PR on GitHub with:
- Description of changes
- How to test
- Related issue number

