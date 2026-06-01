# ADO Natural Language Query Skill

> Ask questions about Azure DevOps work items in plain English. No WIQL required.

![OpenCode Skill](https://img.shields.io/badge/OpenCode-Skill-blue)
![Azure DevOps](https://img.shields.io/badge/Azure%20DevOps-API-0078D7)
![Natural Language](https://img.shields.io/badge/NL-to--WIQL-purple)

---

## What It Does

This OpenCode skill lets you query Azure DevOps using **natural language**:

```
"What regressions were in the last release?"
"Show me velocity for team Alpha last 5 sprints"
"How many P1 bugs are still open?"
"Who has the most open bugs assigned?"
```

The skill translates your question to WIQL, executes it against ADO, and returns a formatted report with analysis.

---

## Features

| Feature | Description |
|---------|-------------|
| **Natural Language Input** | Ask questions without knowing WIQL syntax |
| **Multiple Report Formats** | Tables, summaries, velocity charts, regression analysis |
| **Automatic Analysis** | Patterns, trends, outliers, recommendations |
| **Sprint/Iteration Support** | Fetches iteration data for velocity queries |
| **Flexible Filtering** | By area path, iteration, assignee, state, priority, tags |

---

## Installation

### 1. Copy to your OpenCode skills directory

```bash
cp -r ado-query ~/.opencode/skills/
```

### 2. Set your Azure DevOps PAT

```bash
# Add to ~/.zprofile or ~/.bashrc
export ADO_PAT="your-personal-access-token"
export ADO_ORG="your-organization"
export ADO_PROJECT="your-project"
```

The PAT needs **Work Items (Read)** scope.

### 3. Use in OpenCode

```
/ado-query What regressions were in the last release?
```

---

## Example Queries

| Question | What It Does |
|----------|--------------|
| "What regressions in release 42?" | Bugs tagged 'Regression' in that iteration |
| "Velocity of team Alpha" | Story points completed per sprint |
| "P1 bugs still open" | Priority 1 bugs not closed |
| "Bugs by assignee" | Summary grouped by assigned person |
| "What broke last sprint?" | Bugs created in the last iteration |
| "Trending bugs last 3 months" | Timeline of bug creation |

---

## Report Formats

| Format | Use Case |
|--------|----------|
| `table` | General work item listing |
| `summary` | Aggregated counts by field (state, assignee, area) |
| `velocity` | Sprint velocity analysis with trends |
| `regressions` | Regression root cause analysis |
| `bugs` | Bug analysis by severity and area |
| `timeline` | Items over time by creation date |

---

## Files

```
ado-query/
├── SKILL.md              # Skill definition (loaded by OpenCode)
├── README.md             # This file
└── scripts/
    ├── query-workitems.sh    # Executes WIQL queries
    ├── fetch-iterations.sh   # Gets sprint/iteration data
    └── format-results.py     # Formats output into reports
```

---

## Configuration

Edit the scripts or use environment variables:

```bash
export ADO_ORG="your-organization"      # Azure DevOps organization
export ADO_PROJECT="your-project"       # Project name
export ADO_PAT="your-pat-token"         # Personal Access Token
```

---

## WIQL Quick Reference

The skill translates natural language to WIQL. Key fields:

| Field | WIQL Name |
|-------|-----------|
| ID | `System.Id` |
| Title | `System.Title` |
| State | `System.State` |
| Type | `System.WorkItemType` |
| Priority | `Microsoft.VSTS.Common.Priority` |
| Severity | `Microsoft.VSTS.Common.Severity` |
| Assigned To | `System.AssignedTo` |
| Iteration | `System.IterationPath` |
| Area Path | `System.AreaPath` |
| Tags | `System.Tags` |
| Story Points | `Microsoft.VSTS.Scheduling.StoryPoints` |

---

## Requirements

- Bash shell
- Python 3
- `curl`
- Azure DevOps PAT with Work Items Read scope

---

## License

MIT
