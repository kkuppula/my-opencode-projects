---
description: "Hotfix mode for critical production bugs. Uses lite discovery, skips new test writing, prioritizes speed."
mode: agent
model: github-copilot/claude-opus-4.6
temperature: 0.1
---

# Hotfix Pipeline Mode

For urgent production bugs that need immediate resolution.

## When to Use

- Critical bug in production
- Users are blocked or data is at risk
- Urgency overrides thoroughness
- Root cause is identified or suspected

## Pipeline Stages

```
[Discovery (Lite)] → [Implementation] → [Verification]
   (faster discovery — focused on root cause only)
   (skip new test writing — run existing tests to prevent regression)
```

## Lite Discovery Behavior

When invoking the discovery agent in hotfix mode, modify the prompt:

```
MODE: HOTFIX — Lite Discovery

Focus ONLY on:
1. The suspected root cause
2. The minimal fix location (1-3 files max)
3. Existing test coverage for the affected area
4. Regression risk of the fix

Do NOT analyze:
- Full architecture
- Long-term patterns
- Refactoring opportunities
- Comprehensive edge cases

Time target: < 20 seconds
```

## Implementation Behavior

```
MODE: HOTFIX — Minimal Fix

Rules:
- Fix the bug with the MINIMAL change possible
- Do NOT clean up surrounding code
- Do NOT add abstractions "while you're in there"
- Do NOT change behavior beyond fixing the bug
- Prefer the safest fix, not the cleanest fix
- If unsure between two approaches, pick the more conservative one
```

## Verification Behavior

```
MODE: HOTFIX — Focused Verification

Focus on:
1. Does the fix address the root cause?
2. Does it compile?
3. Do existing tests pass?
4. Is there any regression risk?
5. Is the fix safe to deploy immediately?

Skip:
- Comprehensive style review
- Performance optimization review
- Long-term maintainability assessment
```

## Gate Behavior

- **2 gates** (after lite discovery, after verification)
- Gate 1: "Root cause identified. Fix approach: [X]. Proceed?"
- Gate 2: "Fix applied and verified. Safe to deploy? Approve?"

## Guard Rails

- If the fix requires > 3 files → **escalate to full-feature**
- If root cause is unclear → **escalate to full-feature**
- If the fix changes public API → **require explicit approval**
- Always run existing tests — never skip test execution

## Time Expectation

- Target: **< 90 seconds** total pipeline time
- vs. full-feature: **~3 minutes** (saves ~50%)

## Post-Hotfix Recommendation

After the hotfix is deployed, recommend:
```
⚠️ Hotfix applied. Recommended follow-up:
1. Add test coverage for this scenario (currently missing)
2. Consider a more thorough fix in the next sprint
3. Update the incident report with root cause
```
