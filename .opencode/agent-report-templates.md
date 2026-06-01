# Agent Report Templates

Structured output formats for the multi-agent workflow pipeline.
Each agent MUST produce its report in this format when completing its task.

**NEW:** All agents MUST include a Confidence Assessment and Timing section.

---

## 1. Discovery Agent Report

```markdown
# Discovery Report: [Feature Name]

## Metadata
| Field | Value |
|-------|-------|
| Pipeline Mode | full-feature / quick-fix / hotfix / refactor |
| Target Repo | Backend (Learn) / Frontend (Ultra) / Both |
| Complexity Score | [1-10] |
| Started At | [ISO timestamp] |
| Duration | [X seconds] |

## Requirement Summary
[1-3 sentences summarizing what needs to be built]

## Existing Architecture

### Relevant Code Paths
| File | Purpose | Key Lines |
|------|---------|-----------|
| `path/to/file.java` | Description | L100-150 |

### Patterns Identified
- **Pattern Name**: How it works (with file references)
- **Pattern Name**: ...

### Data Flow
[How data flows through the relevant area — brief textual or ASCII diagram]

## Implementation Contract

### Must Do
1. [Specific action with file path]
2. [Specific action with file path]

### Must NOT Do
1. [Constraint — e.g., "Do not modify public API of X"]
2. [Constraint]

### Guard Rails
- [Performance constraint]
- [Backward compatibility requirement]
- [Security consideration]

### API Contract (if full-stack)
| Field | Type (Backend) | Type (Frontend) | Default | Notes |
|-------|---------------|-----------------|---------|-------|
| fieldName | Java type | TS type | value | |

## Role/Access Model
[How the system determines permissions relevant to this feature]

## Test Strategy
- Existing test patterns: [framework, location, style]
- Recommended approach: [unit/integration/both]
- Key scenarios to cover: [list]

## Risks & Edge Cases
| Risk | Severity | Mitigation |
|------|----------|------------|
| Description | Low/Med/High | How to handle |

## Open Questions
1. [Question for product/team]
2. [Question for product/team]

## Code-RAG Searches Performed
| # | Query | Results Found | Key Finding |
|---|-------|---------------|-------------|
| 1 | "natural language query" | X results | Found [pattern] in [file] |
| 2 | "query" | X results | [finding] |

## Confidence Assessment
| Dimension | Score | Rationale |
|-----------|-------|-----------|
| Pattern clarity | [0-100]% | How clear the existing patterns are |
| Scope certainty | [0-100]% | How well-defined the scope is |
| Risk identification | [0-100]% | Confidence all risks are identified |
| **Overall confidence** | **[0-100]%** | **Weighted average** |

## Final Recommendation
[1-2 paragraphs: go/no-go, approach recommendation, key decision points]
```

---

## 2. Implementation Agent Report

```markdown
# Implementation Report: [Feature Name]

## Metadata
| Field | Value |
|-------|-------|
| Pipeline Mode | full-feature / quick-fix / hotfix / refactor |
| Target Repo | Backend (Learn) / Frontend (Ultra) |
| Started At | [ISO timestamp] |
| Duration | [X seconds] |

## Changes Made

### Files Modified
| File | Change Type | Lines (+/-) | Description |
|------|-------------|-------------|-------------|
| `path/to/file` | Modified/New/Deleted | +X/-Y | What was done |

### Architecture Decisions
| Decision | Rationale | Alternatives Considered |
|----------|-----------|------------------------|
| Chose X over Y | Because Z | Y would have required... |

## Implementation Details

### New Methods/Classes
- `ClassName.methodName()` — purpose, params, return type
- ...

### Modified Behavior
- **Before**: [description]
- **After**: [description]

### Configuration/Properties Added
| Key | Value | File |
|-----|-------|------|
| `property.key` | `value` | `path/to/file.properties` |

## Contract Compliance
| Contract Item (from Discovery) | Status | Notes |
|-------------------------------|--------|-------|
| Must Do #1 | ✅/❌ | |
| Must Do #2 | ✅/❌ | |
| Must NOT Do #1 | ✅/❌ | |

## Assumptions Made
1. [Assumption and why it was reasonable]
2. [Assumption]

## What's Left for Test Agent
- Methods to test: [list with signatures]
- Scenarios to cover: [list]
- Mocking requirements: [list]

## What's Left for Verification
- [ ] Compile check
- [ ] Template variable consistency
- [ ] Message key consistency
- [ ] Security review of any HTML/user input handling
- [ ] [Other items specific to this change]

## Confidence Assessment
| Dimension | Score | Rationale |
|-----------|-------|-----------|
| Contract compliance | [0-100]% | How well implementation matches discovery contract |
| Pattern adherence | [0-100]% | How closely existing patterns were followed |
| Completeness | [0-100]% | Whether all required changes are done |
| **Overall confidence** | **[0-100]%** | **Weighted average** |
```

