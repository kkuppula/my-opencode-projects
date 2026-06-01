---
name: ado-query
description: "Natural language query interface for Azure DevOps. Ask questions about bugs, regressions, velocity, releases, team performance, and work items without writing WIQL. Use when: ADO query, regressions, root causes, velocity, team metrics, release bugs, sprint analysis, bug trends, what broke, how many bugs, find tickets, ADO search, work item analysis, regression report, team Madras velocity, release quality."
argument-hint: "Ask a question, e.g. 'What regressions were in the last release?' or 'Velocity of team Madras last 5 sprints'"
---

# ADO Natural Language Query

Translate natural language questions about Azure DevOps work items into WIQL queries, execute them, and present well-formatted analytical reports.

## Prerequisites

- `ADO_PAT` environment variable must be set (Work Items Read scope).
- If not set, run `source ~/.zprofile` and recheck. If still missing, ask the user.

## Core Workflow

1. **Understand the question** — Identify what the user is asking (regressions? velocity? bugs? trends?).
2. **Build a WIQL query** — Translate to WIQL using the reference below.
3. **Execute** — Run the query via the script.
4. **Format** — Pipe results through the formatter with the appropriate report type.
5. **Analyze** — Add your own analytical commentary: patterns, outliers, recommendations.

## Step 1: Verify PAT

```bash
[[ -z "${ADO_PAT:-}" ]] && source ~/.zprofile
echo "PAT set: ${ADO_PAT:+yes}"
```

If still missing, ask the user to set it.

## Step 2: Build and Execute WIQL

The core script accepts a WIQL query and returns JSON:

```bash
~/.copilot/skills/ado-query/scripts/query-workitems.sh "<WIQL>"
```

### WIQL Quick Reference

**Basic structure:**
```sql
SELECT [System.Id], [System.Title], [System.State]
FROM workitems
WHERE <conditions>
ORDER BY <field> ASC|DESC
```

**Key fields available:**
| Field | WIQL Name | Example Values |
|-------|-----------|---------------|
| ID | `System.Id` | 580890 |
| Title | `System.Title` | text search with `CONTAINS` |
| State | `System.State` | New, Ready to Start, In Progress, In Review, In Testing, Closed, Cancelled, Removed |
| Type | `System.WorkItemType` | Bug, Task, User Story, Feature, Epic, Test Case |
| Priority | `Microsoft.VSTS.Common.Priority` | 1, 2, 3, 4 |
| Severity | `Microsoft.VSTS.Common.Severity` | 1 - Critical, 2 - High, 3 - Medium, 4 - Low |
| Assigned To | `System.AssignedTo` | display name or `@Me` |
| Iteration | `System.IterationPath` | `Learn\Sprint 42` — use `UNDER` for hierarchy |
| Area Path | `System.AreaPath` | `Learn\Madras` — use `UNDER` for hierarchy |
| Tags | `System.Tags` | use `CONTAINS` operator |
| Story Points | `Microsoft.VSTS.Scheduling.StoryPoints` | numeric |
| Created Date | `System.CreatedDate` | use `>=`, `<=`, or `@Today - 30` |
| Closed Date | `Microsoft.VSTS.Common.ClosedDate` | |
| Changed Date | `System.ChangedDate` | |
| Resolved Reason | `Microsoft.VSTS.Common.ResolvedReason` | Fixed, By Design, Duplicate, etc. |
| Reason | `System.Reason` | state-change reason |
| Value Area | `Microsoft.VSTS.Common.ValueArea` | Business, Architectural |

**WIQL operators:** `=`, `<>`, `>`, `<`, `>=`, `<=`, `CONTAINS`, `NOT CONTAINS`, `IN`, `NOT IN`, `UNDER`, `NOT UNDER`, `IN GROUP`, `EVER`, `WAS EVER`

**Date macros:** `@Today`, `@Today - 7` (days ago), `@StartOfDay`, `@StartOfMonth`

**Identity macros:** `@Me`, `@TeamAreas`

