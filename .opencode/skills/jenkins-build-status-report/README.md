# Jenkins Build Status Skill

> Get Jenkins build status, analyze failures, identify root causes, and generate executive reports.

![OpenCode Skill](https://img.shields.io/badge/OpenCode-Skill-blue)
![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-D24939)
![DORA Metrics](https://img.shields.io/badge/DORA-Metrics-green)

---

## What It Does

This OpenCode skill analyzes Jenkins pipelines:

```
/jenkins-build-status https://jenkins.example.com/job/my-pipeline/

"Why did the build fail?"
"What fixed the last failure?"
"Generate executive report for develop branch"
```

---

## Features

| Feature | Description |
|---------|-------------|
| **Build Status** | Current state, last success, last failure |
| **Root Cause Analysis** | Identifies failure patterns from console logs |
| **Fix Detection** | Shows which commits fixed the pipeline |
| **Executive Reports** | Multi-build analysis with DORA metrics |
| **Flaky Test Detection** | Identifies intermittent test failures |
| **Duration Trends** | Build time analysis with outlier detection |
| **Failure Heatmaps** | When do failures typically occur? |

---

## Installation

### 1. Copy to your OpenCode skills directory

```bash
cp -r jenkins-build-status-report ~/.opencode/skills/
```

### 2. Set Jenkins credentials

```bash
# Add to ~/.zprofile or ~/.bashrc
export JENKINS_USER="your-username"
export JENKINS_TOKEN="your-api-token"
```

### 3. Use in OpenCode

```
/jenkins-build-status https://jenkins.example.com/job/my-project/job/main/
```

---

## Output Examples

### Single Build Summary

```markdown
## Jenkins Build Summary — main

**Current Status**: ❌ FAILURE

### Last Failed Build (#142)
- **When**: 2024-01-15 10:23 UTC
- **Root Cause**: Unit/Integration test failure
- **Detail**: MyServiceTest > shouldHandleTimeout FAILED

### What Fixed It (Build #143)
- **Commits that fixed it**:
  - `a1b2c3d4` — Fix timeout handling in MyService by @developer
```

### Executive Report (Multi-Build)

- DORA metrics (MTTR, change failure rate, throughput)
- Success rate trends
- Top failure patterns
- Flaky test candidates
- Duration analysis
- Failure heatmaps by day/hour

---

## Failure Pattern Detection

The skill recognizes these patterns in console logs:

| Pattern | Indicators |
|---------|------------|
| Test failure | `Tests run:...Failures: N`, `FAILED` |
| Compilation error | `COMPILATION ERROR`, `error TS`, `cannot find symbol` |
| Dependency issue | `Could not resolve`, `Artifact not found` |
| OutOfMemory | `OutOfMemoryError`, `OOMKilled` |
| Connection issue | `Connection refused`, `timeout` |
| Disk space | `No space left on device` |
| Auth failure | `401 Unauthorized`, `403 Forbidden` |
| Gradle task failure | `Task :xyz FAILED` |

---

## Files

```
jenkins-build-status-report/
├── SKILL.md              # Skill definition
├── README.md             # This file
└── scripts/
    ├── fetch-build.sh        # Fetches build data via Jenkins API
    └── analyze-builds.py     # Multi-build executive report generator
```

---

## Configuration

```bash
export JENKINS_USER="your-username"
export JENKINS_TOKEN="your-api-token"
export BUILD_COUNT="30"              # Builds to analyze (default: 30)
export OUTPUT_PATH="~/reports/"      # Where to save reports
export GIT_REPO_PATH="/path/to/repo" # For git blame analysis
```

---

## DORA Metrics

The executive report includes DORA (DevOps Research and Assessment) metrics:

| Metric | What It Measures |
|--------|------------------|
| **MTTR** | Mean Time to Recovery (how fast failures are fixed) |
| **Change Failure Rate** | % of builds that fail |
| **Deployment Frequency** | Builds per day/week |

---

## Requirements

- Bash shell
- Python 3
- `curl`
- Jenkins API access (username + API token)

---

## License

MIT
