---
name: requirement-intake
description: "Transforms vague user statements into structured, actionable requirements. Handles ambiguity, asks clarifying questions, classifies requirement type, and produces a pipeline-ready requirement document. Use when: new feature request, vague requirement, unclear scope, user says 'I want...', 'we need...', 'can you add...', 'build me...', requirement analysis, scope definition."
argument-hint: "Describe what you want built, e.g. 'I want a toggle for AI features in course settings' or 'add dark mode to the dashboard'"
---

# Requirement Intake Skill

Transform vague user input into structured, pipeline-ready requirements.

## Purpose

This skill sits **before** the pipeline starts. It takes ambiguous user statements and produces:
1. A structured requirement document
2. Classification (type, complexity, target repo)
3. Acceptance criteria
4. Recommended pipeline mode

## Workflow

### Step 1: Parse the Raw Input

Extract from the user's statement:
- **What** they want (feature/fix/refactor)
- **Where** it should live (backend/frontend/both)
- **Why** they want it (business context)
- **Who** it's for (user role/persona)

### Step 2: Classify the Requirement

Determine:

| Dimension | Options |
|-----------|---------|
| **Type** | Feature / Bug Fix / Refactor / Configuration / UI Change |
| **Complexity** | Trivial (1 file) / Simple (2-3 files) / Medium (4-8 files) / Complex (9+ files) |
| **Target** | Backend / Frontend / Full-Stack |
| **Pipeline Mode** | quick-fix / hotfix / refactor / full-feature |
| **Risk Level** | Low / Medium / High |

### Step 3: Identify Ambiguities

Look for:
- Missing acceptance criteria
- Unclear scope boundaries
- Undefined user roles/permissions
- Missing error handling requirements
- Unclear data model implications
- Performance requirements not stated
- Backward compatibility assumptions

### Step 4: Ask Clarifying Questions (if needed)

Present questions in priority order:
1. **Blocking questions** — can't proceed without answers
2. **Important questions** — affect architecture decisions
3. **Nice-to-have questions** — improve requirement quality

Format:
```
I need clarification on a few things before we proceed:

🔴 Must answer:
   1. [Question about scope/behavior]

🟡 Would help:
   2. [Question about edge cases]
   3. [Question about permissions]

🟢 Optional (I'll assume defaults if not answered):
   4. [Question about UI details]
```

### Step 5: Produce Structured Requirement

Output this format:

```markdown
# Structured Requirement: [Feature Name]

## Summary
[1-2 sentences — clear, unambiguous statement of what needs to be built]

## Classification
| Dimension | Value | Rationale |
|-----------|-------|-----------|
| Type | Feature/Fix/Refactor | Why |
| Complexity | Simple/Medium/Complex | Based on estimated file count |
| Target Repo | Backend/Frontend/Both | Why |
| Pipeline Mode | full-feature/quick-fix/hotfix/refactor | Why |
| Risk Level | Low/Medium/High | Why |

## User Story
**As a** [role]
**I want to** [action]
**So that** [benefit]

## Acceptance Criteria
1. **GIVEN** [context] **WHEN** [action] **THEN** [expected result]
2. **GIVEN** [context] **WHEN** [action] **THEN** [expected result]
3. ...

## Scope Boundaries

### In Scope
- [Specific thing to build/change]
- [Specific thing to build/change]

### Out of Scope
- [Explicitly excluded]
- [Explicitly excluded]

## Technical Hints
- Likely patterns: [REST endpoint / Feature flag / UI component / DB migration]
- Similar existing features: [reference if known]
- Key considerations: [performance / security / backward compat]

## Assumptions Made
1. [Assumption — validated or needs confirmation]
2. [Assumption]

## Pipeline Recommendation
- **Mode**: [full-feature / quick-fix / hotfix / refactor]
- **Estimated stages needed**: [Discovery + Implementation + Testing + Verification]
- **Skip opportunities**: [e.g., "Skip discovery if pattern is well-known"]
- **Parallel opportunity**: [e.g., "Backend and frontend can run in parallel — API contract is clear"]

## Original User Input
> [Exact user statement preserved for traceability]
```

## Decision Logic for Pipeline Mode

```
IF single-file change AND no behavior change → quick-fix
IF bug fix in production AND urgent → hotfix
IF renaming/restructuring AND no behavior change → refactor
IF new feature OR multi-file change OR behavior change → full-feature
```

## Decision Logic for Target Repo

Read `.opencode/repos.yaml` and match keywords:

```
IF mentions: API, service, controller, entity, DTO, database, endpoint → Backend
IF mentions: UI, component, page, view, styling, Angular, template → Frontend
IF mentions both OR "full-stack" OR needs API + UI → Both
IF unclear → ASK the user
```

## Rules

- NEVER guess critical business logic — ask.
- ALWAYS state assumptions explicitly.
- ALWAYS recommend a pipeline mode.
- Prefer asking 2-3 focused questions over making 5+ assumptions.
- If the requirement is already clear and structured, just classify and pass through (don't over-process).
- The output MUST be ready to feed directly into the orchestrator pipeline.
