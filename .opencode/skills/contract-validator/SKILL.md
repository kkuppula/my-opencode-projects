---
name: contract-validator
description: "Validates that implementation matches the discovery contract. Checks Must Do/Must NOT Do items, guard rails, and cross-repo API consistency. Use when: validate contract, check compliance, verify implementation matches discovery, API contract check, cross-repo consistency."
argument-hint: "e.g. 'validate implementation against discovery contract' or 'check API contract consistency'"
---

# Contract Validator Skill

Validate that implementation matches the discovery contract.

## Purpose

At Gate 2 (after implementation), this skill systematically checks every item from the discovery contract against what was actually implemented. It catches drift between what was planned and what was built.

## Workflow

### Step 1: Load the Discovery Contract

From the Discovery Report, extract:
- All "Must Do" items
- All "Must NOT Do" items
- All "Guard Rails"
- API contract (if full-stack)
- Field names, types, defaults

### Step 2: Load the Implementation Report

From the Implementation Report, extract:
- Files changed
- Methods/classes added
- Contract compliance self-assessment
- Assumptions made

### Step 3: Validate Each Contract Item

For each "Must Do":
- Is it implemented? (check files changed)
- Is it complete? (not just started)
- Does it match the specification?

For each "Must NOT Do":
- Was it violated? (check diff for forbidden changes)
- Were there accidental side effects?

For each "Guard Rail":
- Was it respected?
- Any borderline cases?

### Step 4: Cross-Repo Validation (Full-Stack)

If both backend and frontend are involved:

| Check | What to Validate |
|-------|-----------------|
| Field names | Backend DTO field names === Frontend interface field names |
| Types | Java types map correctly to TypeScript types |
| Defaults | Default values are consistent |
| Enums | Enum values match exactly (case-sensitive) |
| Nullability | Optional in TS ↔ @Nullable in Java |
| Endpoint paths | Frontend service URL matches backend @RequestMapping |
| HTTP methods | Frontend service uses correct GET/POST/PUT/DELETE |

## Output Format

```markdown
# Contract Validation Report

## Overall Status: ✅ PASS / ⚠️ PARTIAL / ❌ FAIL

## Must Do Compliance
| # | Contract Item | Status | Evidence |
|---|---------------|--------|----------|
| 1 | [from discovery] | ✅ Implemented | Found in [file:line] |
| 2 | [from discovery] | ❌ Missing | Not found in any changed file |
| 3 | [from discovery] | ⚠️ Partial | Implemented but [concern] |

## Must NOT Do Compliance
| # | Constraint | Status | Evidence |
|---|-----------|--------|----------|
| 1 | [from discovery] | ✅ Respected | No violations in diff |
| 2 | [from discovery] | ❌ Violated | [file:line] shows [violation] |

## Guard Rails
| # | Guard Rail | Status | Notes |
|---|-----------|--------|-------|
| 1 | [constraint] | ✅ OK | |
| 2 | [constraint] | ⚠️ Borderline | [explanation] |

## API Contract Consistency (if full-stack)
| Field | Backend (DTO) | Frontend (Interface) | Match? |
|-------|---------------|---------------------|--------|
| name | `String name` | `name: string` | ✅ |
| enabled | `Boolean enabled` | `enabled: boolean` | ✅ |
| count | `Integer count` | `count: string` | ❌ TYPE MISMATCH |

## Assumptions Validation
| # | Assumption Made | Valid? | Notes |
|---|----------------|--------|-------|
| 1 | [from implementation report] | ✅/❌ | |

## Verdict
- **Contract items fulfilled:** X/Y (Z%)
- **Violations found:** N
- **Blocking issues:** [list or "None"]
- **Recommendation:** Proceed / Fix required / Re-do discovery
```

## Rules

- Every contract item MUST be checked — no skipping
- Evidence MUST cite file and line number
- Type mismatches between repos are ALWAYS blocking
- Field name mismatches are ALWAYS blocking
- Missing "Must Do" items are blocking
- "Must NOT Do" violations are blocking
- Guard rail borderline cases are warnings (non-blocking)
