# Multi-Agent Pipeline — OpenCode Configuration

> Enterprise-grade, multi-agent AI pipeline for automated code discovery, implementation, testing, and verification across multiple repositories.

![Pipeline](https://img.shields.io/badge/Pipeline-Multi--Agent-blue)
![OpenCode](https://img.shields.io/badge/OpenCode-Compatible-green)
![Claude](https://img.shields.io/badge/Model-Claude%20Opus%204-purple)
![MCP](https://img.shields.io/badge/MCP-Enabled-orange)

---

## What This Is

This repo contains the **`.opencode/` pipeline infrastructure** — skills, tools, agents, modes, and knowledge that power a multi-agent AI coding pipeline. Drop this into any workspace to get intelligent code automation.

```
User Input (vague or structured)
       │
       ▼
┌──────────────────────────────┐
│  1. Requirement Intake       │  Transforms vague → structured
└──────────┬───────────────────┘
           ▼
┌──────────────────────────────┐
│  2. Pattern Detection        │  Classifies → selects pipeline mode
└──────────┬───────────────────┘
           ▼
┌──────────────────────────────┐
│  3. Orchestrator Routing     │  Backend / Frontend / Both
└──────────┬───────────────────┘
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
│  Final Report + Lessons      │  Confidence scores + timing + PR
└──────────────────────────────┘
```

---

## Structure

```
.opencode/
├── agents/                         # Agent definitions (6)
│   ├── main-orchestrator.md        #   Routes, gates, assembles
│   ├── discovery-agent.md          #   Read-only code analysis
│   ├── implementation-agent.md     #   Code modifications
│   ├── test-agent.md              #   Test creation
│   ├── verification-agent.md      #   Final review & PR prep
│   └── reviewer.md               #   Code review
├── skills/                         # Reusable skill modules (11)
│   ├── requirement-intake/        #   Vague → structured requirements
│   ├── pattern-detector/          #   Requirement classification
│   ├── context-compressor/        #   Inter-agent token optimization
│   ├── pipeline-summary/          #   Progress visualization
│   ├── contract-validator/        #   Implementation compliance
│   ├── pr-description/            #   PR text generation
│   ├── test-scaffold/             #   Test boilerplate (~40% speedup)
│   ├── rollback/                  #   Safe change reversion
│   ├── lessons-learned/           #   Outcome recording
│   ├── ado-query/                 #   Azure DevOps queries
│   └── jenkins-build-status-report/ # CI/CD status
├── tools/                          # Shell tools (3)
│   ├── compile-check.sh           #   Verify compilation
│   ├── test-runner.sh             #   Run specific tests
│   └── diff-summarizer.sh         #   Structured diff output
├── modes/                          # Pipeline mode definitions (5)
│   ├── full-feature.md            #   Complete 4-stage pipeline
│   ├── quick-fix.md               #   Skip discovery + testing
│   ├── hotfix.md                  #   Lite discovery, skip new tests
│   ├── refactor.md                #   Skip testing, enhanced ref-check
│   └── analyze.md                 #   Read-only analysis mode
├── scripts/
│   └── notify-teams.sh            #   Teams webhook notifications
├── knowledge/                      # Institutional memory
│   ├── pipeline-history.json      #   All pipeline runs
│   ├── patterns/                  #   Discovered code patterns
│   ├── pitfalls/                  #   Common mistakes
│   └── metrics/                   #   Performance data
├── mcp-servers/
│   └── code-rag/                  #   Semantic code search (ChromaDB)
├── repos.yaml                      #   Target repository config
├── agent-report-templates.md       #   Report format standards
└── requirement-template.md         #   Requirement document template
```

---

## Pipeline Modes

| Mode | Stages | When to Use |
|------|--------|-------------|
| `full-feature` | Discovery → Implementation → Testing → Verification | New features, complex changes |
| `quick-fix` | Implementation → Verification | Typos, labels, config values |
| `hotfix` | Discovery(lite) → Implementation → Verification | Critical production bugs |
| `refactor` | Discovery → Implementation → Verification | Structural changes, no behavior change |

Modes automatically **escalate** when complexity exceeds expectations.

---

## Agents

| Agent | Role | Can Write? |
|-------|------|:----------:|
| **Orchestrator** | Routes, gates, assembles reports | Reports only |
| **Discovery** | Analyzes code, produces implementation contracts | ❌ |
| **Implementation** | Makes code changes per contract | ✅ |
| **Test** | Creates/updates tests | ✅ |
| **Verification** | Reviews changes, runs checks | ❌ |
| **Reviewer** | Code review feedback | ❌ |

---

## Skills

| Skill | Purpose |
|-------|---------|
| `requirement-intake` | Transform vague input → structured requirement |
| `pattern-detector` | Classify requirement → select pipeline mode |
| `context-compressor` | Compress reports → minimal inter-agent payloads (50-70% token savings) |
| `pipeline-summary` | Generate progress visualization at gates |
| `contract-validator` | Validate implementation matches discovery contract |
| `pr-description` | Generate polished PR descriptions |
| `test-scaffold` | Pre-generate test boilerplate |
| `rollback` | Safely revert pipeline changes |
| `lessons-learned` | Record outcomes for future improvement |
| `ado-query` | Natural language Azure DevOps queries |
| `jenkins-build-status` | CI/CD build status and failure analysis |

---

## Tools

| Tool | Usage |
|------|-------|
| `compile-check.sh` | `.opencode/tools/compile-check.sh <path> <stack>` |
| `test-runner.sh` | `.opencode/tools/test-runner.sh <path> <stack> <target>` |
| `diff-summarizer.sh` | `.opencode/tools/diff-summarizer.sh <path> [base_ref]` |

---

## MCP Server: code-rag

Locally-running semantic code search using ChromaDB vector embeddings.

| Tool | Description |
|------|-------------|
| `search_code` | Natural language code search |
| `find_related` | Find semantically related files |
| `get_context` | Expand context around results |
| `index_stats` | View indexing statistics |

100% local — code never leaves your machine. See [`.opencode/mcp-servers/code-rag/README.md`](.opencode/mcp-servers/code-rag/README.md) for setup.

---

## Setup

### 1. Copy `.opencode/` into your workspace

```bash
git clone https://github.com/kkuppula/my-opencode-projects.git
cp -r my-opencode-projects/.opencode /path/to/your/workspace/
```

### 2. Configure target repositories

Edit `.opencode/repos.yaml`:

```yaml
repos:
  backend:
    name: MyBackend
    path: /path/to/your/backend
    stack: Java/Gradle
    patterns:
      - "API", "service", "controller", "repository"
  frontend:
    name: MyFrontend
    path: /path/to/your/frontend
    stack: TypeScript/Angular
    patterns:
      - "UI", "component", "page", "view"
```

### 3. Set up code-rag (optional)

```bash
cd .opencode/mcp-servers/code-rag
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
# Edit config.yaml with your repo paths
./run-indexer.sh
```

### 4. Configure Teams notifications (optional)

```bash
export TEAMS_WEBHOOK_URL="https://your-org.webhook.office.com/..."
```

---

## Key Features

- **Intelligent mode selection** — auto-picks the right pipeline depth
- **Multi-repo orchestration** — coordinates backend + frontend changes
- **Context compression** — 50-70% token reduction between stages (see below)
- **Confidence scoring** — every agent reports 0-100% with rationale
- **Human-in-the-loop gates** — approval checkpoints between stages
- **Institutional learning** — records patterns and pitfalls for future runs
- **Contract validation** — ensures implementation matches discovery
- **Safe rollback** — reverts changes cleanly on failure
- **Teams notifications** — posts progress to Microsoft Teams

---

## Context Compression Layer

A key optimization that sits **between pipeline stages** to reduce token waste by 50-70%.

### The Problem

Without compression, each agent receives the full output of all prior stages — most of which is irrelevant prose, rationale, and verbose code snippets. Tokens compound through the pipeline:

```
Without compression:                    With compression:
Discovery Agent:    ~15K tokens         Discovery Agent:    ~6K tokens
Implementation:     ~25K tokens         Implementation:     ~8K tokens
Test Agent:         ~30K tokens         Test Agent:         ~6K tokens
Verification:       ~35K tokens         Verification:       ~6K tokens
────────────────────────────────        ──────────────────────────────
Total:              ~100K+ tokens       Total:              ~26K tokens
                                        Savings:            ~70%
```

### How It Works

```
Stage N Report (full, human-readable)
       │
       ├── Saved to reports/ (for human gates & debugging)
       │
       └── Compressed via rule-based extraction
               │
               ▼
       Structured JSON Contract (1-3K tokens)
               │
               └── Passed to Stage N+1 Agent
```

### Compression Per Transition

| Transition | What's Kept | What's Dropped | Savings |
|-----------|-------------|---------------|---------|
| Discovery → Implementation | Must Do list, file paths, API contract, patterns | Rationale, alternatives, journey narrative | ~60-75% |
| Implementation → Test | Files changed, behavior added, edge cases | Decisions, contract validation, discovery | ~65-80% |
| Implementation → Verification | Contract checklist, diff ref, commands | Full reports, prose, code-rag results | ~70-80% |
| Test → Verification | Test results, coverage gaps | Test code, mock setup, framework config | ~75% |

### Example: Discovery → Implementation

```
❌ Without compression (8K tokens):
   "The discovery agent investigated the codebase using code-rag semantic
   search. We queried 'user roster course membership' which returned 5 
   results from the Learn repository. After analyzing MembershipTOPubV1.java
   we found that the lastAccessed field has been available since version
   3300.9.0 and is stored in the course_users.last_access_date column..."

✅ With compression (2K tokens):
   {
     "must_do": [
       {"action": "Add column header", "file": "course-roster.html", "lines": "289-307"}
     ],
     "api_contract": {"field": "lastAccessDate", "type": "Date|null"},
     "files_to_modify": ["course-roster.html"],
     "patterns": [{"ref": "course-grades-student.html:209", "note": "bb-date usage"}],
     "guard_rails": ["hide-for-small", "No API changes", "No model changes"]
   }
```

### Token Budgets Per Agent

| Agent | Max Context | Breakdown |
|-------|-------------|-----------|
| Discovery | 6K tokens | Requirement (1K) + Instructions (3K) + Repo context (2K) |
| Implementation | 8K tokens | Compressed contract (2K) + Instructions (3K) + Code refs (3K) |
| Test | 6K tokens | Compressed impl (1.5K) + Instructions (3K) + Patterns (1.5K) |
| Verification | 6K tokens | Compressed state (2K) + Instructions (3K) + Diff (1K) |

### Design Principles

- **No LLM call needed** — compression is rule-based extraction, not summarization
- **Full reports always preserved** — humans see everything at gates; only agents get compressed versions
- **Fallback expansion** — if an agent reports confidence < 60%, the orchestrator expands the missing section
- **Never drop actionable items** — Must Do, file paths, and API contracts always pass through

---

## License

MIT License — Copyright (c) 2026 Kiran Kuppula

See [LICENSE](LICENSE) for details.
