---
name: pr-description
description: "Generates a polished, copy-paste ready PR description from a git diff and implementation context. Can be used standalone or as part of the pipeline. Use when: generate PR description, write PR body, create pull request text, PR summary, changelog entry."
argument-hint: "e.g. 'generate PR for the current changes' or 'PR description for AI toggle feature'"
---

# PR Description Generator Skill

Generate polished, copy-paste ready PR descriptions.

## Purpose

Creates well-structured PR descriptions that are ready to paste into GitHub/Azure DevOps. Can be used:
1. As part of the pipeline (called by Verification Agent)
2. Standalone (user just wants a PR description for their current changes)

## Workflow

### Step 1: Gather Context

From available sources (in priority order):
1. **Verification Report** (if available) — most complete
2. **Implementation Report** (if available) — changes details
3. **Git diff** (always available) — raw changes
4. **Commit messages** — developer intent

### Step 2: Analyze the Diff

Categorize changes:
- **Feature code** — new functionality
- **Bug fix** — corrective changes
- **Tests** — test additions/modifications
- **Configuration** — properties, configs
- **Documentation** — comments, docs

### Step 3: Generate PR Description

## Output Format

```markdown
## [PR Title — imperative mood, concise]

### Summary
[2-3 bullet points explaining WHAT and WHY]

### Changes
| File | Change | Purpose |
|------|--------|---------|
| `path/to/file` | Added/Modified/Removed | Brief description |

### Type of Change
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to change)
- [ ] Refactoring (no functional changes)
- [ ] Configuration change
- [ ] Documentation update

### How Has This Been Tested?
- [x] Unit tests added/updated
- [x] Existing test suite passes
- [ ] Manual testing performed
- [ ] Integration tests

### Test Coverage
| Scenario | Status |
|----------|--------|
| [Happy path] | ✅ Covered |
| [Edge case] | ✅ Covered |
| [Error handling] | ✅ Covered |

### Screenshots / Evidence
[If applicable — for UI changes]

### Risks & Considerations
- [Any known risks]
- [Backward compatibility notes]
- [Performance implications]

### Checklist
- [x] My code follows the existing code style
- [x] I have added tests that prove my fix/feature works
- [x] New and existing unit tests pass locally
- [ ] I have updated documentation if needed
- [x] No new warnings introduced

### Related Items
- [ADO Work Item / Issue link if known]
- [Related PRs if any]
```

## Title Conventions

| Change Type | Title Format | Example |
|-------------|--------------|---------|
| Feature | `Add [feature] to [area]` | `Add AI microservice toggle to course settings` |
| Bug fix | `Fix [issue] in [area]` | `Fix null pointer in grade calculation` |
| Refactor | `Refactor [what] for [why]` | `Refactor UserService for better testability` |
| Config | `Update [config] for [purpose]` | `Update feature flags for release 42` |

## Rules

- Use imperative mood ("Add", not "Added" or "Adds")
- Keep summary to 2-3 bullets max
- Always include the file change table
- Always include test coverage section
- Always include risks section (even if "None identified")
- If generated from a diff alone (no reports), note lower confidence
- Match the team's existing PR style if examples are available
