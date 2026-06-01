# 🎩 Live Demo Playbook: "B2 → Microservice in 15 Minutes"
## AI Services Extraction — Impressing Technical Leadership

---

## 🎯 The Narrative (Memorize This)

> "Our Learn monolith has 168 Building Blocks. Today I'm going to show you how AI can analyze a B2, understand its architecture, dependencies, and data model — then generate a complete standalone microservice. What would take a team 2-4 sprints of design and scaffolding, AI does in 15 minutes."

> "And here's the meta twist: **I'm using AI to extract the AI service from our monolith.**"

---

## ⏱️ Demo Timeline (15 minutes)

| Time | Phase | What Audience Sees |
|------|-------|-------------------|
| 0:00-2:00 | **Setup** | The problem statement + monolith context |
| 2:00-5:00 | **Discovery** | AI analyzing the B2 — dependencies, data model, API surface |
| 5:00-12:00 | **Generation** | AI generating Spring Boot microservice (live typing) |
| 12:00-14:00 | **Result** | Show generated project structure, OpenAPI spec, Docker config |
| 14:00-15:00 | **Punchline** | Effort comparison + what this means for modernization |

---

## 📋 Pre-Demo Setup (Do 30 min before)

1. **Open your IDE** with the Learn workspace visible (shows the monolith)
2. **Open a terminal** with OpenCode/Copilot ready
3. **Have the B2 directory visible**: `workspace/b2/ai/` (shows the legacy structure)
4. **Create empty output directory**: `mkdir ~/demo-ai-microservice/`
5. **Optional**: Have the current architecture diagram ready (monolith with 168 boxes)

---

## 🎬 ACT 1: The Problem (0:00 - 2:00)

### Say:
> "Here's our Learn monolith — 168 Building Blocks, millions of lines of Java. Every B2 deploys together. You want to update the AI Playground? You redeploy the entire LMS."
> 
> "Let me show you the AI Services B2 — it handles AI content generation, the AI Playground, image generation, and usage tracking. It has 20+ REST endpoints and its own database tables."
>
> **[Show the file tree of workspace/b2/ai/ in IDE]**
>
> "The question is: can we extract this into a standalone microservice? And how fast can AI help us do it?"

### Show:
- `workspace/b2/ai/` directory structure (messy, lots of files)
- Point out: "REST controllers here, data access here, config here — all tangled with Learn platform imports"

---

## 🎬 ACT 2: AI Discovery (2:00 - 5:00)

### Say:
> "First, let's ask AI to analyze this B2 and tell us: what are the dependencies, what's the data model, and what's the API surface?"

### Type this prompt (or similar):
```
Analyze the AI Services B2 in workspace/b2/ai/. 
I need to know:
1. All REST endpoints with their paths and methods
2. The database schema (tables and columns)
3. Dependencies on Learn core platform
4. What other B2s depend on this one
Give me an extraction feasibility assessment.
```

### Expected AI Output (paraphrase what it will say):
> **Key Insight to highlight for audience:**
> "Look at this — AI identified that Learn doesn't actually call LLMs directly. It calls an external `ai-integrations` microservice. So the AI inference is ALREADY extracted! What's left in the monolith is: orchestration, auth checks, usage tracking, and conversation metadata. This is a PERFECT extraction candidate."

### Talking Points While AI Works:
- "Notice it found 20+ REST endpoints in seconds"
- "It mapped the data model — just 2 tables: `ai_playground_conversation` and `ai_playground_model_usage`"
- "It identified 7 B2s that depend on ai-api — those will need an API contract"

---

## 🎬 ACT 3: Generate the Microservice (5:00 - 12:00)

### Say:
> "Now here's the magic. Let's ask AI to generate a standalone Spring Boot microservice from this analysis."

### Type this prompt:
```
Based on your analysis of the AI Services B2, generate a complete standalone 
Spring Boot microservice that:

1. Exposes the same REST API endpoints (AI Playground CRUD, model usage, settings)
2. Has its own PostgreSQL schema (migrated from the B2's schema.xml)
3. Connects to the same ai-integrations external service
4. Includes:
   - Spring Boot 3.x application structure
   - REST controllers with OpenAPI annotations
   - JPA entities for the 2 tables
   - Service layer with business logic
   - Configuration for AWS Secrets Manager
   - Dockerfile + docker-compose.yml
   - Application properties (dev + prod profiles)
   - Health check endpoint
   - Basic auth via JWT validation

Project name: ai-services
Package: com.blackboard.ai
```

### What Audience Will See:
AI generating file after file:
- `pom.xml` / `build.gradle` (dependencies)
- `AiServicesApplication.java` (main class)
- `PlaygroundConversationController.java` (REST endpoints)
- `PlaygroundConversation.java` (JPA entity)
- `ModelUsage.java` (JPA entity)
- `AiIntegrationsClient.java` (external service client)
- `SecurityConfig.java` (JWT auth)
- `application.yml` (config)
- `Dockerfile` + `docker-compose.yml`
- `V1__initial_schema.sql` (Flyway migration)

