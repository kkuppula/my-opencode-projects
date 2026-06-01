---
description: "Full feature mode - complete 4-stage pipeline with all gates. Default mode for new features and complex changes."
mode: agent
model: github-copilot/claude-opus-4.6
temperature: 0.1
---

# Full-Feature Pipeline Mode (Default)

The complete 4-stage pipeline for new features and complex changes.

## When to Use

- New features or capabilities
- Behavior changes (existing functionality modified)
- Multi-file changes (4+ files)
- Changes that touch data model or API surface
- Complexity score > 5
- Cross-repo (backend + frontend) changes
- When in doubt — this is the safe default

## Pipeline Stages

```
[Discovery] → [Implementation] → [Testing] → [Verification]
   (full analysis)  (full implementation)  (new tests)   (complete review)
   
   🛑 Gate 1      🛑 Gate 2           🛑 Gate 3      🛑 Gate 4
```

## All Agents Run Full Scope

- **Discovery**: Complete architecture analysis, pattern identification, risk assessment
- **Implementation**: Full implementation with contract compliance
- **Testing**: New unit/integration tests written
- **Verification**: Complete checklist, compilation, PR description

## Gate Behavior

- **4 gates** with full approval workflow
- Each gate presents: summary, key findings, confidence, next step preview
- User can: Proceed / Redo / Correct / Stop

## Time Expectation

- Target: **2-4 minutes** (depending on complexity)
- Discovery: ~30-45s
- Implementation: ~30-60s
- Testing: ~30-45s
- Verification: ~20-30s

## This is the DEFAULT

If the pattern-detector skill is not loaded or returns uncertain results, fall back to full-feature mode. It's always safe.
