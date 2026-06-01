#!/usr/bin/env python3
"""
Jenkins Multi-Build Analyzer — Executive Report Generator v2
Fetches N builds, analyzes all FAILURE/UNSTABLE console logs,
detects flaky tests, DORA metrics, duration trends, failure heatmaps,
commit correlation, streak detection, and produces an executive report
with Mermaid visualizations.
"""

import urllib.request
import base64
import json
import sys
import re
import os
import subprocess
from datetime import datetime, timezone, timedelta
from collections import Counter, defaultdict
import statistics

# ── Configuration ────────────────────────────────────────────────────────────
JENKINS_USER = os.environ.get("JENKINS_USER", "")
JENKINS_TOKEN = os.environ.get("JENKINS_TOKEN", "")
BUILD_COUNT = int(os.environ.get("BUILD_COUNT", "30"))
OUTPUT_PATH = os.environ.get("OUTPUT_PATH", "")
GIT_REPO_PATH = os.environ.get("GIT_REPO_PATH", "")  # Local git repo path for blame analysis

if len(sys.argv) < 2:
    print("Usage: python3 analyze-builds.py <jenkins-job-url> [build-count]", file=sys.stderr)
    sys.exit(1)

JOB_URL = sys.argv[1].rstrip("/")
if len(sys.argv) >= 3:
    BUILD_COUNT = int(sys.argv[2])

auth_raw = (JENKINS_USER + ":" + JENKINS_TOKEN) if JENKINS_USER else (":" + JENKINS_TOKEN)
AUTH = base64.b64encode(auth_raw.encode()).decode()
HEADERS = {"Authorization": "Basic " + AUTH}

BRANCH = JOB_URL.split("/")[-1].replace("%252F", "/").replace("%2F", "/")

# ── Helpers ───────────────────────────────────────────────────────────────────
def api_get(url):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read())

def console_get(url):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8", errors="replace")

def fmt_ts(ms):
    if not ms:
        return "—"
    dt = datetime.utcfromtimestamp(ms / 1000)
    return dt.strftime("%Y-%m-%d %H:%M UTC")

def fmt_duration(ms):
    if not ms:
        return "—"
    secs = ms / 1000
    if secs < 60:
        return f"{secs:.0f}s"
    mins = secs / 60
    if mins < 60:
        return f"{mins:.1f}m"
    hours = mins / 60
    return f"{hours:.1f}h"

# ── Failure pattern extractors ─────────────────────────────────────────────
PATTERNS = [
    ("Zero-warnings policy (unused imports/variables)",
     r"warning: The import .+ is never used|warning: .+ is never used|compileTestJavaWarnings FAILED|compileJavaWarnings FAILED"),
    ("Unit/Integration test failure",
     r"Tests run:.*Failures: [1-9]|Tests run:.*Errors: [1-9]|\d+ test.* FAILED|BUILD FAILURE.*test"),
    ("WDIO / E2E test failure",
     r"FAILED in chrome|FAILED in firefox|UI test failures found|Pipeline UIA test run finished with exit code: [1-9]|Found \d+ failed test"),
    ("AssociationNotFoundException / 404 REST",
     r"AssociationNotFoundException|404 Not Found.*association|No association exists"),
    ("Selenium / UI test (ElementClickIntercepted)",
     r"ElementClickInterceptedException|element click intercepted|NoSuchElementException"),
    ("WebDriver / Browser error",
     r"WebDriverError|Failed to read the 'localStorage'|WebDriverException.*Access is denied"),
    ("Jenkinsfile / Groovy pipeline error",
     r"MissingPropertyException.*WorkflowScript|groovy\.lang\.MissingPropertyException|Pipeline script.*error"),
    ("OutOfMemoryError / OOMKilled",
     r"OutOfMemoryError|OOMKilled|java\.lang\.OutOfMemoryError|GC overhead limit"),
    ("Dependency resolution failure",
     r"Could not resolve|Could not download|Artifact .* not found|dependency .* failed"),
    ("curl / REST call failure (exit code 22)",
     r"exit code 22|curl.*error|HTTP request failed|RestClientException"),
    ("Compilation error (Java/TypeScript)",
     r"COMPILATION ERROR|error: cannot find symbol|error TS\d+:|error: package .* does not exist"),
    ("Connection refused / timeout",
     r"Connection refused|Connection timed out|SocketTimeoutException|ConnectException"),
    ("Disk / Space issue",
     r"No space left on device|Disk quota exceeded"),
    ("Credentials / Auth failure",
     r"401 Unauthorized|403 Forbidden|Authentication required"),
    ("Gradle task failure",
     r"Task .+ FAILED|Execution failed for task"),
    ("Duplicate localisation key",
     r"Duplicate key error.*duplicate keys found in localisation"),
]

def classify_console(log_text):
    hits = []
    for name, pattern in PATTERNS:
        if re.search(pattern, log_text, re.IGNORECASE | re.MULTILINE):
            hits.append(name)
    return hits if hits else ["Unknown / Other"]

def extract_failed_tests(log_text):
    tests = set()
    # JUnit / Gradle style: "FAILED ClassName" or "ClassName > method FAILED"
    for m in re.finditer(r"FAILED\s+([a-zA-Z][\w.$]+(?:Test|Spec|IT|Suite)[\w.$]*)", log_text):
        tests.add(m.group(1))
    for m in re.finditer(r"([a-zA-Z][\w.$]+(?:Test|Spec|IT)[\w.$]*)\s+>\s+\S+\s+FAILED", log_text):
        tests.add(m.group(1))
    # WDIO style: "FAILED in chrome - file:///__tests__/path/to/test.ts"
    for m in re.finditer(r"FAILED in \w+ - file:///.*?__tests__/(.+?\.test\.ts)", log_text):
        # Extract just the filename without path for shorter display
        full_path = m.group(1)
        filename = full_path.rsplit("/", 1)[-1] if "/" in full_path else full_path
        tests.add(filename)
    return tests

def extract_failed_tasks(log_text):
    tasks = set()
    for m in re.finditer(r"> Task ([\w:.\-]+) FAILED", log_text):
        tasks.add(m.group(1))
    return tasks

def extract_wdio_details(log_text):
    """Extract WDIO-specific metadata: PTIDs, failed spec paths, quarantine info, error patterns."""
    details = {
        "ptids": [],
        "failed_specs": [],
        "quarantine_reversions": 0,
        "failed_test_count": 0,
        "wdio_errors": [],
    }
    # WDIO failed specs with full path: "FAILED in chrome - file:///__tests__/uia/..."
    for m in re.finditer(r"FAILED in \w+ - file:///.*?(__tests__/.+?\.test\.ts)", log_text):
        details["failed_specs"].append(m.group(1))

    # PTIDs from ADO ticket creation: "Processing PTID 3440..."
    for m in re.finditer(r"Processing PTID (\d+)", log_text):
        details["ptids"].append(int(m.group(1)))

    # Total failed test count: "Found 8 failed test(s)"
    for m in re.finditer(r"Found (\d+) failed test\(s\)", log_text):
        details["failed_test_count"] = max(details["failed_test_count"], int(m.group(1)))

    # Quarantine reversions: "marked as 'fixed' but is failing again"
    details["quarantine_reversions"] = len(re.findall(
        r"marked as ['\"]fixed['\"] but is failing again", log_text, re.IGNORECASE
    ))

    # WDIO-specific errors
    wdio_error_patterns = [
        ("localStorage access denied", r"Failed to read the 'localStorage' property from 'Window'"),
        ("activemq container not found", r"unable to upgrade connection: container not found.*activemq"),
        ("Groovy MissingPropertyException", r"MissingPropertyException: No such property: \w+ for class: WorkflowScript"),
    ]
    for name, pattern in wdio_error_patterns:
        if re.search(pattern, log_text):
            details["wdio_errors"].append(name)

    return details