### Common Query Patterns

**Regressions in a release/iteration:**
```sql
SELECT [System.Id], [System.Title], [System.State], [Microsoft.VSTS.Common.Severity]
FROM workitems
WHERE [System.WorkItemType] = 'Bug'
  AND [System.Tags] CONTAINS 'Regression'
  AND [System.IterationPath] UNDER 'Learn\Release 42'
ORDER BY [Microsoft.VSTS.Common.Severity] ASC
```

Alternative if regressions are not tagged — look for bugs created AFTER a release started:
```sql
SELECT [System.Id], [System.Title], [System.State]
FROM workitems
WHERE [System.WorkItemType] = 'Bug'
  AND [System.Tags] CONTAINS 'Regression'
  AND [System.CreatedDate] >= @Today - 90
ORDER BY [System.CreatedDate] DESC
```

**Bugs in an area/team:**
```sql
SELECT [System.Id], [System.Title], [System.State], [Microsoft.VSTS.Common.Severity]
FROM workitems
WHERE [System.WorkItemType] = 'Bug'
  AND [System.AreaPath] UNDER 'Learn\Madras'
  AND [System.State] <> 'Closed'
ORDER BY [Microsoft.VSTS.Common.Priority] ASC
```

**Velocity (items with story points in recent iterations):**
```sql
SELECT [System.Id], [System.Title], [System.State], [Microsoft.VSTS.Scheduling.StoryPoints], [System.IterationPath]
FROM workitems
WHERE [System.AreaPath] UNDER 'Learn\Madras'
  AND [Microsoft.VSTS.Scheduling.StoryPoints] > 0
  AND [System.IterationPath] UNDER 'Learn'
  AND [System.ChangedDate] >= @Today - 90
ORDER BY [System.IterationPath] ASC
```

**Items closed/completed in a time window:**
```sql
SELECT [System.Id], [System.Title], [System.State], [Microsoft.VSTS.Scheduling.StoryPoints]
FROM workitems
WHERE [System.State] IN ('Closed', 'Done', 'Resolved')
  AND [Microsoft.VSTS.Common.ClosedDate] >= @Today - 30
  AND [System.AreaPath] UNDER 'Learn\Madras'
ORDER BY [Microsoft.VSTS.Common.ClosedDate] DESC
```

**Find items by keyword:**
```sql
SELECT [System.Id], [System.Title], [System.State]
FROM workitems
WHERE [System.Title] CONTAINS 'authentication'
  AND [System.State] <> 'Removed'
ORDER BY [System.ChangedDate] DESC
```

**High-priority open bugs:**
```sql
SELECT [System.Id], [System.Title], [System.State], [Microsoft.VSTS.Common.Severity]
FROM workitems
WHERE [System.WorkItemType] = 'Bug'
  AND [System.State] NOT IN ('Closed', 'Removed', 'Done')
  AND [Microsoft.VSTS.Common.Priority] <= 2
ORDER BY [Microsoft.VSTS.Common.Priority] ASC, [Microsoft.VSTS.Common.Severity] ASC
```

## Step 3: Format Results

Pipe query output through the formatter:

```bash
~/.copilot/skills/ado-query/scripts/query-workitems.sh "<WIQL>" | python3 ~/.copilot/skills/ado-query/scripts/format-results.py --format <format> [options]
```

**Available formats:**
| Format | Use For | Options |
|--------|---------|---------|
| `table` | General item listing | `--sort-by`, `--title` |
| `summary` | Aggregated breakdowns | `--group-by <field>`, `--title` |
| `velocity` | Sprint velocity analysis | `--title` |
| `regressions` | Regression root cause analysis | `--title` |
| `bugs` | Bug analysis by severity/area | `--title` |
| `timeline` | Items over time by creation date | `--title` |

**Group-by options** for `--group-by`: `System.AreaPath`, `System.AssignedTo`, `System.IterationPath`, `System.State`, `System.WorkItemType`, `System.Tags`

