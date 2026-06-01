# OpenCode Multi-Agent Workflow

> A 4-agent orchestration pipeline for AI-assisted code changes in production codebases.

![OpenCode](https://img.shields.io/badge/OpenCode-Workflow-blue)
![Claude](https://img.shields.io/badge/Claude-Opus%204-purple)
![Multi-Agent](https://img.shields.io/badge/Multi--Agent-Pipeline-green)

---

## What It Does

This workflow coordinates **4 specialized AI agents** to implement code changes safely and systematically:

```
User Requirement
       │
       ▼
┌──────────────────┐
│  Orchestrator    │ ← Routes tasks, enforces boundaries, assembles reports
└──────┬───────────┘
       │
       ├──▶ Discovery Agent     → Analyzes codebase, identifies risks, creates contract
       │
       ├──▶ Implementation Agent → Makes minimal safe code changes
       │
       ├──▶ Test Agent          → Adds/updates tests following existing patterns
       │
       └──▶ Verification Agent  → Runs checks, reviews diff, prepares PR
       │
       ▼
┌──────────────────┐
│ Final Composite  │ ← All reports assembled, PR-ready
│    Report        │
└──────────────────┘
```

---

## Why Multi-Agent?

| Problem | Solution |
|---------|----------|
| AI jumps to coding without understanding context | **Discovery first** — analyze before implementing |
| Broad, risky refactors | **Scoped changes** — implementation follows discovery contract |
| Missing test coverage | **Dedicated test agent** — tests are a first-class phase |
| Incomplete verification | **Verification agent** — runs checks, reviews diff, validates behavior |
| No audit trail | **Structured reports** — every phase produces artifacts |

---

## Agents

### 1. Discovery Agent (Read-Only)

Analyzes the requirement and existing codebase **without making changes**:

- Identifies relevant code paths
- Lists files likely to change
- Finds existing patterns to follow
- Assesses risks and edge cases
- Produces implementation contract

**Output:** `discovery-report.md`

### 2. Implementation Agent

Makes the minimal code changes required:

- Follows discovery contract strictly
- Uses existing patterns
- Avoids broad refactoring
- Preserves existing behavior

**Output:** `implementation-report.md`

### 3. Test Agent

Adds or updates tests:

- Follows existing test conventions
- Covers new behavior and regressions
- Includes edge cases from discovery
- Keeps tests deterministic

**Output:** `test-report.md`

### 4. Verification Agent

Final quality gate:

- Reviews git diff
- Runs tests/lint/build
- Validates changes match requirement
- Prepares PR description

**Output:** `verification-report.md`

---

## Installation

### 1. Copy agents to your OpenCode workspace

```bash
cp -r agents/ ~/.opencode/agents/
```

### 2. Add to your AGENTS.md or opencode.json

```json
{
  "agent": {
    "discovery-agent": {
      "model": "github-copilot/claude-opus-4.5",
      "permission": {
        "*": "allow",
        "edit": "deny",
        "write": "deny"
      }
    },
    "implementation-agent": {
      "model": "github-copilot/claude-opus-4.5"
    },
    "test-agent": {
      "model": "github-copilot/claude-opus-4.5"
    },
    "verification-agent": {
      "model": "github-copilot/claude-opus-4.5"
    }
  }
}
```

### 3. Use the workflow

```
You: Implement feature X following the multi-agent workflow

OpenCode: Starting Discovery phase...
         [Discovery Agent analyzes codebase]
         
         Discovery complete. Proceeding to Implementation...
         [Implementation Agent makes changes]
         
         Implementation complete. Proceeding to Testing...
         [Test Agent adds tests]
         
         Testing complete. Proceeding to Verification...
         [Verification Agent validates everything]
         
         ✅ Ready for PR
```

---

## Files

```
multi-agent-workflow/
├── README.md                    # This file
├── WORKFLOW.md                  # Detailed workflow documentation
└── agents/
    ├── main-orchestrator.md     # Main coordinator agent
    ├── discovery-agent.md       # Analysis agent (read-only)
    ├── implementation-agent.md  # Code change agent
    ├── test-agent.md            # Test writing agent
    ├── verification-agent.md    # Final validation agent
    └── reviewer.md              # Code review agent
```

---

## Report Templates

Each agent produces a structured Markdown report:

### Discovery Report
```markdown
# Discovery Report
## Requirement Summary
## Relevant Code Areas
## Files Likely To Change
## Existing Patterns To Follow
## Proposed Implementation Approach
## Risks and Edge Cases
## Test Strategy
```

### Implementation Report
```markdown
# Implementation Report
## Files Changed
## Behavior Changed
## Compatibility Notes
## Assumptions
```

### Test Report
```markdown
# Test Report
## Tests Added
## Tests Updated
## Coverage Matrix
## Edge Cases Covered
```

### Verification Report
```markdown
# Verification Report
## Requirement Match
## Commands Run
## Diff Review Notes
## PR Title & Description
## Ready For PR?
```

---

## Guardrails

The workflow enforces safety through explicit rules:

| Rule | Purpose |
|------|---------|
| Discovery before implementation | Understand before changing |
| Follow existing patterns | Maintain codebase consistency |
| No unrelated refactors | Keep changes focused |
| Implementation follows contract | Prevent scope creep |
| Tests required | Ensure regression safety |
| Verification gate | Final quality check |

---

## Example Use Cases

| Scenario | How It Helps |
|----------|--------------|
| New feature | Discovery finds existing patterns; Implementation follows them |
| Bug fix | Discovery identifies root cause; Tests prevent regression |
| Refactoring | Discovery assesses blast radius; Verification validates behavior unchanged |
| API change | Discovery checks all callers; Tests cover migration |

---

## Requirements

- [OpenCode](https://opencode.ai) or compatible AI coding assistant
- Claude Opus 4 (recommended) or equivalent model
- Git repository with existing codebase

---

## License

MIT

---

*Designed for production codebases where correctness, patterns, performance, and test coverage matter.*