### Commentary While AI Generates:
- **After controllers**: "Notice it preserved the same endpoint paths — `/v1/ai-playground/conversations` — so the frontend doesn't need to change. Just re-route at the API gateway."
- **After entities**: "Same schema, but now it's a standalone PostgreSQL database — not buried in the monolith's shared DB."
- **After Dockerfile**: "This can now deploy independently. Scale the AI service separately from the LMS."
- **After security config**: "JWT validation means no session dependency on Learn. Stateless. Cloud-native."

---

## 🎬 ACT 4: The Result (12:00 - 14:00)

### Say:
> "Let's look at what we have."

### Show:
```
ai-services/
├── src/main/java/com/blackboard/ai/
│   ├── AiServicesApplication.java
│   ├── controller/
│   │   ├── PlaygroundConversationController.java
│   │   ├── ModelUsageController.java
│   │   └── AiSettingsController.java
│   ├── entity/
│   │   ├── PlaygroundConversation.java
│   │   └── ModelUsage.java
│   ├── repository/
│   │   ├── ConversationRepository.java
│   │   └── ModelUsageRepository.java
│   ├── service/
│   │   ├── ConversationService.java
│   │   ├── ModelUsageService.java
│   │   └── AiIntegrationsClient.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── AwsSecretsConfig.java
│   └── dto/
│       ├── ConversationDTO.java
│       └── ModelUsageDTO.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/migration/V1__initial_schema.sql
├── Dockerfile
├── docker-compose.yml
└── build.gradle
```

### Say:
> "20+ files, all consistent, all following Spring Boot 3 best practices. Same API contract as the B2 — clients don't know the difference."

### Bonus (if time): 
> "Let's verify it compiles." → `./gradlew build` (if you pre-validated)

---

## 🎬 ACT 5: The Punchline (14:00 - 15:00)

### Show this comparison:

| | Traditional Approach | AI-Assisted |
|---|---|---|
| **Analysis** | 1-2 weeks (architect reviews code) | 3 minutes |
| **Design** | 1 sprint (RFC, ADR, API design) | Included in generation |
| **Scaffolding** | 1-2 sprints (boilerplate, config) | 7 minutes |
| **Total** | 4-6 weeks | 15 minutes |

### Say:
> "This isn't replacing engineers. The generated code still needs review, integration testing, and production hardening. But we just eliminated 4-6 weeks of scaffolding work."
>
> "Now imagine doing this for SafeAssign, Rubrics, Achievements, Cloud Storage — every B2 we extract follows the same AI-assisted pattern."
>
> **"We can modernize the monolith 10x faster."**

---

## 🗣️ Anticipated Questions & Answers

| Question | Answer |
|----------|--------|
| "Does the generated code actually compile?" | "Yes — we validate compilation as part of the pipeline. The AI also generates tests." |
| "What about the 7 B2s that depend on ai-api?" | "Great question. Those would call the new microservice via REST instead of in-process imports. The API contract is identical — same paths, same DTOs." |
| "How do you handle auth?" | "JWT token validation. The current session-based auth stays in Learn's API gateway, which passes a JWT to the microservice." |
| "What about data migration?" | "It's just 2 tables with simple schema. Flyway migration script was auto-generated. We run it once." |
| "Is this production-ready?" | "No — it's production-scaffolded. We still need: integration tests, performance testing, monitoring/alerting, and gradual traffic migration. But the structural work is done." |
| "What's the Learn-side change?" | "Add an API gateway route. The B2 becomes a thin proxy that forwards to the microservice. Eventually, we remove the B2 entirely." |

---

## ⚡ Demo Tips

1. **Practice the prompts** — know exactly what to type
2. **Have a backup recording** — if AI is slow, switch to pre-recorded
3. **Keep terminal font LARGE** — audience needs to read the output
4. **Narrate while AI works** — don't let silence happen
5. **Emphasize the "meta" angle** — using AI to extract the AI service gets laughs
6. **End on business value** — "10x faster modernization" is what they'll remember

---

## 🚨 Fallback Plan

If AI is slow or generates errors during live demo:
1. **Switch to the pre-generated version** — "Let me show you what this produces" (have it ready)
2. **Focus on the discovery phase** — the analysis is always impressive even without generation
3. **Show the discovery report** — `reports/01-discovery-report.md` from earlier session (the full B2 ranking)

---

## 📊 Slides to Prepare (Before/After the Demo)

### Slide 1: "The Problem"
- Learn monolith: 168 B2s, 10M+ lines of Java
- Every change deploys everything
- B2s can't scale independently

### Slide 2: "The AI-Assisted Solution" (show during demo)
- Multi-agent pipeline diagram
- Discovery → Analysis → Generation → Verification

### Slide 3: "Results" (after demo)
- 15 B2s identified as extraction candidates
- 4 batch roadmap
- Estimated 10x faster than traditional approach

### Slide 4: "What's Next"
- Phase 1: Ally Integration (proof of concept)
- Phase 2: AI Services + SafeAssign (real extractions)
- Phase 3: Event infrastructure for harder B2s

---

## 🎤 One-Liner to Open With

> "What if I told you that understanding 168 modules, identifying extraction candidates, and generating a production microservice scaffold — work that would take an architect team a quarter — can be done in a single afternoon with AI? Let me show you."
