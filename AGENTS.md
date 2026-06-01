# AGENTS.md

## Overview

Personal OpenCode workspace. Contains standalone mini-projects and PDF reports — no monorepo, no build system, no shared dependencies.

## Structure

- `todo-app/` — Single-file vanilla HTML/CSS/JS todo app (no build step, no framework)
- `*.pdf` — Review/metrics comparison documents (read-only artifacts)
- `.opencode/` — OpenCode runtime config, skills, plugins, agent definitions

## Multi-Agent Architecture (v2)

This workspace uses an **advanced multi-agentic pipeline** with intelligent mode selection, confidence scoring, and cross-pipeline learning:

```
User Input (vague or structured)
       │
       ▼
┌──────────────────────────────┐
│  1. Requirement Intake       │ ← Skill: requirement-intake
│     Transforms vague →       │    Handles ambiguity, asks questions
│     structured requirement   │    Produces pipeline-ready spec
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  2. Pattern Detection        │ ← Skill: pattern-detector
│     Classifies requirement   │    Type, complexity, risk
│     Selects pipeline mode    │    full-feature/quick-fix/hotfix/refactor
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  3. Orchestrator Routing     │ ← repos.yaml matching
│     Repo identification      │    Backend / Frontend / Both
│     + Lessons Learned query  │    Historical patterns surfaced
└──────────┬───────────────────┘
           │
           ▼ (mode-dependent stages)
┌──────────────────────────────────────────────────────────────────┐
│                    PIPELINE EXECUTION                              │
│                                                                    │
│  full-feature: Discovery → Implementation → Testing → Verification│
│  quick-fix:    Implementation → Verification                      │
│  hotfix:       Discovery(lite) → Implementation → Verification    │
│  refactor:     Discovery → Implementation → Verification          │
│                                                                    │
│  Each stage produces:                                              │
│    • Structured report with confidence scores                     │
│    • Timing metrics                                               │
│    • Teams notification                                           │
│    • Human approval gate                                          │
└──────────────────────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────┐
│  Final Composite Report      │ ← With confidence + timing + risks
│  + Lessons Learned Recording │ ← Patterns saved for future runs
│  + Teams Notification        │ ← Full report posted to channel
└──────────────────────────────┘
```

### Agent Definitions
Located in `.opencode/agents/`:
- `main-orchestrator.md` — Primary agent; intake, routing, mode selection, gating, assembles
- `discovery-agent.md` — Read-only analysis; produces implementation contract
- `implementation-agent.md` — Code changes; follows discovery contract
- `test-agent.md` — Test creation; follows implementation report
- `verification-agent.md` — Final review; produces PR-ready output
- `reviewer.md` — Code review agent

### Pipeline Modes
Located in `.opencode/modes/`:
- `full-feature.md` — Complete 4-stage pipeline (default for new features)
- `quick-fix.md` — Skip discovery + testing (typos, config, labels)
- `hotfix.md` — Lite discovery, skip test writing (critical bugs)
- `refactor.md` — Skip test writing, enhanced reference checking (structural changes)

### Report Templates
Defined in `.opencode/agent-report-templates.md` — includes confidence scoring and timing metadata.

### Target Repositories
Defined in `.opencode/repos.yaml` (single source of truth).
Agents read this file at session start to determine repo paths, stacks, and routing patterns.

## Skills

Located in `.opencode/skills/`:

### Pipeline Skills (orchestrator workflow)
| Skill | Purpose |
|-------|---------|
| `requirement-intake` | Transform vague input → structured requirement |
| `pattern-detector` | Classify requirement → select pipeline mode |
| `pipeline-summary` | Generate progress visualization at gates |
| `test-scaffold` | Pre-generate test boilerplate (speeds up test agent) |
| `pr-description` | Generate polished PR descriptions |
| `contract-validator` | Validate implementation matches discovery contract |
| `rollback` | Safely revert pipeline changes |
| `lessons-learned` | Record outcomes, build institutional knowledge |

### Integration Skills (external systems)
| Skill | Purpose |
|-------|---------|
| `jenkins-build-status` | Query Jenkins build status and failures |
| `ado-query` | Natural language Azure DevOps queries |

## Custom Tools

Located in `.opencode/tools/`:
| Tool | Purpose |
|------|---------|
| `compile-check.sh` | Run compilation and return structured JSON result |
| `test-runner.sh` | Run specific tests and return structured results |
| `diff-summarizer.sh` | Produce structured git diff summary |

## Knowledge Base (Cross-Pipeline Learning)

Located in `.opencode/knowledge/`:
- `pipeline-history.json` — Structured log of all pipeline runs
- `patterns/` — Discovered code patterns (reusable across features)
- `pitfalls/` — Common mistakes and how to avoid them
- `metrics/pipeline-metrics.json` — Aggregated timing and success metrics

## MCP Servers

- **code-rag** — Semantic code search (224K chunks indexed locally, ChromaDB)
  - Searches both Learn (backend) and Ultra (frontend) repos
  - Natural language queries → relevant code chunks
  - Runs 100% locally (no code leaves the machine)

## Key Facts

- **No package manager at root** — no `npm install` needed
- **No tests, no linter, no CI** — verify changes by opening HTML files directly
- **Not a git repo** (at workspace root) — no commit workflows apply
- **Model**: `github-copilot/claude-opus-4.6` (primary), `claude-haiku-4-5` (plan agent)
- **Pipeline modes**: 4 modes with automatic escalation from lighter to heavier
- **Confidence scoring**: Every agent reports 0-100% confidence with rationale
- **Learning**: Pipeline outcomes recorded for future improvement
- **Enterprise integration**: Teams notifications, ADO, Jenkins