---

## 3. Test Agent Report

```markdown
# Test Report: [Feature Name]

## Metadata
| Field | Value |
|-------|-------|
| Pipeline Mode | full-feature / quick-fix / hotfix / refactor |
| Target Repo | Backend (Learn) / Frontend (Ultra) |
| Started At | [ISO timestamp] |
| Duration | [X seconds] |

## Test Files Created/Modified
| File | Framework | Tests Added | Tests Modified |
|------|-----------|-------------|----------------|
| `path/to/TestFile.java` | JUnit 4/5 + Mockito | 8 | 0 |

## Test Coverage Matrix
| Scenario | Method Under Test | Status | Notes |
|----------|-------------------|--------|-------|
| Happy path - feature works | `methodName()` | ✅ Written | |
| Guard clause - no context | `methodName()` | ✅ Written | |
| Edge case - null input | `methodName()` | ⚠️ Skipped | Reason |

## Test Execution Results
- **Compiled**: ✅/❌/⚠️ (not verified)
- **Tests Run**: X passed, Y failed, Z skipped
- **Command**: `./gradlew test --tests ClassName`
- **Output**: [key lines or "all passed"]

## Mocking Strategy
| Dependency | How Mocked | Notes |
|------------|------------|-------|
| `ContextManagerFactory` | MockedStatic | Singleton pattern |

## Coverage Gaps
| Gap | Reason | Recommendation |
|-----|--------|----------------|
| Description | Why it couldn't be tested | What to do instead |

## Regression Risk Assessment
- [ ] Existing tests still pass (verified / not verified)
- [ ] New tests cover all changed code paths
- [ ] Edge cases documented even if not testable
- [ ] No flaky/time-dependent assertions

## Recommendations for Manual Testing
1. [Scenario that requires manual verification]
2. [Scenario]

## Confidence Assessment
| Dimension | Score | Rationale |
|-----------|-------|-----------|
| Coverage completeness | [0-100]% | % of scenarios covered |
| Test quality | [0-100]% | Assertion strength, edge case handling |
| Regression safety | [0-100]% | Confidence no regressions introduced |
| **Overall confidence** | **[0-100]%** | **Weighted average** |
```

---

## 4. Verification Agent Report

```markdown
# Verification Report: [Feature Name]

## Metadata
| Field | Value |
|-------|-------|
| Pipeline Mode | full-feature / quick-fix / hotfix / refactor |
| Target Repo | Backend (Learn) / Frontend (Ultra) |
| Started At | [ISO timestamp] |
| Duration | [X seconds] |

## Requirement Match
- **Requirement**: [1 sentence]
- **Implemented**: ✅ Matches / ⚠️ Partial / ❌ Does not match
- **Notes**: [if partial or mismatch, explain]

## Verification Checklist
| # | Check | Result | Notes |
|---|-------|--------|-------|
| 1 | Code compiles / no syntax errors | ✅/❌ | |
| 2 | No unused imports | ✅/❌ | |
| 3 | No unused variables or fields | ✅/❌ | |
| 4 | Imports correct and minimal | ✅/❌ | |
| 5 | No typos in string keys/constants | ✅/❌ | |
| 6 | Template/config variables consistent | ✅/❌ | |
| 7 | Message keys match code constants | ✅/❌ | |
| 8 | Role/permission logic correct | ✅/❌ | |
| 9 | Date/time logic correct | ✅/❌ | |
| 10 | Null safety handled | ✅/❌ | |
| 11 | Locale variants appropriate | ✅/❌ | |
| 12 | No XSS/injection/security issues | ✅/⚠️/❌ | |
| 13 | Existing behavior preserved | ✅/❌ | |
| 14 | Tests cover requirements | ✅/❌ | |
| 15 | No unrelated files changed | ✅/❌ | |
| 16 | Follows existing code patterns | ✅/❌ | |

## Issues Found
| # | Severity | Description | Fix Applied? | Blocking? |
|---|----------|-------------|--------------|-----------|
| 1 | Low/Med/High | Description | ✅/❌ | Yes/No |

## Commands Run
| Command | Result | Notes |
|---------|--------|-------|
| `git diff` | Reviewed | [summary] |
| `./gradlew test` | Pass/Fail/Skipped | [details] |
| `.opencode/tools/compile-check.sh` | Pass/Fail | [details] |

## Performance Assessment
- **Impact**: None / Minimal / Needs Review
- **Details**: [explanation]

## Security Assessment
- **Impact**: None / Low / Needs Review
- **Details**: [explanation]

## Files Changed (Final List)
| File | Lines Changed | Type |
|------|--------------|------|
| `path/to/file` | +X / -Y | Modified/New/Deleted |

## PR Readiness
- **Status**: ✅ Ready / ⚠️ Ready with noted risks / ❌ Blocked
- **Blocking issues**: [if any]

## PR Description (Copy-Paste Ready)

### Title
[Concise PR title]

### Body
## Summary
- Bullet point 1
- Bullet point 2
- Bullet point 3

## Changes
- [File-level descriptions]

## Testing
- [What was tested and how]

## Risks & Notes
- [Known risks and mitigations]
- [Configuration notes for reviewers]

## Confidence Assessment
| Dimension | Score | Rationale |
|-----------|-------|-----------|
| Code quality | [0-100]% | Adherence to standards and patterns |
| Test coverage | [0-100]% | Adequacy of test coverage |
| Regression safety | [0-100]% | Confidence no regressions |
| PR readiness | [0-100]% | Ready for human review |
| **Overall confidence** | **[0-100]%** | **Weighted average** |
```

