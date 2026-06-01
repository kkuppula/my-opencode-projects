---
name: lessons-learned
description: "Records patterns and outcomes from completed pipelines to improve future runs. Tracks what worked, what failed, and builds institutional knowledge. Use when: save lessons, record outcome, pipeline completed, what did we learn, improve future runs, pattern library."
argument-hint: "e.g. 'record lessons from this pipeline run' or 'what patterns have we learned?'"
---

# Lessons Learned Skill

Record patterns and outcomes from pipeline runs to improve future executions.

## Purpose

Each pipeline run generates knowledge:
- Which code patterns exist for different feature types
- What assumptions commonly fail
- Which areas of the codebase are fragile
- How long different types of changes take
- Common pitfalls per repo/area

This skill captures and retrieves that knowledge.

## Storage

Knowledge is stored in: `.opencode/knowledge/`

```
.opencode/knowledge/
├── pipeline-history.json      # Structured log of all pipeline runs
├── patterns/                  # Discovered code patterns
│   ├── backend-rest-endpoint.md
│   ├── backend-feature-flag.md
│   └── frontend-component.md
├── pitfalls/                  # Common mistakes and how to avoid them
│   ├── learn-singleton-mocking.md
│   └── ultra-import-paths.md
└── metrics/                   # Timing and success metrics
    └── pipeline-metrics.json
```

## Workflow: Recording (After Pipeline Completes)

### Step 1: Extract Key Data Points

From the completed pipeline reports, capture:

```json
{
  "pipeline_id": "uuid",
  "timestamp": "2026-05-31T10:00:00Z",
  "feature_name": "AI Microservice Toggle",
  "requirement_type": "feature",
  "pipeline_mode": "full-feature",
  "target_repos": ["backend"],
  "complexity_score": 6,
  "outcome": "success",
  "timing": {
    "discovery_seconds": 28,
    "implementation_seconds": 45,
    "testing_seconds": 32,
    "verification_seconds": 22,
    "total_seconds": 127,
    "gate_wait_seconds": 15
  },
  "metrics": {
    "files_changed": 4,
    "lines_added": 87,
    "lines_removed": 3,
    "tests_added": 12,
    "confidence_scores": {
      "discovery": 92,
      "implementation": 88,
      "testing": 85,
      "verification": 95
    }
  },
  "patterns_used": [
    "REST controller endpoint pattern",
    "Feature flag check pattern",
    "DTO builder pattern"
  ],
  "issues_found": [
    {
      "stage": "verification",
      "severity": "low",
      "description": "Unused import detected",
      "fix_applied": true
    }
  ],
  "lessons": [
    "CourseSettingsController uses a specific DTO pattern — always check existing DTOs first",
    "Feature flags require both Java and Angular updates"
  ]
}
```

### Step 2: Update Pattern Library

If a new pattern was discovered, create/update a pattern file:

```markdown
# Pattern: [Pattern Name]

## Where Used
- [file paths where this pattern appears]

## Structure
[Code skeleton showing the pattern]

## When to Apply
- [Conditions that make this pattern appropriate]

## Common Mistakes
- [What people get wrong with this pattern]

## Example
[Link to a good implementation of this pattern]

## First Recorded
- Pipeline: [id]
- Feature: [name]
- Date: [date]
```

### Step 3: Update Pitfalls Registry

If an issue was found during the pipeline:

```markdown
# Pitfall: [Short Description]

## What Happens
[Description of the mistake]

## Why It Happens
[Root cause]

## How to Avoid
[Prevention strategy]

## Detection
[How to catch this early]

## Affected Area
- Repo: [backend/frontend]
- Path pattern: [e.g., "src/main/java/**/controller/**"]

## Occurrences
| Date | Pipeline | Caught At |
|------|----------|-----------|
| 2026-05-31 | AI Toggle | Verification |
```

## Workflow: Retrieval (Before Pipeline Starts)

When starting a new pipeline, query lessons learned:

### Step 1: Find Similar Past Pipelines

Match by:
- Same target repo
- Similar file paths
- Similar requirement type
- Similar keywords

### Step 2: Surface Relevant Lessons

Present to the discovery agent:
```
📚 Lessons from prior pipelines in this area:

1. [Lesson from pipeline X] — "Always check for existing DTOs before creating new ones"
2. [Pattern hint] — "This area uses the Builder pattern for DTOs (see patterns/backend-dto-builder.md)"
3. [Pitfall warning] — "⚠️ The singleton ContextManagerFactory requires MockedStatic in tests"
```

### Step 3: Update Timing Estimates

Use historical timing data to improve estimates:
```
Based on 5 prior pipelines of similar complexity:
- Discovery typically takes: 25-35 seconds
- Implementation typically takes: 40-60 seconds
- Full pipeline typically takes: 2-3 minutes
```

## Metrics Dashboard (for Demo)

```markdown
## Pipeline Metrics (Last 30 Days)

| Metric | Value |
|--------|-------|
| Total pipelines run | 23 |
| Success rate | 91% (21/23) |
| Average total time | 2m 14s |
| Fastest pipeline | 38s (quick-fix) |
| Slowest pipeline | 4m 12s (full-feature, cross-repo) |
| Most common mode | full-feature (65%) |
| Issues caught by verification | 7 |
| Patterns recorded | 12 |
| Pitfalls recorded | 5 |

### Success by Mode
| Mode | Runs | Success Rate | Avg Time |
|------|------|-------------|----------|
| full-feature | 15 | 87% | 2m 48s |
| quick-fix | 5 | 100% | 42s |
| hotfix | 2 | 100% | 1m 15s |
| refactor | 1 | 100% | 1m 55s |
```

## Rules

- ALWAYS record after a pipeline completes (success or failure)
- ALWAYS query before starting a new pipeline
- Keep pattern descriptions short and actionable
- Include file paths in lessons (not just abstract advice)
- Track timing trends — alert if pipeline is getting slower
- Failures are the most valuable lessons — capture root causes
