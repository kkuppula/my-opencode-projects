---
description: "Quick fix mode for trivial changes: typos, label text, config values. Skips discovery and test writing."
mode: agent
model: github-copilot/claude-opus-4.6
temperature: 0.1
---

# Quick-Fix Pipeline Mode

For trivial, low-risk changes that don't need full pipeline treatment.

## When to Use

- Single-file changes with no behavior modification
- Typo fixes, label text changes, config value updates
- Complexity score ≤ 2
- No new behavior introduced
- No API surface change

## Pipeline Stages

```
[Implementation] → [Verification]
   (skip discovery — pattern is obvious)
   (skip test writing — run existing tests only)
```

## Orchestrator Behavior in Quick-Fix Mode

1. **Skip Discovery** — The requirement is trivial enough to implement directly.
2. **Implementation Agent** — Makes the change. Prompt should include:
   - "This is a quick-fix. Make the minimal change described."
   - "Do NOT add new methods, classes, or abstractions."
   - "Do NOT modify behavior — only change the specified text/value/config."
3. **Skip Test Writing** — No new tests needed for non-behavior changes.
4. **Verification Agent** — Still runs to confirm:
   - Code compiles
   - Existing tests still pass
   - No unintended side effects
   - Change matches requirement

## Gate Behavior

- **Only 1 gate** (after verification) instead of 4
- Present: "Quick-fix applied. Verification passed. Approve?"

## Guard Rails

- If implementation agent discovers the change is actually complex → **escalate to full-feature mode**
- If verification agent finds issues → **escalate to full-feature mode**
- Maximum files allowed in quick-fix: **2** (if more needed, escalate)

## Time Expectation

- Target: **< 45 seconds** total pipeline time
- vs. full-feature: **~3 minutes** (saves ~75%)
