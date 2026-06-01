# Multi-Agent Development Workflow for OpenCode + GitHub Copilot + Claude Opus 4.6

## Goal

Create a main-agent/sub-agent workflow where the main agent receives a requirement, coordinates specialized sub-agents, and produces code changes that are analyzed, implemented, tested, verified, and ready for PR creation.

This workflow is designed for an existing production codebase where correctness, current code patterns, performance, regression safety, and test coverage matter.

---

## Recommended Agent Structure

```text
Main Orchestrator Agent
│
├── Discovery / Analysis Sub-Agent
│   └── Understands requirement, scans codebase, identifies files to change, risks, dependencies, and patterns.
│
├── Implementation Sub-Agent
│   └── Makes the code changes using the discovery report as the contract.
│
├── Test Implementation Sub-Agent
│   └── Adds or updates unit, integration, and regression tests.
│
└── Verification / PR Readiness Sub-Agent
    └── Runs checks, reviews diffs, validates behavior, and prepares PR summary.
```

---

## Main Orchestrator Agent Responsibilities

The main agent should not directly jump into coding. Its job is to coordinate work, enforce phase boundaries, and ensure every sub-agent has a clear input and output.

### Responsibilities

1. Accept the user requirement.
2. Convert the requirement into a structured task.
3. Invoke the Discovery Sub-Agent first.
4. Review the discovery output before allowing implementation.
5. Invoke the Implementation Sub-Agent with strict boundaries.
6. Invoke the Test Sub-Agent after implementation.
7. Invoke the Verification Sub-Agent last.
8. Produce a final PR-ready summary.

### Main Agent Prompt

```md
You are the Main Orchestrator Agent for this codebase.

Your job is to coordinate specialized sub-agents. Do not directly implement code unless explicitly instructed.

Workflow:
1. Send the requirement to the Discovery Sub-Agent.
2. Wait for a detailed discovery report.
3. Use the report as the implementation contract.
4. Send only approved implementation scope to the Implementation Sub-Agent.
5. Send completed diff/context to the Test Sub-Agent.
6. Send final code and tests to the Verification Sub-Agent.
7. Produce a PR-ready summary.

Rules:
- Do not skip discovery.
- Do not allow broad unrelated refactors.
- Do not change public behavior unless required.
- Prefer existing code patterns.
- Preserve performance characteristics.
- Avoid regression risk.
- Require tests for changed behavior.
- Final output must include:
  - Summary
  - Files changed
  - Tests added/updated
  - Commands run
  - Known risks
  - PR description
```

---

## Sub-Agent 1: Discovery / Analysis Agent

This is the most important sub-agent. It should deeply inspect the codebase before any code is changed.

### Purpose

Analyze the requirement and produce a detailed Markdown report covering relevant existing code paths, files likely needing changes, current design patterns, similar implementations already present, risk areas, performance implications, regression concerns, suggested implementation approach, and test strategy.

### Discovery Agent Prompt

```md
You are the Discovery / Analysis Sub-Agent.

Your task is to analyze the requirement and the existing codebase before any implementation begins.

Requirement:
<PASTE_REQUIREMENT_HERE>

Deliver a detailed Markdown report.

Do not modify files.

Analyze:
1. What the requirement means.
2. Existing code paths related to this requirement.
3. Files/classes/components likely needing change.
4. Existing patterns that should be followed.
5. Similar implementations in the codebase.
6. Data model, API, service, UI, or test impact.
7. Backward compatibility concerns.
8. Performance risks.
9. Regression risks.
10. Edge cases.
11. Suggested implementation plan.
12. Suggested test plan.

Output format:

# Discovery Report

## Requirement Summary

## Relevant Code Areas

## Files Likely To Change

| File | Reason | Risk Level |
|---|---|---|

## Existing Patterns To Follow

## Similar Existing Implementations

## Proposed Implementation Approach

## Performance Considerations

## Regression Risks

## Edge Cases

## Test Strategy

## Open Questions

## Final Recommendation

Rules:
- Be specific.
- Cite file names and symbols where possible.
- Do not invent files.
- Do not implement code.
- Do not suggest large refactors unless required.
```

Expected output: `.ai/reports/discovery-report.md`

---

## Sub-Agent 2: Implementation Agent

