---
name: pattern-detector
description: "Classifies requirements by type and selects the optimal pipeline mode. Analyzes requirement complexity, target repo, and risk to determine whether to use full-feature, quick-fix, hotfix, or refactor mode. Use when: classify requirement, what pipeline mode, how complex is this, should I skip discovery, requirement classification, complexity analysis."
argument-hint: "e.g. 'classify: add a new REST endpoint for AI toggle' or 'what mode for: fix typo in error message'"
---

# Pattern Detector Skill

Classify requirements and select the optimal pipeline execution mode.

## Purpose

Not every requirement needs the full 4-stage pipeline. This skill analyzes the requirement and recommends the most efficient execution path, saving time and reducing noise.

## Classification Matrix

### Requirement Type Detection

| Signal | Type | Examples |
|--------|------|----------|
| "add", "create", "implement", "build", "new" | **Feature** | "Add AI toggle to course settings" |
| "fix", "broken", "doesn't work", "error", "bug" | **Bug Fix** | "Fix null pointer in grade calculation" |
| "rename", "move", "restructure", "extract", "simplify" | **Refactor** | "Rename getUserById to findUserById" |
| "change text", "update label", "fix typo" | **Cosmetic** | "Fix typo in error message" |
| "config", "property", "flag", "toggle existing" | **Configuration** | "Enable feature flag for new module" |
| "urgent", "production", "hotfix", "critical" | **Hotfix** | "Critical: users can't login" |

### Complexity Scoring

Score 1-10 based on:

| Factor | Low (1-3) | Medium (4-6) | High (7-10) |
|--------|-----------|--------------|-------------|
| **Files affected** | 1-2 files | 3-5 files | 6+ files |
| **Cross-layer** | Single layer (UI only) | 2 layers (API + service) | 3+ layers (DB + API + UI) |
| **Behavior change** | None / cosmetic | Modified behavior | New behavior |
| **Data model** | No changes | New field | New entity/table |
| **API surface** | No change | Modified endpoint | New endpoint |
| **Risk of regression** | None | Some existing tests | Core functionality |
| **Pattern precedent** | Exact match exists | Similar exists | Novel |

**Complexity Score = average of applicable factors**

### Pipeline Mode Selection

```
┌──────────────────────────────────────────────────────────────┐
│                    PIPELINE MODE SELECTOR                      │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  Complexity ≤ 2 AND Type = Cosmetic/Config                   │
│    → MODE: quick-fix                                          │
│    → Stages: Implementation → Verification                    │
│    → Skip: Discovery, Testing (run existing tests only)       │
│                                                               │
│  Type = Hotfix AND Urgency = Critical                         │
│    → MODE: hotfix                                             │
│    → Stages: Discovery(lite) → Implementation → Verification  │
│    → Skip: New test writing (run existing only)               │
│                                                               │
│  Type = Refactor AND Behavior change = None                   │
│    → MODE: refactor                                           │
│    → Stages: Discovery → Implementation → Verification        │
│    → Skip: New test writing (existing tests validate)         │
│                                                               │
│  All other cases                                              │
│    → MODE: full-feature                                       │
│    → Stages: Discovery → Implementation → Testing → Verify    │
│    → Skip: Nothing                                            │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

## Output Format

```markdown
# Requirement Classification

## Input
> [Original requirement text]

## Classification
| Dimension | Value | Confidence | Rationale |
|-----------|-------|------------|-----------|
| Type | Feature / Bug Fix / Refactor / Cosmetic / Config / Hotfix | High/Med/Low | Why |
| Complexity | [1-10] ([label]) | High/Med/Low | Based on [factors] |
| Target Repo | Backend / Frontend / Both | High/Med/Low | Matched patterns: [list] |
| Risk Level | Low / Medium / High | -- | [explanation] |
| Behavior Change | None / Modified / New | -- | [explanation] |

## Recommended Pipeline Mode

**Mode: `[full-feature / quick-fix / hotfix / refactor]`**

### Stages to Execute
| # | Stage | Execute? | Reason |
|---|-------|----------|--------|
| 1 | Discovery | ✅/⏭️ Skip/🔅 Lite | [reason] |
| 2 | Implementation | ✅ | Always required |
| 3 | Testing | ✅/⏭️ Skip | [reason] |
| 4 | Verification | ✅ | Always required |

### Time Estimate
- **Estimated total time:** [X minutes]
- **Full-feature would take:** [Y minutes]
- **Time saved:** [Z minutes] ([N]%)

## Additional Recommendations
- [Any special handling needed]
- [Warnings or considerations]
```

## Quick Decision Rules

For the orchestrator to use without loading the full skill:

```
# One-liner classification (for simple cases)
"fix typo"           → quick-fix (skip discovery + testing)
"rename X to Y"     → refactor (skip testing)
"add new endpoint"  → full-feature
"production bug"    → hotfix (lite discovery, skip new tests)
"update config"     → quick-fix
"new feature"       → full-feature
"UI component"      → full-feature
"change label text" → quick-fix
```

## Rules

- When in doubt, recommend `full-feature` (safer)
- NEVER recommend `quick-fix` for behavior changes
- NEVER recommend skipping verification (always required)
- Hotfix mode still requires discovery (just lighter/faster)
- If complexity > 5, always use `full-feature`
- If cross-repo (both backend + frontend), always use `full-feature`