def extract_error_snippets(log_text, max_lines=8):
    lines = log_text.splitlines()
    snippets = []
    in_block = False
    block = []
    for line in lines:
        stripped = line.strip()
        if re.search(r"(FAILED|ERROR|Exception|error:|What went wrong)", stripped, re.IGNORECASE):
            in_block = True
            block = [stripped]
        elif in_block:
            if stripped:
                block.append(stripped)
                if len(block) >= max_lines:
                    snippets.append("\n".join(block))
                    block = []
                    in_block = False
            else:
                if block:
                    snippets.append("\n".join(block))
                block = []
                in_block = False
        if len(snippets) >= 3:
            break
    return snippets[:2]

# ── Git Blame Analysis ────────────────────────────────────────────────────────

def find_files_in_repo(class_name, repo_path):
    simple_name = class_name.rsplit(".", 1)[-1]
    is_ts_test = simple_name.endswith(".test.ts") or simple_name.endswith(".spec.ts")
    try:
        result = subprocess.run(
            ["git", "ls-files"],
            cwd=repo_path, capture_output=True, text=True, timeout=15
        )
        if result.returncode != 0:
            return []
        matches = []
        for fpath in result.stdout.strip().splitlines():
            fname = os.path.basename(fpath)
            if is_ts_test:
                if fname == simple_name:
                    matches.append(fpath)
            else:
                base = os.path.splitext(fname)[0]
                if base == simple_name:
                    matches.append(fpath)
        return matches
    except Exception:
        return []

def infer_source_files(test_class_name, repo_path):
    simple_name = test_class_name.rsplit(".", 1)[-1]

    if simple_name.endswith(".test.ts") or simple_name.endswith(".spec.ts"):
        ext = ".test.ts" if simple_name.endswith(".test.ts") else ".spec.ts"
        base = simple_name[:-len(ext)]
        candidates = [base + ".ts", base + ".tsx", base + ".js"]
        found = []
        for candidate in candidates:
            found.extend(find_files_in_repo(candidate, repo_path))
        return found

    for suffix in ("Test", "IT", "Spec", "Tests", "Suite"):
        if simple_name.endswith(suffix) and len(simple_name) > len(suffix):
            source_name = simple_name[:-len(suffix)]
            return find_files_in_repo(source_name, repo_path)
    return []

def git_blame_file(file_path, repo_path):
    """Run git blame on a file and return list of (author, date, line_no) sorted most recent first."""
    try:
        result = subprocess.run(
            ["git", "blame", "--porcelain", file_path],
            cwd=repo_path, capture_output=True, text=True, timeout=30
        )
        if result.returncode != 0:
            return []
        entries = []
        current_author = None
        current_time = None
        current_line = None
        for line in result.stdout.splitlines():
            # Porcelain format: first line of each entry is "<hash> <orig_line> <final_line> [<num_lines>]"
            if re.match(r'^[0-9a-f]{40} ', line):
                parts = line.split()
                current_line = int(parts[2]) if len(parts) >= 3 else None
            elif line.startswith("author "):
                current_author = line[len("author "):]
            elif line.startswith("author-time "):
                try:
                    current_time = int(line[len("author-time "):])
                except ValueError:
                    current_time = None
            elif line.startswith("\t"):
                # Content line — this ends the entry
                if current_author and current_time and current_author != "Not Committed Yet":
                    entries.append((current_author, current_time, current_line))
                current_author = None
                current_time = None
                current_line = None
        return entries
    except Exception:
        return []

def summarize_blame(blame_entries):
    """Deduplicate and sort blame entries: most recent touch first.
    Returns list of (author, most_recent_date, line_count).
    """
    author_data = defaultdict(lambda: {"latest": 0, "lines": 0})
    for author, ts, line_no in blame_entries:
        d = author_data[author]
        d["latest"] = max(d["latest"], ts)
        d["lines"] += 1
    result = []
    for author, data in author_data.items():
        result.append((author, data["latest"], data["lines"]))
    result.sort(key=lambda x: -x[1])  # most recent first
    return result

def analyze_blame_for_tests(failed_tests, repo_path):
    """For each failing test, find the test file + source file and blame them.
    Returns dict: test_name -> {test_file, source_files, test_blame, source_blame}
    """
    if not repo_path or not os.path.isdir(repo_path):
        return {}

    blame_results = {}
    for test_class in failed_tests:
        test_files = find_files_in_repo(test_class, repo_path)
        source_files = infer_source_files(test_class, repo_path)

        test_blame_entries = []
        for tf in test_files:
            test_blame_entries.extend(git_blame_file(tf, repo_path))

        source_blame_entries = []
        for sf in source_files:
            source_blame_entries.extend(git_blame_file(sf, repo_path))

        if test_blame_entries or source_blame_entries:
            blame_results[test_class] = {
                "test_files": test_files,
                "source_files": source_files,
                "test_blame": summarize_blame(test_blame_entries),
                "source_blame": summarize_blame(source_blame_entries),
            }

    return blame_results

# ── Fetch build list (with duration + changeSets) ────────────────────────────
print(f"Fetching last {BUILD_COUNT} builds for: {BRANCH}", file=sys.stderr)

builds_url = (
    JOB_URL + "/api/json?tree=builds[number,result,timestamp,duration,"
    "changeSets[items[commitId,msg,author[fullName]]]]"
    f"{{0,{BUILD_COUNT}}}"
)
try:
    builds_data = api_get(builds_url)
    builds = builds_data.get("builds", [])
except Exception as e:
    # Fallback without changeSets
    print(f"  Retrying without changeSets...", file=sys.stderr)
    builds_url = (
        JOB_URL + "/api/json?tree=builds[number,result,timestamp,duration]"
        f"{{0,{BUILD_COUNT}}}"
    )
    builds_data = api_get(builds_url)
    builds = builds_data.get("builds", [])

print(f"  Found {len(builds)} builds", file=sys.stderr)

# ── Analyze each FAILURE / UNSTABLE build ────────────────────────────────────
results = defaultdict(list)
build_details = []
test_fail_count = Counter()
task_fail_count = Counter()
pattern_count = Counter()

# Store all builds with metadata for DORA / trend analysis
all_build_meta = []  # [{number, result, timestamp, duration, commits}]