---

## Assembly: Final Composite Report

When all agents complete, the orchestrator assembles:

```markdown
# Feature Delivery Report: [Feature Name]

## Overview
[1 paragraph summary]

## Pipeline Execution
| Stage | Agent | Duration | Confidence | Status | Key Finding |
|-------|-------|----------|------------|--------|-------------|
| Intake | requirement-intake | Xs | -- | ✅ | Classified as [type] |
| Discovery | discovery-agent | Xs | X% | ✅ | [1-line] |
| Implementation | implementation-agent | Xs | X% | ✅ | [1-line] |
| Testing | test-agent | Xs | X% | ✅ | [1-line] |
| Verification | verification-agent | Xs | X% | ✅ | [1-line] |

## Timing Summary
| Metric | Value |
|--------|-------|
| Total pipeline time | Xm Xs |
| Agent execution time | Xm Xs |
| Gate wait time | Xs |
| Pipeline mode | full-feature / quick-fix / hotfix / refactor |
| Mode time saved vs full | X% (if not full-feature) |

## Confidence Summary
| Stage | Confidence | Risk Areas |
|-------|------------|------------|
| Discovery | X% | [if < 85%, explain] |
| Implementation | X% | [if < 85%, explain] |
| Testing | X% | [if < 85%, explain] |
| Verification | X% | [if < 85%, explain] |
| **Weighted Overall** | **X%** | |

## Repos Affected
- [x/blank] Backend (Learn) — [X files changed, +Y/-Z lines]
- [x/blank] Frontend (Ultra) — [X files changed, +Y/-Z lines]

## Summary of Changes
[From implementation report — file table]

## Test Results
[From test report — coverage matrix summary]

## Verification Status
[From verification report — checklist pass rate: X/16 checks passed]

## Known Risks
[Aggregated from ALL reports — deduplicated]
| # | Risk | Source | Severity | Mitigation |
|---|------|--------|----------|------------|
| 1 | [risk] | [which stage found it] | Low/Med/High | [mitigation] |

## Lessons Learned
- [Pattern discovered or reinforced]
- [Pitfall encountered]
- [Improvement for next time]

## PR Description (per repo)
[From verification report — copy-paste ready]
```

---

## Common Rules for ALL Reports

1. **ALL sections are REQUIRED.** If not applicable, write "N/A — [brief reason]".
2. **Use tables** for file lists, risks, checklists, and matrices. Prefer tables over prose.
3. **Use status indicators**: ✅ (pass), ❌ (fail), ⚠️ (warning/partial).
4. **Include timestamps** in Metadata section.
5. **Include confidence scores** in the Confidence Assessment section.
6. **Cite specific files and line numbers** wherever possible.
7. **Keep summaries concise** — details go in tables, not paragraphs.
