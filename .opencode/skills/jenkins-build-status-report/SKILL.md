---
name: jenkins-build-status
description: "Get Jenkins build status, summarize failures, identify root causes, and explain what fixed the build. Use when: checking Jenkins build, why did build fail, what broke the pipeline, build summary, CI failure cause, Jenkins job status."
argument-hint: "<jenkins-job-url>"
---

# Jenkins Build Status & Failure Analysis

## When to Use
- User pastes a Jenkins job URL and asks about build status
- User asks why a build failed, what caused it, or what fixed it
- User wants a summary of recent build history

## Procedure

### 1. Parse the Jenkins URL
Extract the base URL and job path from the provided URL. Jenkins job URLs follow the pattern:
`https://<host>/job/<folder>/job/<subfolder>/job/<branch>/`

### 2. Fetch Build Data via Jenkins REST API
Run [./scripts/fetch-build.sh](./scripts/fetch-build.sh) with the job URL to retrieve:
- Last build result (SUCCESS / FAILURE / UNSTABLE / ABORTED)
- Last successful build number
- Last failed build number
- Build change sets (commits that triggered each build)
- Console log excerpt of the failed build

The script uses the Jenkins JSON API:
```
<job-url>/api/json?tree=lastBuild[number,result,url,changeSet[items[commitId,msg,author[fullName]]],timestamp],lastFailedBuild[number,url],lastSuccessfulBuild[number,url]
```

For the console log of a specific build:
```
<job-url>/<build-number>/consoleText
```

### 3. Analyze the Failure

Look for these common failure patterns in the console log:

| Pattern | Likely Cause |
|---------|-------------|
| `BUILD FAILURE` + `Tests run:` with failures | Unit/integration test failures |
| `ERROR` + `Cannot find module` / `ModuleNotFoundError` | Dependency/import error |
| `exit code 1` + `npm ERR!` / `yarn ERR!` | npm/yarn install or script failure |
| `COMPILATION ERROR` / `error TS` | TypeScript/Java compile error |
| `Connection refused` / `timeout` | Service/infrastructure not reachable |
| `Forbidden` / `401` / `403` | Credentials or permissions issue |
| `No space left on device` | Disk full on agent |
| `OOMKilled` / `OutOfMemoryError` | Memory limit exceeded |
| `Pipeline script` syntax error | Jenkinsfile groovy error |

### 4. Compare Failed vs Fixed Build

- Fetch commit messages between `lastFailedBuild` and `lastSuccessfulBuild`
- The commits in that range are the fix candidates
- Cross-reference commit messages with the failure cause

### 5. Output Summary

Respond with this structure:

```
## Jenkins Build Summary — <branch-name>

**Current Status**: ✅ SUCCESS / ❌ FAILURE / ⚠️ UNSTABLE

### Last Failed Build (#<N>)
- **When**: <timestamp>
- **Root Cause**: <one-line summary>
- **Detail**: <what exactly failed — test name, error message, file>

### What Fixed It (Build #<N>)
- **Commits that fixed it**:
  - `<sha>` — <commit message> by <author>
- **Why it fixed it**: <explanation connecting the commit to the failure>

### Build History (last 5)
| Build | Result | Commit |
|-------|--------|--------|
| #N    | ❌ FAIL | <msg> |
| #N+1  | ✅ PASS | <msg> |

### Recommendations
Based on the root cause, provide 2–3 actionable recommendations. Use the following mapping:

| Root Cause Pattern | Recommendations |
|--------------------|-----------------|
| `compileTestJavaWarnings` / unused imports / zero-warnings policy | 1. Add a pre-commit hook or IDE rule to flag unused imports before push. 2. Enable warnings-as-errors in local IDE settings to match CI. 3. Use `@SuppressWarnings` with justification only as a last resort. |
| Selenium / `ElementClickInterceptedException` / element click intercepted | 1. Fix z-index on overlapping UI elements. 2. Add scroll-into-view before click in test helpers. 3. Use `ExpectedConditions.elementToBeClickable()` instead of implicit waits. |
| `AssociationNotFoundException` / 404 / missing association | 1. Pre-seed required associations in CI environment bootstrap scripts. 2. Add pre-flight environment health checks to the pipeline. 3. Mock the association API for environment-sensitive tests. |
| `OutOfMemoryError` / `OOMKilled` | 1. Increase JVM heap or pod memory limits. 2. Profile memory usage with GC logging. 3. Reduce `--max-workers` to lower concurrent task memory pressure. |
| `No space left on device` | 1. Add a periodic cache/workspace cleanup step. 2. Lower artifact retention count on build agents. |
| `npm ERR!` / `yarn ERR!` | 1. Audit and pin dependency versions. 2. Add `npm ci` instead of `npm install` for reproducible installs. 3. Clear npm cache on failure as a retry step. |
| `exit code 22` / curl / REST call failed | 1. Wrap the failing REST call with `--retry 3`. 2. Pre-seed required API state before the test phase. 3. Add an environment readiness gate at pipeline start. |
| Compilation error / `COMPILATION ERROR` / `error TS` | 1. Run compile locally before pushing. 2. Add incremental compile checks to the PR checklist. |
| `Connection refused` / `timeout` | 1. Add a service readiness wait step before dependent tests. 2. Check infrastructure health dashboards for the affected service. |
| Generic / unknown | 1. Review the full console log for additional context. 2. Compare recent commits between the failed and fixed build. 3. Tag known flaky tests to reduce noise. |
```