for b in builds:
    num = b.get("number")
    result = b.get("result") or "OTHER"
    ts = b.get("timestamp", 0)
    dur = b.get("duration", 0)
    results[result].append(num)

    # Extract commits
    commits = []
    for cs in b.get("changeSets", []):
        for item in cs.get("items", []):
            commits.append({
                "id": (item.get("commitId") or "")[:8],
                "msg": (item.get("msg") or "").split("\n")[0][:80],
                "author": (item.get("author") or {}).get("fullName", "unknown"),
            })

    all_build_meta.append({
        "number": num,
        "result": result,
        "timestamp": ts,
        "duration": dur,
        "commits": commits,
    })

# Sort by build number ascending for timeline analysis
all_build_meta.sort(key=lambda x: x["number"])

for b in builds:
    num = b.get("number")
    result = b.get("result") or "OTHER"
    if result not in ("FAILURE", "UNSTABLE"):
        continue

    print(f"  Analyzing build #{num} ({result})...", file=sys.stderr)
    console_url = JOB_URL + f"/{num}/consoleText"
    try:
        log = console_get(console_url)
    except Exception as e:
        print(f"    WARNING: could not fetch console for #{num}: {e}", file=sys.stderr)
        log = ""

    patterns = classify_console(log)
    failed_tests = extract_failed_tests(log)
    failed_tasks = extract_failed_tasks(log)
    snippets = extract_error_snippets(log)
    wdio_details = extract_wdio_details(log)

    for p in patterns:
        pattern_count[p] += 1
    for t in failed_tasks:
        task_fail_count[t] += 1
    for t in failed_tests:
        test_fail_count[t] += 1

    build_details.append({
        "number": num,
        "result": result,
        "timestamp": b.get("timestamp", 0),
        "patterns": patterns,
        "failed_tests": list(failed_tests),
        "failed_tasks": list(failed_tasks),
        "snippets": snippets,
        "wdio": wdio_details,
    })

# ── DORA Metrics Computation ─────────────────────────────────────────────────
print("  Computing DORA metrics...", file=sys.stderr)

# 1. Mean Time to Recovery (MTTR)
# For each failure/unstable streak, find how many builds until next SUCCESS
mttr_values = []  # in number of builds
mttr_time_values = []  # in hours
sorted_builds = all_build_meta  # already sorted ascending

for i, bm in enumerate(sorted_builds):
    if bm["result"] in ("FAILURE", "UNSTABLE"):
        # Find the next SUCCESS after this
        for j in range(i + 1, len(sorted_builds)):
            if sorted_builds[j]["result"] == "SUCCESS":
                mttr_values.append(j - i)
                if sorted_builds[j]["timestamp"] and bm["timestamp"]:
                    delta_h = (sorted_builds[j]["timestamp"] - bm["timestamp"]) / 3600000
                    if delta_h > 0:
                        mttr_time_values.append(delta_h)
                break

avg_mttr_builds = round(statistics.mean(mttr_values), 1) if mttr_values else 0
avg_mttr_hours = round(statistics.mean(mttr_time_values), 1) if mttr_time_values else 0

# 2. Build Duration Stats
durations = [bm["duration"] for bm in all_build_meta if bm["duration"] > 0]
if durations:
    dur_avg = statistics.mean(durations)
    dur_median = statistics.median(durations)
    dur_p95 = sorted(durations)[int(len(durations) * 0.95)] if len(durations) >= 5 else max(durations)
    dur_min = min(durations)
    dur_max = max(durations)
    dur_stdev = statistics.stdev(durations) if len(durations) >= 2 else 0
else:
    dur_avg = dur_median = dur_p95 = dur_min = dur_max = dur_stdev = 0

# Detect duration outliers (> mean + 2*stdev)
duration_outliers = []
if dur_stdev > 0:
    threshold = dur_avg + 2 * dur_stdev
    for bm in all_build_meta:
        if bm["duration"] > threshold:
            duration_outliers.append(bm)