## Step 4: Fetch Iteration/Sprint Data (for velocity questions)

```bash
~/.copilot/skills/ado-query/scripts/fetch-iterations.sh [--team <team-name>] [--timeframe past|current|future]
```

This returns sprint dates and names. Combine with the velocity query to provide richer context.

## Step 5: Analyze and Comment

After presenting the formatted report, **add your own analysis**:

- **Patterns**: "Most regressions are concentrated in the Auth area (5 of 12)."
- **Trends**: "Velocity has been declining over the last 3 sprints — from 45 to 32 points."
- **Root causes**: "7 of 10 regressions have the same root cause: incomplete test coverage for the new API."
- **Recommendations**: "Consider adding integration tests for the Auth module before the next release."
- **Outliers**: "Sprint 42 had unusually high velocity (60 points) — investigate if that's sustainable."

## Question -> Query Translation Guide

Use this to map natural language to WIQL:

| User Question Pattern | WIQL Strategy | Format |
|----------------------|---------------|--------|
| "What regressions in release X?" | Filter Bug + Tag 'Regression' + IterationPath UNDER X | `regressions` |
| "Root causes of regressions" | Same as above — reasons and tags are in the data | `regressions` |
| "Velocity of team X" | AreaPath UNDER team + StoryPoints > 0 + recent iterations | `velocity` |
| "Bugs in area/team X" | WorkItemType=Bug + AreaPath UNDER X | `bugs` |
| "What broke in the last sprint?" | Bug + created in current/last iteration | `table` or `bugs` |
| "How many items did team X close?" | State=Closed + AreaPath UNDER team + date range | `summary` |
| "Show me P1 bugs" | Bug + Priority=1 | `table` |
| "What's assigned to [person]?" | AssignedTo = name | `table` |
| "Trending bugs last 3 months" | Bug + CreatedDate >= @Today - 90 | `timeline` |
| "Compare teams A vs B velocity" | Run velocity query twice with different area paths | `velocity` (x2) |
| "Unresolved blockers" | Bug/Task + Priority=1 + State not Closed | `table` |
| "Release readiness" | Open items in release iteration + summary | `summary` |

## Handling Ambiguity

If the user's question is ambiguous:
1. **Guess the most likely interpretation** and run the query.
2. **State your assumptions** clearly: "I'm interpreting 'last release' as iteration `Learn\Release 42`. If you mean a different release, let me know."
3. **Offer refinements**: "I can also filter by severity, area, or assigned team. Want me to narrow this down?"

If you don't know the exact iteration or area path name, fetch available iterations first:
```bash
~/.copilot/skills/ado-query/scripts/fetch-iterations.sh --timeframe past --top 10
```

## Example Invocations

| User says | Action |
|-----------|--------|
| "What regressions were in the last release?" | Query bugs tagged Regression in recent iterations -> `regressions` format |
| "What were the root causes?" | Same regression query -> `regressions` format (shows root cause grouping) |
| "Give me velocity of team Madras" | Query story points under Madras area path -> `velocity` format |
| "How many bugs did we close last month?" | Closed bugs in last 30 days -> `summary` format |
| "Show me all P1/P2 bugs" | Priority <= 2 bugs -> `table` format |
| "What's the bug trend for Auth team?" | Bugs under Auth area -> `timeline` format |
| "Compare velocity: Madras vs Dublin" | Two velocity queries -> present side by side |
| "Who has the most open bugs?" | Open bugs grouped by assignee -> `summary` with `--group-by System.AssignedTo` |

## Troubleshooting

- **401 Unauthorized**: PAT expired. Ask user to regenerate.
- **Empty results**: Loosen filters — try removing date constraints or broadening the area path. Also verify the area/iteration path names are correct.
- **WIQL syntax error**: The error message from ADO will indicate the issue. Fix the WIQL and retry.
- **No story points**: Velocity requires `StoryPoints` or `Effort` fields to be populated. If a team doesn't use points, fall back to item count.
