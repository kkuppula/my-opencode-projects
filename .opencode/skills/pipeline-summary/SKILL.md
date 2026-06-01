---
name: pipeline-summary
description: "Generates live pipeline progress visualization and status reports. Shows current stage, timing, confidence scores, and overall progress. Use when: pipeline status, show progress, what stage are we at, pipeline visualization, progress report, timing, how long did it take."
argument-hint: "e.g. 'show pipeline status' or 'generate progress report'"
---

# Pipeline Summary Skill

Generate real-time pipeline progress visualizations and status reports.

## Purpose

Provides a visual, at-a-glance view of pipeline execution — used during demos and for Teams notifications.

## Visualization Formats

### Format 1: ASCII Pipeline Diagram

```
┌─────────────────┐   ┌──────────────────────┐   ┌─────────────────┐   ┌──────────────────────┐
│  🔍 Discovery    │──▶│  🛠️ Implementation     │──▶│  🧪 Testing      │──▶│  ✅ Verification      │
│  ✅ COMPLETE     │   │  ◉◉◉◉◉◉◉○○○ 70%     │   │  ○ PENDING      │   │  ○ PENDING           │
│  ⏱️ 28s          │   │  ⏱️ 34s (running)     │   │                 │   │                      │
│  🎯 92% conf.   │   │  🎯 --% conf.        │   │  🎯 --% conf.  │   │  🎯 --% conf.       │
└─────────────────┘   └──────────────────────┘   └─────────────────┘   └──────────────────────┘

Pipeline: feature/ai-toggle │ Mode: full-feature │ Target: Backend (Learn) │ Elapsed: 1m 02s
```

### Format 2: Compact Status Line

```
🔍✅ → 🛠️⏳ → 🧪○ → ✅○  │  Mode: full-feature │ 1m 02s │ Backend (Learn)
```

### Format 3: Detailed Status Table

```markdown
## Pipeline Status: [Feature Name]

| Stage | Status | Duration | Confidence | Key Finding |
|-------|--------|----------|------------|-------------|
| 🔍 Discovery | ✅ Complete | 28s | 92% | Found 3 files, clear pattern |
| 🛠️ Implementation | ⏳ In Progress | 34s... | -- | 2/4 files modified |
| 🧪 Testing | ○ Pending | -- | -- | -- |
| ✅ Verification | ○ Pending | -- | -- | -- |

**Total elapsed:** 1m 02s
**Estimated remaining:** ~1m 30s
**Pipeline mode:** full-feature
**Target:** Backend (Learn)
**Requirement:** Add AI microservice toggle to course settings
```

### Format 4: Gate Summary (for human approval)

```markdown
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  🛑 GATE 1: Discovery Complete — Approval Required
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📊 Stage Metrics:
   ⏱️  Duration: 28 seconds
   🎯 Confidence: 92%
   📁 Files identified: 3
   ⚠️  Risks found: 1 (medium severity)
   ❓ Open questions: 0

📋 Key Findings:
   • REST endpoint pattern matches existing CourseSettingsController
   • Feature flag infrastructure already exists (FeatureFlagService)
   • Database migration needed for new column

🔮 Next Stage Preview:
   Implementation will modify 3-4 files following existing patterns.
   Estimated time: ~45 seconds.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
→ ✅ Proceed  │  🔄 Redo  │  ✏️ Correct  │  ⏹️ Stop
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Timing Tracking

The orchestrator should track:

```json
{
  "pipeline_id": "uuid",
  "feature_name": "AI Microservice Toggle",
  "mode": "full-feature",
  "target_repos": ["backend"],
  "started_at": "2026-05-31T10:00:00Z",
  "stages": {
    "discovery": {
      "status": "complete",
      "started_at": "2026-05-31T10:00:02Z",
      "completed_at": "2026-05-31T10:00:30Z",
      "duration_seconds": 28,
      "confidence": 92,
      "findings_count": 3
    },
    "implementation": {
      "status": "in_progress",
      "started_at": "2026-05-31T10:00:35Z",
      "completed_at": null,
      "duration_seconds": null,
      "confidence": null,
      "files_changed": 2
    },
    "testing": { "status": "pending" },
    "verification": { "status": "pending" }
  },
  "total_elapsed_seconds": 62
}
```

## Confidence Score Guidelines

| Score | Meaning | When to Assign |
|-------|---------|----------------|
| 95-100% | Very high confidence | Clear patterns, no ambiguity, exact matches found |
| 85-94% | High confidence | Good patterns, 1-2 minor assumptions |
| 70-84% | Moderate confidence | Some assumptions, partial pattern match |
| 50-69% | Low confidence | Significant assumptions, no clear precedent |
| <50% | Very low | Novel territory, high uncertainty |

## Integration Points

- **Orchestrator** calls this skill at each gate to generate the visualization
- **Teams notifications** use Format 2 (compact) in the card body
- **Final composite report** uses Format 3 (detailed table)
- **Gate approvals** use Format 4 (gate summary)

## Rules

- Always show elapsed time
- Always show confidence (or "--" if not yet calculated)
- Use emoji consistently for stages
- Keep compact format to a single line
- Include "estimated remaining" when possible (based on prior pipeline runs)