# 3. Success Rate Trend (last 25% vs first 75%)
split_point = max(1, len(sorted_builds) // 4)
recent_builds = sorted_builds[-split_point:]
older_builds = sorted_builds[:-split_point]
recent_success = sum(1 for b in recent_builds if b["result"] == "SUCCESS")
older_success = sum(1 for b in older_builds if b["result"] == "SUCCESS")
recent_rate = round(recent_success / len(recent_builds) * 100) if recent_builds else 0
older_rate = round(older_success / len(older_builds) * 100) if older_builds else 0
trend_delta = recent_rate - older_rate
trend_emoji = "📈" if trend_delta > 5 else "📉" if trend_delta < -5 else "➡️"

# 4. Failure Heatmap (day of week + hour)
day_hour_failures = defaultdict(int)
day_hour_total = defaultdict(int)
day_names = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

for bm in all_build_meta:
    if bm["timestamp"]:
        dt = datetime.utcfromtimestamp(bm["timestamp"] / 1000)
        day = day_names[dt.weekday()]
        hour_bucket = f"{(dt.hour // 6) * 6:02d}-{((dt.hour // 6) + 1) * 6:02d}"
        key = f"{day} {hour_bucket}"
        day_hour_total[key] += 1
        if bm["result"] in ("FAILURE", "UNSTABLE"):
            day_hour_failures[key] += 1

# 5. Build Throughput
if all_build_meta and all_build_meta[0]["timestamp"] and all_build_meta[-1]["timestamp"]:
    span_days = max(1, (all_build_meta[-1]["timestamp"] - all_build_meta[0]["timestamp"]) / 86400000)
    builds_per_day = round(len(all_build_meta) / span_days, 1)
    builds_per_week = round(builds_per_day * 7, 1)
else:
    span_days = builds_per_day = builds_per_week = 0

# 6. Failure Streak Detection
streaks = []
current_streak = []
for bm in sorted_builds:
    if bm["result"] in ("FAILURE", "UNSTABLE"):
        current_streak.append(bm)
    else:
        if current_streak:
            streaks.append(list(current_streak))
        current_streak = []
if current_streak:
    streaks.append(list(current_streak))

longest_streak = max(streaks, key=len) if streaks else []
# Current streak (is the pipeline currently broken?)
is_currently_broken = sorted_builds[-1]["result"] in ("FAILURE", "UNSTABLE") if sorted_builds else False
current_red_streak = []
for bm in reversed(sorted_builds):
    if bm["result"] in ("FAILURE", "UNSTABLE"):
        current_red_streak.append(bm)
    else:
        break

# 7. Commit Correlation
commit_fail_authors = Counter()
commit_fail_msgs = []
for bm in all_build_meta:
    if bm["result"] in ("FAILURE", "UNSTABLE") and bm.get("commits"):
        for c in bm["commits"]:
            commit_fail_authors[c["author"]] += 1
            commit_fail_msgs.append({
                "build": bm["number"],
                "result": bm["result"],
                "author": c["author"],
                "msg": c["msg"],
                "commit": c["id"],
            })

# 8. Recovery Pattern — who fixed it?
recovery_events = []
for i, bm in enumerate(sorted_builds):
    if bm["result"] in ("FAILURE", "UNSTABLE"):
        for j in range(i + 1, len(sorted_builds)):
            if sorted_builds[j]["result"] == "SUCCESS":
                fix_build = sorted_builds[j]
                fix_authors = set(c["author"] for c in fix_build.get("commits", []))
                break_authors = set(c["author"] for c in bm.get("commits", []))
                recovery_events.append({
                    "broken_build": bm["number"],
                    "broken_result": bm["result"],
                    "fixed_build": fix_build["number"],
                    "fix_authors": list(fix_authors) if fix_authors else ["(no commit data)"],
                    "break_authors": list(break_authors) if break_authors else ["(no commit data)"],
                    "builds_to_fix": j - i,
                })
                break

# ── Flaky test detection ──────────────────────────────────────────────────────
flaky_candidates = []
for test, count in test_fail_count.most_common():
    in_unstable = any(
        test in d["failed_tests"] and d["result"] == "UNSTABLE"
        for d in build_details
    )
    in_failure = any(
        test in d["failed_tests"] and d["result"] == "FAILURE"
        for d in build_details
    )
    if in_unstable and not in_failure:
        flaky_candidates.append((test, count, "UNSTABLE only"))
    elif in_unstable and in_failure:
        flaky_candidates.append((test, count, "UNSTABLE + FAILURE"))

all_ptids = set()
all_wdio_failed_specs = set()
total_quarantine_reversions = 0
for d in build_details:
    wdio = d.get("wdio", {})
    all_ptids.update(wdio.get("ptids", []))
    all_wdio_failed_specs.update(wdio.get("failed_specs", []))
    total_quarantine_reversions += wdio.get("quarantine_reversions", 0)

# ── Build the report ──────────────────────────────────────────────────────────
now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
date_str = datetime.now(timezone.utc).strftime("%Y-%m-%d")

total = len(builds)
success_count = len(results.get("SUCCESS", []))
failure_count = len(results.get("FAILURE", []))
unstable_count = len(results.get("UNSTABLE", []))
aborted_count = len(results.get("ABORTED", []))
not_built_count = len(results.get("NOT_BUILT", []))

success_rate = round(success_count / total * 100) if total else 0
failure_rate = round(failure_count / total * 100) if total else 0
unstable_rate = round(unstable_count / total * 100) if total else 0

meaningful_total = total - not_built_count
adjusted_success_rate = round(success_count / meaningful_total * 100) if meaningful_total else 0
adjusted_failure_rate = round(failure_count / meaningful_total * 100) if meaningful_total else 0
adjusted_unstable_rate = round(unstable_count / meaningful_total * 100) if meaningful_total else 0

health_score = adjusted_success_rate if not_built_count > total * 0.1 else success_rate
health_label = (
    "🟢 Healthy" if health_score >= 85 else
    "🟡 Needs Attention" if health_score >= 65 else
    "🔴 Critical"
)

lines = []

lines += [
    "---",
    f"branch: {BRANCH}",
    f"date: {date_str}",
    f"builds_analyzed: {total}",
    f"success_rate: {success_rate}%",
    f"mttr_hours: {avg_mttr_hours}",
    f"builds_per_day: {builds_per_day}",
    f"generated_by: jenkins-build-analyzer-v2",
    "---",
    "",
    '<div align="center">',
    "",
    "# 📊 Jenkins Executive Build Report",
    "",
    f"### `{BRANCH}`",
    "",
    f"**Pipeline:** [{BRANCH}]({JOB_URL}) &nbsp;|&nbsp; **Date:** {date_str} &nbsp;|&nbsp; **Builds Analyzed:** {total}",
    "",
    "</div>",
    "",
    "---",
    "",
    "## 🏥 Executive Health Summary",
    "",
    f"| Metric | Value |",
    f"|--------|-------|",
    f"| **Overall Health** | {health_label} |",
    f"| **Success Rate** | ✅ {success_rate}% ({success_count}/{total} builds) |",
    f"| **Failure Rate** | ❌ {failure_rate}% ({failure_count}/{total} builds) |",
    f"| **Unstable Rate** | ⚠️ {unstable_rate}% ({unstable_count}/{total} builds) |",
    f"| **Aborted** | 🚫 {aborted_count} builds |",
    f"| **Not Built** | 🚫 {not_built_count} builds |",
    f"| **Builds Analyzed** | Last {total} builds |",
    "",
]

if not_built_count > total * 0.1:
    lines += [
        f"| **Adjusted Success Rate** | ✅ {adjusted_success_rate}% ({success_count}/{meaningful_total} meaningful builds) |",
        "",
        f"> ⚠️ **{not_built_count} NOT_BUILT builds ({round(not_built_count / total * 100)}%)** skew the headline rate. "
        f"Adjusted rate (excluding NOT_BUILT): **{adjusted_success_rate}%**",
        "",
    ]

filled = round(success_rate / 10)
empty = 10 - filled
bar = "█" * filled + "░" * empty
lines += [
    f"> **Pipeline Health:** `{bar}` {success_rate}%",
    "",
    "---",
    "",
]

# ── NOT_BUILT Analysis ────────────────────────────────────────────────────────
if not_built_count >= 3 or not_built_count > total * 0.1:
    lines += [
        "## 🚫 NOT_BUILT Analysis",
        "",
        f"**{not_built_count} of {total} builds ({round(not_built_count / total * 100)}%)** ended as NOT_BUILT — "
        "builds triggered but terminated before meaningful execution (typically duplicate triggers or early aborts).",
        "",
        "| Metric | Value |",
        "|--------|-------|",
        f"| **NOT_BUILT count** | {not_built_count} / {total} ({round(not_built_count / total * 100)}%) |",
        f"| **Adjusted success rate** | {success_count} / {meaningful_total} meaningful builds = **{adjusted_success_rate}%** |",
        f"| **Adjusted unstable rate** | {unstable_count} / {meaningful_total} = **{adjusted_unstable_rate}%** |",
        f"| **Adjusted failure rate** | {failure_count} / {meaningful_total} = **{adjusted_failure_rate}%** |",
        "",
    ]
    if not_built_count > total * 0.2:
        lines += [
            f"> ⚠️ **High NOT_BUILT rate ({round(not_built_count / total * 100)}%).** "
            "Consider adding concurrent build prevention or deduplication logic in the Jenkinsfile.",
            "",
        ]
    lines += ["---", ""]

# ── DORA Metrics ──────────────────────────────────────────────────────────────
lines += [
    "## 📐 DORA / DevOps Metrics",
    "",
    "| Metric | Value | Industry Benchmark |",
    "|--------|-------|--------------------|",
    f"| **Mean Time to Recovery (MTTR)** | {avg_mttr_hours:.1f} hours ({avg_mttr_builds:.1f} builds) | Elite: <1h, High: <1d |",
    f"| **Build Throughput** | {builds_per_day}/day ({builds_per_week}/week) | Higher = better |",
    f"| **Change Failure Rate** | {failure_rate + unstable_rate}% | Elite: <5%, High: <15% |",
    f"| **Deployment Frequency** | ~{builds_per_day:.0f} per day | Elite: on-demand, High: daily |",
    "",
]

# DORA verdict
dora_score = 0
if avg_mttr_hours <= 1:
    dora_score += 2
elif avg_mttr_hours <= 24:
    dora_score += 1
if (failure_rate + unstable_rate) < 5:
    dora_score += 2
elif (failure_rate + unstable_rate) < 15:
    dora_score += 1
if builds_per_day >= 3:
    dora_score += 2
elif builds_per_day >= 1:
    dora_score += 1

dora_label = "🏆 Elite" if dora_score >= 5 else "🟢 High" if dora_score >= 3 else "🟡 Medium" if dora_score >= 1 else "🔴 Low"
lines += [
    f"> **DORA Performance Level:** {dora_label} (score {dora_score}/6)",
    "",
    "---",
    "",
]

# ── Build Duration Trend ─────────────────────────────────────────────────────
if durations:
    lines += [
        "## ⏱️ Build Duration Analysis",
        "",
        "| Metric | Value |",
        "|--------|-------|",
        f"| **Average** | {fmt_duration(dur_avg)} |",
        f"| **Median** | {fmt_duration(dur_median)} |",
        f"| **P95** | {fmt_duration(dur_p95)} |",
        f"| **Min** | {fmt_duration(dur_min)} |",
        f"| **Max** | {fmt_duration(dur_max)} |",
        f"| **Std Dev** | {fmt_duration(dur_stdev)} |",
        "",
    ]

    # Duration outliers
    if duration_outliers:
        lines += [
            f"### ⚠️ Duration Outliers ({len(duration_outliers)} builds > 2σ above mean)",
            "",
            "| Build | Duration | Result | Δ from Mean |",
            "|-------|----------|--------|-------------|",
        ]
        for bm in sorted(duration_outliers, key=lambda x: -x["duration"])[:5]:
            delta = bm["duration"] - dur_avg
            lines.append(
                f"| [#{bm['number']}]({JOB_URL}/{bm['number']}/) | {fmt_duration(bm['duration'])} | {bm['result']} | +{fmt_duration(delta)} |"
            )
        lines += [""]

    # Duration sparkline via Mermaid
    # Sample up to 30 builds for chart readability
    chart_builds = all_build_meta[-30:] if len(all_build_meta) > 30 else all_build_meta
    lines += [
        "### Build Duration Trend (last {} builds)".format(len(chart_builds)),
        "",
        "```mermaid",
        "xychart-beta",
        f'  title "Build Duration (minutes) — {BRANCH}"',
        "  x-axis [{}]".format(", ".join(f'"#{b["number"]}"' for b in chart_builds)),
        "  y-axis \"Duration (min)\"",
        "  bar [{}]".format(", ".join(f'{b["duration"] / 60000:.1f}' for b in chart_builds)),
        "```",
        "",
        "---",
        "",
    ]

# ── Success Rate Trend ────────────────────────────────────────────────────────
lines += [
    "## 📈 Success Rate Trend",
    "",
    f"| Period | Builds | Success Rate | Trend |",
    f"|--------|--------|:------------:|-------|",
    f"| Older ({len(older_builds)} builds) | #{older_builds[0]['number']}–#{older_builds[-1]['number']} | {older_rate}% | baseline |",
    f"| Recent ({len(recent_builds)} builds) | #{recent_builds[0]['number']}–#{recent_builds[-1]['number']} | {recent_rate}% | {trend_emoji} {'+' if trend_delta > 0 else ''}{trend_delta}% |",
    "",
]

# Mermaid build result timeline
chart_builds2 = all_build_meta[-40:] if len(all_build_meta) > 40 else all_build_meta
result_map = {"SUCCESS": 3, "UNSTABLE": 2, "FAILURE": 1, "ABORTED": 0}
lines += [
    "### Build Result Timeline",
    "",
    "```mermaid",
    "xychart-beta",
    f'  title "Build Results — {BRANCH} (3=✅ 2=⚠️ 1=❌ 0=🚫)"',
    "  x-axis [{}]".format(", ".join(f'"#{b["number"]}"' for b in chart_builds2)),
    "  y-axis \"Result\" 0 --> 3",
    "  bar [{}]".format(", ".join(str(result_map.get(b["result"], 0)) for b in chart_builds2)),
    "```",
    "",
]

if trend_delta > 5:
    lines.append(f"> 📈 **Improving:** Success rate is up {trend_delta}% in recent builds compared to earlier builds.")
elif trend_delta < -5:
    lines.append(f"> 📉 **Degrading:** Success rate has dropped {abs(trend_delta)}% in recent builds. Investigate recent changes.")
else:
    lines.append(f"> ➡️ **Stable:** Success rate is holding steady across the analyzed window.")
lines += ["", "---", ""]

# ── Failure Heatmap ───────────────────────────────────────────────────────────
if day_hour_failures:
    lines += [
        "## 🗓️ Failure Heatmap — When Do Failures Happen?",
        "",
        "| Time Window | Total Builds | Failures | Failure Rate | Heat |",
        "|-------------|:------------:|:--------:|:------------:|------|",
    ]
    for key in sorted(day_hour_total.keys()):
        total_k = day_hour_total[key]
        fail_k = day_hour_failures.get(key, 0)
        rate_k = round(fail_k / total_k * 100) if total_k else 0
        heat = "🔴" if rate_k > 30 else "🟡" if rate_k > 15 else "🟢" if total_k > 0 else "⚪"
        lines.append(f"| {key} | {total_k} | {fail_k} | {rate_k}% | {heat} |")

    # Find the worst time slot
    worst_slot = max(day_hour_failures.keys(), key=lambda k: day_hour_failures[k] / max(day_hour_total[k], 1))
    worst_rate = round(day_hour_failures[worst_slot] / day_hour_total[worst_slot] * 100)
    lines += [
        "",
        f"> 🔥 **Hotspot:** `{worst_slot}` has the highest failure rate ({worst_rate}%). Consider scheduling deployments/merges outside this window.",
        "",
        "---",
        "",
    ]

# ── Failure Streak Detection ─────────────────────────────────────────────────
lines += [
    "## 🔥 Failure Streak Analysis",
    "",
]

if longest_streak:
    lines += [
        f"| Metric | Value |",
        f"|--------|-------|",
        f"| **Longest Failure Streak** | {len(longest_streak)} builds (#{longest_streak[0]['number']}–#{longest_streak[-1]['number']}) |",
        f"| **Total Streaks** | {len(streaks)} separate failure sequences |",
        f"| **Currently Broken?** | {'🔴 YES — {0} build(s) and counting'.format(len(current_red_streak)) if is_currently_broken else '🟢 No — pipeline is green'} |",
        "",
    ]

    if is_currently_broken and current_red_streak:
        lines += [
            "### 🚨 Active Red Streak",
            "",
            "| Build | Result | Timestamp |",
            "|-------|--------|-----------|",
        ]
        for bm in reversed(current_red_streak):
            lines.append(f"| [#{bm['number']}]({JOB_URL}/{bm['number']}/) | {bm['result']} | {fmt_ts(bm['timestamp'])} |")
        lines += [""]
else:
    lines += [
        "🎉 **No failure streaks detected!** All failures were isolated incidents followed by immediate recovery.",
        "",
    ]

lines += ["---", ""]

# ── Pattern breakdown ─────────────────────────────────────────────────────────
lines += [
    "## 🔍 Top Failure Root Causes (All Failures + Unstables)",
    "",
    "Ranked by frequency across the last {} builds:".format(total),
    "",
    "| # | Root Cause | Occurrences | % of Bad Builds |",
    "|---|-----------|:-----------:|:---------------:|",
]

bad_total = failure_count + unstable_count or 1
for rank, (pattern, count) in enumerate(pattern_count.most_common(10), 1):
    pct = round(count / bad_total * 100)
    lines.append(f"| {rank} | {pattern} | **{count}** | {pct}% |")

lines += ["", "---", ""]

# ── Failure details ───────────────────────────────────────────────────────────
failure_builds = [d for d in build_details if d["result"] == "FAILURE"]
if failure_builds:
    lines += [
        "## ❌ Failed Builds — Detailed Analysis",
        "",
        f"**{len(failure_builds)} FAILURE build(s)** in the last {total} builds:",
        "",
    ]
    for d in failure_builds:
        bnum = d["number"]
        burl = f"{JOB_URL}/{bnum}/"
        btime = fmt_ts(d["timestamp"])
        lines += [
            f"### Build [#{bnum}]({burl}) — {btime}",
            "",
            f"**Root Cause(s):** {', '.join(d['patterns'])}",
            "",
        ]
        if d["failed_tasks"]:
            lines.append("**Failed Gradle Tasks:**")
            for t in sorted(d["failed_tasks"])[:5]:
                lines.append(f"- `{t}`")
            lines.append("")
        if d["failed_tests"]:
            lines.append("**Failed Tests:**")
            for t in sorted(d["failed_tests"])[:5]:
                lines.append(f"- `{t}`")
            lines.append("")
        if d["snippets"]:
            lines += [
                "<details>",
                "<summary>📋 Error snippet</summary>",
                "",
                "```",
                d["snippets"][0][:1000],
                "```",
                "",
                "</details>",
                "",
            ]
    lines += ["---", ""]

# ── Unstable builds ───────────────────────────────────────────────────────────
unstable_builds = [d for d in build_details if d["result"] == "UNSTABLE"]
if unstable_builds:
    lines += [
        "## ⚠️ Unstable Builds — Analysis",
        "",
        f"**{len(unstable_builds)} UNSTABLE build(s)** — artifacts published but tests had intermittent failures:",
        "",
        "| Build | Date | Root Cause(s) | Failed Tasks |",
        "|-------|------|---------------|-------------|",
    ]
    for d in unstable_builds:
        bnum = d["number"]
        burl = f"{JOB_URL}/{bnum}/"
        btime = fmt_ts(d["timestamp"])
        causes = ", ".join(d["patterns"])[:60]
        tasks = ", ".join(f"`{t}`" for t in sorted(d["failed_tasks"])[:2]) or "—"
        lines.append(f"| [#{bnum}]({burl}) | {btime} | {causes} | {tasks} |")
    lines += ["", "---", ""]

# ── Common patterns across unstables ─────────────────────────────────────────
if unstable_builds:
    unstable_patterns = Counter()
    for d in unstable_builds:
        for p in d["patterns"]:
            unstable_patterns[p] += 1

    lines += [
        "### 🔁 Recurring Patterns in Unstable Builds",
        "",
        "| Pattern | Occurrences in Unstable | Verdict |",
        "|---------|:-----------------------:|---------|",
    ]
    for pat, cnt in unstable_patterns.most_common(8):
        verdict = "🚨 Systemic" if cnt >= len(unstable_builds) * 0.5 else "⚠️ Recurring" if cnt >= 2 else "🔵 Isolated"
        lines.append(f"| {pat} | {cnt} | {verdict} |")
    lines += ["", "---", ""]

# ── Flaky test detection ──────────────────────────────────────────────────────
if flaky_candidates or task_fail_count:
    lines += [
        "## 🎲 Flaky & Recurring Test Detection",
        "",
    ]

if flaky_candidates:
    lines += [
        "### Flaky Test Candidates",
        "",
        "Tests that failed intermittently (present in UNSTABLE builds = build passes overall but test unreliable):",
        "",
        "| Test Class | Failure Count | Pattern |",
        "|-----------|:-------------:|---------|",
    ]
    for test, count, pattern in sorted(flaky_candidates, key=lambda x: -x[1])[:10]:
        lines.append(f"| `{test}` | {count} | {pattern} |")
    lines += [""]

if all_wdio_failed_specs:
    lines += [
        "### WDIO Failed Spec Files",
        "",
        "| Spec Path | Builds Affected |",
        "|-----------|:---------------:|",
    ]
    spec_counts = Counter()
    for d in build_details:
        for spec in d.get("wdio", {}).get("failed_specs", []):
            spec_counts[spec] += 1
    for spec, cnt in spec_counts.most_common(15):
        lines.append(f"| `{spec}` | {cnt} |")
    lines += [""]

if all_ptids:
    lines += [
        f"### WDIO PTIDs (ADO Tracking)",
        "",
        f"**{len(all_ptids)} unique PTID(s)** created/referenced across analyzed builds: "
        + ", ".join(f"`{p}`" for p in sorted(all_ptids)),
        "",
    ]

if total_quarantine_reversions:
    lines += [
        f"> ⚠️ **{total_quarantine_reversions} quarantine reversion(s)** detected — "
        "tests previously marked as 'fixed' are failing again. Review quarantine process.",
        "",
    ]

if task_fail_count:
    lines += [
        "### Most Frequently Failing Gradle Tasks",
        "",
        "| Task | Times Failed | Verdict |",
        "|------|:------------:|---------|",
    ]
    for task, count in task_fail_count.most_common(10):
        verdict = (
            "🚨 Chronic — investigate immediately" if count >= 3 else
            "⚠️ Recurring — monitor closely" if count >= 2 else
            "🔵 Isolated"
        )
        lines.append(f"| `{task}` | {count} | {verdict} |")
    lines += ["", "---", ""]

# ── Git Blame — Who to Contact ────────────────────────────────────────────────
all_failed_tests = set()
for d in build_details:
    all_failed_tests.update(d["failed_tests"])

if all_failed_tests:
    blame_data = {}
    if GIT_REPO_PATH:
        print("  Running git blame analysis on failing tests...", file=sys.stderr)
        blame_data = analyze_blame_for_tests(all_failed_tests, GIT_REPO_PATH)

    lines += [
        "## 👤 Git Blame — Who to Contact About Failing Tests",
        "",
    ]

    if blame_data:
        lines += [
            "For each failing test, shows who last touched the **test file** and the **source code under test**,",
            "ordered from most recent to oldest. Start with the most recent author — they have the freshest context.",
            "",
        ]

        for test_class in sorted(blame_data.keys()):
            info = blame_data[test_class]
            lines += [
                f"### `{test_class}`",
                "",
            ]

            if info["test_files"]:
                lines.append(f"**Test file(s):** {', '.join(f'`{f}`' for f in info['test_files'])}")
            if info["source_files"]:
                lines.append(f"**Source file(s) under test:** {', '.join(f'`{f}`' for f in info['source_files'])}")
            lines.append("")

            if info["test_blame"]:
                lines += [
                    "**Test file authors** (most recent first):",
                    "",
                    "| Author | Last Touched | Lines Owned |",
                    "|--------|-------------|:-----------:|",
                ]
                for author, ts, line_count in info["test_blame"][:8]:
                    dt = datetime.utcfromtimestamp(ts).strftime("%Y-%m-%d")
                    lines.append(f"| {author} | {dt} | {line_count} |")
                lines.append("")

            if info["source_blame"]:
                lines += [
                    "**Source code authors** (most recent first):",
                    "",
                    "| Author | Last Touched | Lines Owned |",
                    "|--------|-------------|:-----------:|",
                ]
                for author, ts, line_count in info["source_blame"][:8]:
                    dt = datetime.utcfromtimestamp(ts).strftime("%Y-%m-%d")
                    lines.append(f"| {author} | {dt} | {line_count} |")
                lines.append("")

        # Aggregate: across all failing tests, who is the top contact?
        all_blame_combined = Counter()
        all_blame_latest = {}
        for info in blame_data.values():
            for author, ts, lc in info["test_blame"] + info["source_blame"]:
                all_blame_combined[author] += lc
                all_blame_latest[author] = max(all_blame_latest.get(author, 0), ts)

        if all_blame_combined:
            sorted_contacts = sorted(
                all_blame_combined.keys(),
                key=lambda a: -all_blame_latest[a]
            )
            lines += [
                "### 📞 Recommended Contact Order (Across All Failing Tests)",
                "",
                "| Priority | Author | Last Touch | Total Lines in Affected Files |",
                "|:--------:|--------|------------|:----------------------------:|",
            ]
            for rank, author in enumerate(sorted_contacts[:10], 1):
                dt = datetime.utcfromtimestamp(all_blame_latest[author]).strftime("%Y-%m-%d")
                lines.append(f"| {rank} | {author} | {dt} | {all_blame_combined[author]} |")
            lines += [
                "",
                "> 💡 **Tip:** Start with #1 — they touched the relevant code most recently and likely have the best context.",
                "",
            ]

        lines += ["---", ""]
    else:
        if not GIT_REPO_PATH:
            lines += [
                f"> ℹ️ **{len(all_failed_tests)} failing test(s) detected** but no local git repo path provided (`GIT_REPO_PATH` env var). "
                "Set it to enable per-test blame analysis and contact recommendations.",
                "",
                "**Failing tests:**",
                "",
            ]
        else:
            lines += [
                f"> ℹ️ **{len(all_failed_tests)} failing test(s) detected** but none could be matched to files in `{GIT_REPO_PATH}`. "
                "This may happen with WDIO/E2E tests whose filenames don't exist in the local clone.",
                "",
                "**Failing tests:**",
                "",
            ]
        for t in sorted(all_failed_tests):
            lines.append(f"- `{t}`")
        lines += ["", "---", ""]

# ── Commit Correlation ────────────────────────────────────────────────────────
if commit_fail_msgs:
    lines += [
        "## 🔗 Commit Correlation — Changes in Failing Builds",
        "",
        "Commits present in FAILURE/UNSTABLE builds (correlation, not blame):",
        "",
        "| Build | Result | Author | Commit | Message |",
        "|-------|--------|--------|--------|---------|",
    ]
    seen_builds = set()
    for entry in commit_fail_msgs[:20]:
        lines.append(
            f"| #{entry['build']} | {entry['result']} | {entry['author']} | `{entry['commit']}` | {entry['msg'][:50]} |"
        )
    lines += [""]

    if commit_fail_authors:
        lines += [
            "### Authors with Most Changes in Failing Builds",
            "",
            "| Author | # Commits in Bad Builds | Note |",
            "|--------|:-----------------------:|------|",
        ]
        for author, cnt in commit_fail_authors.most_common(5):
            note = "⚠️ Frequent — may need CI feedback loop" if cnt >= 3 else ""
            lines.append(f"| {author} | {cnt} | {note} |")
        lines += [
            "",
            "> ℹ️ This is **correlation, not blame**. Authors listed may have committed alongside a pre-existing issue.",
            "",
        ]

    lines += ["---", ""]

# ── Recovery Pattern ──────────────────────────────────────────────────────────
if recovery_events:
    lines += [
        "## 🔧 Recovery Patterns — Who Fixed What?",
        "",
        "| Broken Build | Type | Fixed In | Builds to Fix | Fix Author(s) |",
        "|-------------|------|----------|:-------------:|---------------|",
    ]
    for ev in recovery_events[:10]:
        fix_str = ", ".join(ev["fix_authors"][:2])
        lines.append(
            f"| [#{ev['broken_build']}]({JOB_URL}/{ev['broken_build']}/) | {ev['broken_result']} | "
            f"[#{ev['fixed_build']}]({JOB_URL}/{ev['fixed_build']}/) | {ev['builds_to_fix']} | {fix_str} |"
        )

    # Stats
    fix_counts = [ev["builds_to_fix"] for ev in recovery_events]
    lines += [
        "",
        f"| Metric | Value |",
        f"|--------|-------|",
        f"| **Avg builds to fix** | {statistics.mean(fix_counts):.1f} |",
        f"| **Fastest recovery** | {min(fix_counts)} build(s) |",
        f"| **Slowest recovery** | {max(fix_counts)} build(s) |",
        "",
        "---",
        "",
    ]

# ── Executive narrative ──────────────────────────────────────────────────────
lines += [
    "## 📝 Executive Summary & Common Issues",
    "",
    f"Over the last **{total} builds**, the `{BRANCH}` pipeline shows a **{success_rate}% success rate**.",
    "",
]

if pattern_count:
    lines += [
        "**The most common recurring issues are:**",
        "",
    ]
    for i, (pat, cnt) in enumerate(pattern_count.most_common(5), 1):
        pct = round(cnt / bad_total * 100)
        lines.append(f"{i}. **{pat}** — appeared in **{cnt}** builds ({pct}% of all failures/unstables)")
    lines += [""]

if unstable_rate > 15:
    lines.append(
        f"> ⚠️ **High Unstable Rate ({unstable_rate}%):** More than 1 in 6 builds are UNSTABLE, "
        "suggesting systemic intermittent test failures. These should be treated as failures, not noise."
    )
    lines += [""]

if flaky_candidates:
    lines.append(
        f"> 🎲 **Flaky Tests Detected:** {len(flaky_candidates)} test class(es) are failing intermittently "
        "across multiple builds. These inflate the UNSTABLE rate and erode developer trust in CI."
    )
    lines += [""]

if all_wdio_failed_specs:
    lines.append(
        f"> 🧪 **WDIO E2E Failures:** {len(all_wdio_failed_specs)} unique spec file(s) failing across builds. "
        f"{len(all_ptids)} PTID(s) tracked in ADO."
    )
    lines += [""]

lines += ["---", ""]

# ── Actionable Recommendations ────────────────────────────────────────────────
lines += [
    "## 💡 Actionable Recommendations",
    "",
    "| Priority | Recommendation | Impact |",
    "|----------|---------------|--------|",
]

recs = []
for pat, count in pattern_count.most_common():
    if "unused imports" in pat or "zero-warnings" in pat or "Zero-warnings" in pat:
        recs.append(("🔴 High", "Add a pre-commit hook to catch unused imports before push. Enable warnings-as-errors in IDE to match CI.", "Eliminates most compiler warning failures"))
    elif "AssociationNotFoundException" in pat or "404 REST" in pat:
        recs.append(("🔴 High", "Pre-seed required API associations in CI bootstrap scripts. Add environment readiness gate before test phase.", "Eliminates environment-dependent test failures"))
    elif "Unit/Integration test" in pat:
        recs.append(("🔴 High", "Review failing tests individually. Quarantine confirmed flaky tests with @Ignore + JIRA ticket. Fix genuine failures.", "Reduces failure rate directly"))
    elif "Selenium" in pat or "UI test" in pat:
        recs.append(("🟡 Medium", "Fix z-index on overlapping elements. Use ExpectedConditions.elementToBeClickable() instead of implicit waits.", "Stabilizes UI test suite"))
    elif "WDIO" in pat or "E2E" in pat:
        recs.append(("🔴 High", "Investigate WDIO E2E failures. Check for env-dependent flake (localStorage, ActiveMQ containers). Review quarantine process for reverted tests.", "Stabilizes E2E test pipeline"))
    elif "Jenkinsfile" in pat or "Groovy" in pat:
        recs.append(("🔴 High", "Fix Groovy pipeline errors in Jenkinsfile (MissingPropertyException). Test pipeline changes in a sandbox branch before merging.", "Prevents pipeline-level failures"))
    elif "WebDriver" in pat or "Browser" in pat:
        recs.append(("🟡 Medium", "Fix WebDriver errors: localStorage access denied may indicate same-origin issues; ensure test URLs match app domain.", "Reduces browser-level test flake"))
    elif "Dependency" in pat:
        recs.append(("🟡 Medium", "Pin dependency versions. Use Gradle dependency locking. Add a dependency audit step to the pipeline.", "Prevents random resolution failures"))
    elif "curl" in pat or "REST call" in pat:
        recs.append(("🟡 Medium", "Wrap REST calls with --retry 3. Add pre-flight service health checks at pipeline start.", "Reduces transient network failures"))
    elif "OutOfMemory" in pat or "OOM" in pat:
        recs.append(("🔴 High", "Increase JVM heap or pod memory limits. Profile memory with GC logging. Reduce parallel test workers.", "Prevents OOM crashes"))
    elif "Connection" in pat or "timeout" in pat:
        recs.append(("🟡 Medium", "Add service readiness wait step. Check infra health dashboards. Consider circuit breaker pattern.", "Reduces infra-flakiness"))

if flaky_candidates:
    recs.append(("🟡 Medium", f"Tag {len(flaky_candidates)} flaky test(s) with @Disabled + JIRA tickets. Track in a dedicated 'Flaky Tests' dashboard.", "Decouples flaky tests from build health"))

if total_quarantine_reversions:
    recs.append(("🔴 High", f"{total_quarantine_reversions} quarantine reversion(s) detected — tests marked 'fixed' are failing again. Tighten exit criteria before removing tests from quarantine.", "Prevents re-introducing known flaky tests"))

if all_ptids:
    recs.append(("🟡 Medium", f"Track {len(all_ptids)} active PTID(s) in ADO: {', '.join(str(p) for p in sorted(all_ptids)[:5])}{'...' if len(all_ptids) > 5 else ''}. Ensure each has an owner and target resolution date.", "Systematic flaky test tracking"))

if not_built_count > total * 0.2:
    recs.append(("🟡 Medium", f"High NOT_BUILT rate ({round(not_built_count / total * 100)}%). Add concurrent build prevention (e.g., `disableConcurrentBuilds()` in Jenkinsfile) to reduce wasted builds.", "Reduces wasted CI resources"))

if unstable_rate > 20:
    recs.append(("🔴 High", "Treat UNSTABLE builds as failures in branch protection rules. Require green CI before merge.", "Enforces quality gate"))

# DORA-based recommendations
if avg_mttr_hours > 24:
    recs.append(("🔴 High", f"MTTR is {avg_mttr_hours:.0f}h. Set up CI failure Slack/Teams alerts. Assign on-call rotation for build fixes.", "Reduces recovery time"))
if builds_per_day < 1:
    recs.append(("🟡 Medium", "Build throughput is below 1/day. Consider enabling auto-merge for green PRs to increase deployment frequency.", "Improves deployment cadence"))

seen = set()
for priority, rec, impact in recs:
    if rec not in seen:
        seen.add(rec)
        lines.append(f"| {priority} | {rec} | {impact} |")

lines += [
    "",
    "---",
    "",
    "## 🎯 Impact Assessment",
    "",
    "| Area | Status | Notes |",
    "|------|--------|-------|",
]

prod_status = "✅ No impact" if success_rate >= 70 else "⚠️ Monitor"
lines += [
    f"| 🏭 Production systems | {prod_status} | develop branch; not directly in prod |",
    f"| 🚀 Release pipeline | {'⚠️ At risk' if failure_rate > 10 else '✅ Not blocked'} | {failure_count} failures in last {total} builds |",
    f"| 👥 Developer velocity | {'⚠️ Impacted' if (failure_rate + unstable_rate) > 25 else '✅ OK'} | {failure_rate + unstable_rate}% builds require rework |",
    f"| 🎲 Test reliability | {'🔴 Low' if unstable_rate > 20 else '⚠️ Medium' if unstable_rate > 10 else '✅ Good'} | {unstable_rate}% unstable rate |",
    f"| 📐 DORA level | {dora_label} | MTTR: {avg_mttr_hours:.1f}h, CFR: {failure_rate + unstable_rate}%, Throughput: {builds_per_day}/day |",
    "",
    "---",
    "",
    '<div align="center">',
    "",
    f"*🤖 Auto-generated by jenkins-build-analyzer v2 &nbsp;|&nbsp; {now}*",
    "",
    "</div>",
]

report = "\n".join(lines)

if OUTPUT_PATH:
    with open(OUTPUT_PATH, "w") as f:
        f.write(report)
    print(f"\nReport saved to: {OUTPUT_PATH}", file=sys.stderr)
else:
    print(report)
