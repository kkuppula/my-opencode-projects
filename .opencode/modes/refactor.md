---
description: "Refactor mode for structural changes with no behavior modification. Relies on existing tests for validation."
mode: agent
model: github-copilot/claude-opus-4.6
temperature: 0.1
---

# Refactor Pipeline Mode

For structural improvements that don't change observable behavior.

## When to Use

- Renaming variables, methods, classes, or files
- Extracting methods or classes
- Moving code between packages/modules
- Simplifying complex logic (same behavior)
- Removing dead code
- Improving naming consistency

## Pipeline Stages

```
[Discovery] → [Implementation] → [Verification (enhanced)]
   (full discovery — need to understand all references)
   (skip new test writing — existing tests validate behavior is unchanged)
   (enhanced verification — ensure ALL references updated, no broken imports)
```

## Discovery Behavior for Refactor

When invoking the discovery agent in refactor mode, modify the prompt:

```
MODE: REFACTOR — Reference Analysis

Focus on:
1. ALL references to the symbol(s) being refactored
2. All files that import/use the affected code
3. Test files that reference the symbol(s)
4. Configuration files that reference the symbol(s)
5. String references (e.g., reflection, serialization, message keys)
6. Documentation/comments that mention the symbol(s)

Produce a COMPLETE list of files that need updating.
Missing even one reference = broken build.

Key question: "If I rename X to Y, what breaks?"
```

## Implementation Behavior for Refactor

```
MODE: REFACTOR — Structural Change Only

Rules:
- Change structure/naming ONLY — no behavior changes
- Update ALL references found in discovery
- Update ALL imports
- Update ALL test references
- Update documentation/comments that reference old names
- If you find a file that discovery missed, update it AND report it
- Do NOT "improve" logic while refactoring
- Do NOT add features while refactoring
- The existing test suite MUST pass unchanged (beyond reference updates)
```

## Verification Behavior for Refactor

```
MODE: REFACTOR — Enhanced Reference Check

Focus on:
1. Are ALL references updated? (grep for old name — should return 0 results)
2. Does it compile?
3. Do ALL existing tests pass?
4. Are imports consistent?
5. Are there any remaining references to the old name?
6. String literals, reflection, serialization — checked?

Critical check: `grep -r "oldName" . --include="*.java"` should return NOTHING
```

## Gate Behavior

- **3 gates** (after discovery, after implementation, after verification)
- Gate 1: "Found [N] references across [M] files. Ready to refactor?"
- Gate 2: "All [N] references updated. Running verification..."
- Gate 3: "All tests pass. No remaining references to old name. Approve?"

## Guard Rails

- If any existing test fails → **STOP** (refactor broke behavior)
- If grep still finds old references after implementation → **flag as incomplete**
- If the refactor touches > 20 files → warn about risk
- If refactor touches public API → require explicit approval

## Time Expectation

- Target: **< 2 minutes** (similar to full-feature minus test writing)
- Key cost: Discovery takes longer (must find ALL references)

## No New Tests Policy

Refactors explicitly skip new test writing because:
1. Existing tests validate behavior is unchanged
2. If existing tests pass → refactor is safe
3. If existing tests fail → refactor broke something (fix it)
4. Adding tests during refactor conflates two concerns