### 6. Generate Executive Multi-Build Analysis (for develop/main/release branches)

When the user asks for an executive report, full analysis, or comparison across builds, run the multi-build analyzer:

```bash
JENKINS_USER="<user>" JENKINS_TOKEN="<token>" \
OUTPUT_PATH="~/jenkins-reports/<branch-sanitized>-executive-<YYYY-MM-DD>.md" \
python3 ./scripts/analyze-builds.py <job-url> <build-count>
```

- Default build count: **30** (use 50+ for release branches or executive reviews)
- The script fetches console logs for ALL FAILURE and UNSTABLE builds in the range
- It produces:
  - **Executive Health Summary** (success/failure/unstable rates, health score)
  - **Top Failure Root Causes** ranked by frequency across all bad builds
  - **Failed Builds — Detailed Analysis** with per-build root cause, failed tasks, error snippets
  - **Unstable Builds Analysis** with recurring pattern classification (Systemic / Recurring / Isolated)
  - **Flaky Test Detection** — tests failing only in UNSTABLE = flaky candidates
  - **Most Frequently Failing Gradle Tasks** with chronic/recurring verdicts
  - **Executive Summary** narrative tying it all together
  - **Actionable Recommendations** table with priority and impact
  - **Impact Assessment** (prod, release pipeline, dev velocity, test reliability)

### 7. Save Report as Markdown File (ALWAYS — every run)

After generating the summary, **always** save it as a `.md` file using the create_file tool.

**File path formats:**
```
~/jenkins-reports/<branch-name>-<YYYY-MM-DD>.md              ← single-build report
~/jenkins-reports/<branch-name>-executive-<YYYY-MM-DD>.md    ← multi-build executive report
```

**Examples:**
- `~/jenkins-reports/develop-2026-04-25.md`
- `~/jenkins-reports/develop-executive-2026-04-25.md`
- `~/jenkins-reports/feature-AB-668282-public-api-submission-settings-2026-04-25.md`

**File content template (single-build):**
```markdown
---
branch: <branch-name>
date: <YYYY-MM-DD>
status: SUCCESS | FAILURE | UNSTABLE
last_build: <build-number>
last_failed_build: <build-number>
generated_by: jenkins-build-status skill
---

# Jenkins Build Report — <branch-name>
**Date:** <date>
**Pipeline:** <job-url>

<full summary output from step 5>
```

- Sanitize the branch name for the filename: replace `/` and `%` with `-`
- If a report file for the same branch+date already exists, overwrite it with the latest data
- After saving, confirm to the user: `Report saved to ~/jenkins-reports/<filename>.md`

## Credentials

Jenkins may require authentication. If the API call returns 401/403:
1. Ask the user for a Jenkins API token
2. Pass it as: `curl -u <user>:<token> <url>`
3. Or use `JENKINS_USER` and `JENKINS_TOKEN` environment variables (set in shell profile)

## Notes
- Branch names with `/` are double-encoded in URLs: `/` → `%2F` → `%252F`
- Console logs can be large — fetch only the last 200 lines for efficiency
- For multibranch pipelines, the job URL already includes the branch segment
- The multi-build analyzer (`analyze-builds.py`) is at `./scripts/analyze-builds.py`
- Flaky test detection: tests that fail only in UNSTABLE builds (not FAILURE) are flagged as flaky candidates
- Pattern classification uses regex against console logs; patterns are ranked by frequency across all bad builds