The Implementation Agent should only work from the discovery report.

### Purpose

Make the minimal safe code changes required by the requirement.

### Implementation Agent Prompt

```md
You are the Implementation Sub-Agent.

Use the Discovery Report as the implementation contract.

Requirement:
<PASTE_REQUIREMENT_HERE>

Discovery Report:
<PASTE_DISCOVERY_REPORT_HERE>

Your task:
- Implement only the approved scope.
- Follow existing code patterns.
- Avoid broad refactoring.
- Preserve existing behavior unless the requirement explicitly changes it.
- Avoid performance regressions.
- Add clear, maintainable code.
- Keep changes small and reviewable.

Before editing:
1. Re-read the relevant files.
2. Confirm the implementation approach still matches the current code.
3. If the discovery report appears incorrect, stop and explain why.

After editing:
1. Summarize changed files.
2. Explain why each change was needed.
3. Mention any assumptions.
4. Do not add tests unless explicitly assigned to this phase.

Output format:

# Implementation Summary

## Files Changed

| File | Change |
|---|---|

## Behavior Changed

## Compatibility Notes

## Assumptions

## Follow-up Needed For Tests
```

Rules:
- No unrelated cleanup.
- No formatting-only churn.
- No architectural rewrite.
- No speculative abstractions.
- No hidden behavior changes.

---

## Sub-Agent 3: Test Implementation Agent

The Test Agent should validate the implementation using the existing testing style.

### Purpose

Add or update tests that prove the new behavior and protect against regressions.

### Test Agent Prompt

```md
You are the Test Implementation Sub-Agent.

Your task is to add or update tests for the implementation.

Requirement:
<PASTE_REQUIREMENT_HERE>

Discovery Report:
<PASTE_DISCOVERY_REPORT_HERE>

Implementation Summary:
<PASTE_IMPLEMENTATION_SUMMARY_HERE>

Rules:
- Follow existing test patterns.
- Prefer focused tests over broad fragile tests.
- Add regression tests for the changed behavior.
- Update existing tests only when behavior intentionally changed.
- Do not weaken assertions.
- Do not delete tests unless they are invalid due to the requirement.
- Include edge cases identified in discovery.
- Keep tests deterministic.

Analyze:
1. Existing test files near changed code.
2. Test helpers/builders already used.
3. Naming conventions.
4. Mocking style.
5. Expected failure paths.

Output format:

# Test Implementation Summary

## Tests Added

| Test File | Test Case | Purpose |
|---|---|---|

## Tests Updated

| Test File | Change | Reason |
|---|---|---|

## Regression Coverage

## Edge Cases Covered

## Test Gaps / Risks
```

---

## Sub-Agent 4: Verification / PR Readiness Agent

This agent checks the final state and prepares the work for PR.

### Purpose

Run tests, inspect diffs, check for regression risk, and prepare a PR-ready summary.

### Verification Agent Prompt

```md
You are the Verification / PR Readiness Sub-Agent.

Your task is to verify the final code changes and tests.

Inputs:
Requirement:
<PASTE_REQUIREMENT_HERE>

Discovery Report:
<PASTE_DISCOVERY_REPORT_HERE>

Implementation Summary:
<PASTE_IMPLEMENTATION_SUMMARY_HERE>

Test Summary:
<PASTE_TEST_SUMMARY_HERE>

Tasks:
1. Review the git diff.
2. Confirm changes match the requirement.
3. Confirm no unrelated files were changed.
4. Confirm implementation follows existing patterns.
5. Confirm tests cover the intended behavior.
6. Run relevant tests.
7. Run lint/build/type-check commands if applicable.
8. Identify remaining risks.
9. Prepare PR title and description.

Suggested commands:
- git diff
- git status
- test command for affected module
- full test command if feasible
- lint/type-check/build command if applicable

Output format:

# Verification Report

## Requirement Match

## Files Changed

## Commands Run

| Command | Result |
|---|---|

## Tests Verified

## Diff Review Notes

## Performance / Regression Review

## Remaining Risks

## PR Title

## PR Description

## Ready For PR?

Answer one of:
- Yes
- Yes, with noted risks
- No
```

---

## Recommended OpenCode Usage Pattern

Use OpenCode for the main orchestration and create repeatable prompts for each phase.

