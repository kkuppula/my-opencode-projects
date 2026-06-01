# Multi-Agent Pipeline Workspace

> An enterprise-grade, multi-agent AI pipeline for automated code discovery, implementation, testing, and verification across multiple repositories.

![Pipeline](https://img.shields.io/badge/Pipeline-Multi--Agent-blue)
![OpenCode](https://img.shields.io/badge/OpenCode-Compatible-green)
![Claude](https://img.shields.io/badge/Model-Claude%20Opus%204.6-purple)
![MCP](https://img.shields.io/badge/MCP-Enabled-orange)

---

## Overview

This workspace implements a **multi-agentic pipeline** that takes vague user requirements and delivers production-ready code changes through intelligent orchestration. It supports multiple pipeline modes, cross-repo workflows, confidence scoring, and institutional learning.

```
User Input (vague or structured)
       │
       ▼
┌──────────────────────────────┐
│  1. Requirement Intake       │  Transforms vague → structured
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  2. Pattern Detection        │  Classifies → selects pipeline mode
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  3. Orchestrator Routing     │  Backend / Frontend / Both
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────────────┐
│                    PIPELINE EXECUTION                              │
│                                                                    │
│  full-feature: Discovery → Implementation → Testing → Verification│
│  quick-fix:    Implementation → Verification                      │
│  hotfix:       Discovery(lite) → Implementation → Verification    │
│  refactor:     Discovery → Implementation → Verification          │
└──────────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  Final Composite Report      │  Confidence scores + timing + PR
│  + Lessons Learned           │  Institutional knowledge saved
└──────────────────────────────┘
```

---

## Features

| Feature | Description |
|---------|-------------|
| **Intelligent Mode Selection** | Automatically picks the right pipeline mode based on requirement complexity |
| **Multi-Repo Support** | Orchestrates changes across backend + frontend simultaneously |
| **Confidence Scoring** | Every agent reports 0-100% confidence with rationale |
| **Human-in-the-Loop Gates** | Approval checkpoints between every stage |
| **Institutional Learning** | Records patterns, pitfalls, and outcomes for future improvement |
| **Contract Validation** | Verifies implementation matches discovery contract |
| **Rollback Support** | Safely reverts all changes if something goes wrong |
| **Teams Notifications** | Posts pipeline progress to Microsoft Teams channels |
| **Semantic Code Search** | Local code-rag MCP server (ChromaDB) for natural language code queries |

---

## Pipeline Modes

| Mode | When to Use | Stages | Time |
|------|-------------|--------|------|
| `full-feature` | New features, complex changes | Discovery → Implementation → Testing → Verification | ~10-15 min |
| `quick-fix` | Typos, labels, config values | Implementation → Verification | ~2-3 min |
| `hotfix` | Critical production bugs | Discovery(lite) → Implementation → Verification | ~5-8 min |
| `refactor` | Structural changes, no behavior change | Discovery → Implementation → Verification | ~8-12 min |

Modes automatically **escalate** when complexity exceeds expectations (e.g., quick-fix → full-feature if >2 files need changes).

---

## Directory Structure

```
.
├── .opencode/                          # Pipeline infrastructure
│   ├── agents/                         # Agent definitions (6)
│   │   ├── main-orchestrator.md        #   Primary routing & gating
│   │   ├── discovery-agent.md          #   Read-only code analysis
│   │   ├── implementation-agent.md     #   Code modifications
│   │   ├── test-agent.md              #   Test creation
│   │   ├── verification-agent.md      #   Final review & PR prep
│   │   └── reviewer.md               #   Code review
│   ├── skills/                         # Reusable skill modules (10)
│   │   ├── requirement-intake/        #   Vague → structured requirements
│   │   ├── pattern-detector/          #   Requirement classification
│   │   ├── pipeline-summary/          #   Progress visualization
│   │   ├── contract-validator/        #   Implementation compliance
│   │   ├── pr-description/            #   PR text generation
│   │   ├── test-scaffold/             #   Test boilerplate generation
│   │   ├── rollback/                  #   Safe change reversion
│   │   ├── lessons-learned/           #   Outcome recording
│   │   ├── ado-query/                 #   Azure DevOps queries
│   │   └── jenkins-build-status-report/ # CI/CD status
│   ├── tools/                          # Shell tools (3)
│   │   ├── compile-check.sh           #   Verify compilation
│   │   ├── test-runner.sh             #   Run specific tests
│   │   └── diff-summarizer.sh         #   Structured diff output
│   ├── modes/                          # Pipeline mode definitions (5)
│   │   ├── full-feature.md
│   │   ├── quick-fix.md
│   │   ├── hotfix.md
│   │   ├── refactor.md
│   │   └── analyze.md
│   ├── scripts/                        # Automation scripts
│   │   └── notify-teams.sh           #   Teams webhook notifications
│   ├── knowledge/                      # Institutional memory
│   │   ├── pipeline-history.json      #   All pipeline runs
│   │   ├── patterns/                  #   Discovered code patterns
│   │   ├── pitfalls/                  #   Common mistakes
│   │   └── metrics/                   #   Aggregated performance data
│   ├── mcp-servers/                    # MCP server implementations
│   │   └── code-rag/                  #   Semantic code search (ChromaDB)
│   ├── repos.yaml                      # Target repository definitions
│   ├── agent-report-templates.md       # Report format standards
│   └── requirement-template.md         # Requirement document template
├── reports/                            # Pipeline output reports
│   ├── 01-discovery-report.md
│   ├── 02-implementation-report.md
│   ├── 03-test-report.md
│   ├── 04-verification-report.md
│   └── 05-final-composite-report.md
├── demo-ai-microservice/              # Demo: Spring Boot AI service
├── multi-agent-workflow/              # Workflow documentation
├── todo-app/                          # Demo: Vanilla JS todo app
├── log-ocean/                         # Demo: Log visualization
├── pr-dashboard/                      # Demo: PR metrics dashboard
├── AGENTS.md                          # Architecture documentation
├── opencode.json                      # OpenCode configuration
└── tui.json                           # TUI settings
```

---

## Agent Architecture

### Agent Roles & Permissions

| Agent | Model | Role | Can Write? |
|-------|-------|------|:----------:|
| **Orchestrator** | Claude Opus 4.6 | Routes, gates, assembles reports | Reports only |
| **Discovery** | Claude Opus 4.6 | Analyzes code, produces contracts | ❌ Read-only |
| **Implementation** | Claude Sonnet 4.5 | Makes code changes | ✅ |
| **Test** | Claude Sonnet 4.5 | Creates/updates tests | ✅ |
| **Verification** | Claude Sonnet 4.5 | Reviews changes, runs checks | ❌ Read-only |
| **Reviewer** | Claude Sonnet 4.5 | Code review feedback | ❌ Read-only |

### Gate System

Every stage transition requires human approval:

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🛑 GATE [N]: [Stage] Complete — Approval Required
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 Stage Metrics:
   ⏱️  Duration: [X] seconds
   🎯 Confidence: [X]%
   📁 Files changed: [N]
   ⚠️  Risks: [N]

→ ✅ Proceed  │  🔄 Redo  │  ✏️ Correct  │  ⏹️ Stop  │  🔙 Rollback
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## Skills Reference

### Pipeline Skills

| Skill | Purpose | Loaded When |
|-------|---------|-------------|
| `requirement-intake` | Transform vague user statements into structured specs | Pipeline start (unclear input) |
| `pattern-detector` | Classify requirement → select mode | After structuring |
| `pipeline-summary` | Generate progress visualization | At each gate |
| `contract-validator` | Verify implementation matches discovery | After implementation |
| `test-scaffold` | Pre-generate test boilerplate (~40% speedup) | Before test agent |
| `pr-description` | Generate polished PR description | Final stage |
| `rollback` | Safely revert all pipeline changes | On failure or request |
| `lessons-learned` | Record outcomes for future runs | After pipeline completes |

### Integration Skills

| Skill | Purpose | External System |
|-------|---------|-----------------|
| `ado-query` | Natural language Azure DevOps queries | Azure DevOps |
| `jenkins-build-status` | Build status, failure analysis | Jenkins CI |

---

## MCP Servers

### code-rag (Semantic Code Search)

A locally-running MCP server that provides natural language code search using ChromaDB vector embeddings.

| Tool | Description |
|------|-------------|
| `search_code` | Search code with natural language queries |
| `find_related` | Find files semantically related to a given file |
| `get_context` | Expand context around a search result |
| `index_stats` | View indexing statistics |

**Key facts:**
- 224K+ chunks indexed across multiple repos
- 100% local execution — code never leaves your machine
- Supports multiple repos (Java, TypeScript, etc.)

See [`.opencode/mcp-servers/code-rag/README.md`](.opencode/mcp-servers/code-rag/README.md) for setup instructions.

---

## Setup

### Prerequisites

- [OpenCode](https://opencode.ai) CLI installed
- GitHub Copilot subscription (for Claude models)
- Python 3.10+ (for code-rag MCP server)

### Quick Start

```bash
# Clone the repo
git clone https://github.com/kkuppula/my-opencode-projects.git
cd my-opencode-projects

# Set up code-rag (optional — for semantic search)
cd .opencode/mcp-servers/code-rag
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
# Edit config.yaml with your repo paths
./run-indexer.sh
cd ../../..

# Launch OpenCode
opencode
```

### Configuration

Edit `opencode.json` to customize:
- Model assignments per agent
- MCP server connections
- Permission boundaries
- Tool access

Edit `.opencode/repos.yaml` to define your target repositories:

```yaml
repos:
  backend:
    name: MyBackend
    path: /path/to/backend
    stack: Java/Gradle
    patterns:
      - "API", "service", "controller", "repository"
  frontend:
    name: MyFrontend
    path: /path/to/frontend
    stack: TypeScript/Angular
    patterns:
      - "UI", "component", "page", "view"
```

---

## Teams Integration

Pipeline notifications are sent to Microsoft Teams via Incoming Webhook:

```bash
# Set your webhook URL
export TEAMS_WEBHOOK_URL="https://your-org.webhook.office.com/..."

# Notifications are sent automatically at each pipeline stage
```

Notifications include:
- Pipeline start/completion
- Stage transitions
- Risk alerts
- Full report content (as Adaptive Cards)

---

## Demo Projects

| Project | Description | Stack |
|---------|-------------|-------|
| `demo-ai-microservice/` | Spring Boot AI service with playground, settings, and model usage tracking | Java, Spring Boot, PostgreSQL |
| `todo-app/` | Single-file vanilla todo app (no build step) | HTML/CSS/JS |
| `log-ocean/` | Log visualization dashboard | HTML/JS |
| `pr-dashboard/` | Pull request metrics dashboard | HTML/JS |

---

## Example Pipeline Run

```
User: "Add a Last Login column to the user roster page"

📋 Classification:
   Type: Feature │ Complexity: 6/10 │ Target: Both │ Mode: full-feature

🔍 Discovery (92% confidence):
   → Backend API already exposes `lastAccessed` field
   → Frontend model has `lastAccessDate` mapped but not rendered
   → Only frontend template change needed

🛠️ Implementation (85% confidence):
   → Added column to course-roster.html (+9 lines)
   → Rebalanced column widths (5+2+2+2=11)
   → Used bb-date directive for formatting

🧪 Testing:
   → Template rendering tests
   → Null state ("Never") coverage
   → Responsive breakpoint tests

✅ Verification:
   → Contract compliance: 100%
   → No regressions
   → PR description generated
```

---

## Knowledge Base

The pipeline builds institutional knowledge over time:

| Type | Location | Purpose |
|------|----------|---------|
| **Patterns** | `.opencode/knowledge/patterns/` | Reusable code patterns (e.g., "how to add a roster column") |
| **Pitfalls** | `.opencode/knowledge/pitfalls/` | Common mistakes and how to avoid them |
| **History** | `.opencode/knowledge/pipeline-history.json` | Structured log of all pipeline runs |
| **Metrics** | `.opencode/knowledge/metrics/` | Timing, success rates, confidence trends |

This knowledge is consulted at the start of each pipeline run to inform decisions and avoid repeating past mistakes.

---

## License

Private — Internal use only.
