# AI Services Microservice

> Demo/backup microservice for VP presentation - AI functionality extracted from Learn B2

## Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Gradle 8.x (or use included wrapper)

### Run with Docker Compose
```bash
# Start PostgreSQL + mock ai-integrations + app
docker-compose up -d

# View logs
docker-compose logs -f ai-services

# Access Swagger UI
open http://localhost:8080/swagger-ui.html
```

### Run Locally (Dev Mode)
```bash
# Start PostgreSQL only
docker-compose up -d postgres

# Run app with dev profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/v1/ai-playground/conversations` | Create conversation |
| GET | `/v1/ai-playground/conversations` | List conversations (paged) |
| PATCH | `/v1/ai-playground/conversations/{id}` | Update conversation |
| DELETE | `/v1/ai-playground/conversations/{id}` | Delete conversation |
| GET | `/v1/ai-playground/models/usage` | Get model usage/quotas |
| POST | `/v1/courses/{id}/ai/outline` | Generate course outline |
| POST | `/v1/courses/{id}/ai/contents/{cid}/suggest` | Generate content |
| POST | `/v1/courses/{id}/ai/contents/{cid}/images` | Generate images |
| POST | `/v1/courses/{id}/learning-activities/ai/flashcards` | Generate flashcards |
| GET | `/v1/ai/tasks/{taskId}` | Poll async task status |
| GET | `/v1/ai/settings` | System AI settings |
| GET | `/v1/courses/{id}/ai/settings` | Course AI settings |

## Model Quotas

| Model | Daily Limit |
|-------|-------------|
| AMAZON_NOVA_MICRO | 20 |
| AMAZON_NOVA_LITE | 15 |
| OPEN_AI_GPT_OSS_20B | 10 |

## Architecture

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  API GW     │────▶│  AI Services    │────▶│ ai-integrations  │
│  (JWT)      │     │  (this service) │     │  (AI backend)    │
└─────────────┘     └────────┬────────┘     └──────────────────┘
                             │
                    ┌────────▼────────┐
                    │   PostgreSQL    │
                    │  (conversations │
                    │   + usage)      │
                    └─────────────────┘
```

## For Demo

1. Start services: `docker-compose up -d`
2. Open Swagger UI: http://localhost:8080/swagger-ui.html
3. Use dev profile (no JWT required): requests go through without auth
4. Show async task flow: POST to generate -> GET task status -> COMPLETED with result