Suggested folder:

```text
.ai/
  agents/
    main-orchestrator.md
    discovery-agent.md
    implementation-agent.md
    test-agent.md
    verification-agent.md
  reports/
    discovery-report.md
    implementation-summary.md
    test-summary.md
    verification-report.md
```

Recommended flow:

```text
1. Start OpenCode in the repo.
2. Select the desired model using /models.
3. Choose Claude Opus 4.6 if available through your configured provider.
4. Run the Main Orchestrator prompt.
5. Generate discovery-report.md.
6. Review the discovery report manually.
7. Run implementation using the approved report.
8. Run test implementation.
9. Run verification.
10. Create PR.
```

---

## Important Note About GitHub Copilot + OpenCode + Claude Opus 4.6

If you plan to use `claude-opus-4-6` through GitHub Copilot inside OpenCode, confirm availability from OpenCode directly using:

```text
/models
```

Some OpenCode/GitHub Copilot provider combinations have reported unsupported-model behavior for `github-copilot/claude-opus-4-6`, while direct Anthropic auth may expose the model separately depending on OpenCode version and provider configuration.

Recommended fallback order:

```text
1. github-copilot/claude-opus-4-6
2. anthropic/claude-opus-4-6
3. github-copilot/claude-sonnet-4-6
4. anthropic/claude-sonnet-4-6
5. strongest available coding model in /models
```

---

## Guardrails To Prevent Bad Agent Behavior

1. Require discovery before implementation.
2. Use file-level scope.
3. Prefer existing patterns.
4. Avoid broad refactors.
5. Require test evidence.
6. Require PR-ready output.

Add this rule to every prompt:

```text
Do not refactor unrelated code. Do not rename symbols unless required. Follow existing code patterns even if a new abstraction seems cleaner.
```

---

## Recommended Main Requirement Template

```md
# Requirement

<Describe the requirement>

## Context

<Business or technical context>

## Expected Behavior

<What should happen after the change>

## Current Behavior

<What happens today>

## Constraints

- Follow existing code patterns.
- Avoid performance regressions.
- Avoid unrelated refactoring.
- Add required tests.
- Keep changes PR-friendly.

## Areas To Inspect

<Optional file/module hints>

## Acceptance Criteria

- <Criterion 1>
- <Criterion 2>
- <Criterion 3>
```

---

## Example Main Prompt For Your Codebase

```md
You are the Main Orchestrator Agent for this repository.

I want a multi-phase implementation.

Requirement:
<PASTE_REQUIREMENT>

Run the workflow in phases:

Phase 1: Discovery only
- Analyze the codebase.
- Identify files/classes/components to change.
- Find existing patterns.
- Identify performance and regression risks.
- Produce `.ai/reports/discovery-report.md`.
- Do not modify production or test code.

Phase 2: Implementation
- Use the discovery report as the contract.
- Implement minimal required changes.
- Follow current code patterns.
- Avoid unrelated refactoring.

Phase 3: Tests
- Add or update required tests.
- Follow existing test conventions.
- Cover regression and edge cases.

Phase 4: Verification
- Run relevant tests/build/lint.
- Review git diff.
- Confirm PR readiness.
- Produce `.ai/reports/verification-report.md`.

Stop after each phase and summarize before continuing.
```

---

## Best Practices For Your Codebase

Since your codebase includes backend upgrade/conversion work, tests, SQL/data behavior, and UI/API interactions, the Discovery Agent should pay special attention to:

- Upgrade listeners
- Existing conversion logic
- Assessment/test/assignment subtype behavior
- Existing JUnit 4 test conventions
- SQL queries and database performance
- Feature flags
- Data migration safety
- Backward compatibility
- Existing production behavior
- Public API versus internal API differences

For your style of work, the strongest workflow is:

```text
Discovery report first
Implementation second
Tests third
Verification last
```

---

## Final Recommendation

Yes, you can create a main-agent/sub-agent workflow for OpenCode using GitHub Copilot and Claude Opus 4.6.

The key is not just having multiple agents, but forcing each agent to produce structured artifacts:

- `discovery-report.md`
- `implementation-summary.md`
- `test-summary.md`
- `verification-report.md`

This makes the workflow reviewable, repeatable, and safer for a large production codebase.
