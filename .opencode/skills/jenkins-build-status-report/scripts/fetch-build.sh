#!/usr/bin/env bash
# fetch-build.sh — Fetch Jenkins build data via REST API
# Usage: ./fetch-build.sh <jenkins-job-url>
#
# Environment variables:
#   JENKINS_USER  — Jenkins username (optional, reads from env)
#   JENKINS_TOKEN — Jenkins API token (optional, reads from env)
#   CONSOLE_TAIL  — Number of console log lines to fetch (default: 200)
#
# Outputs JSON to stdout with build metadata, changesets, and console excerpt.

set -euo pipefail

# ── Args ─────────────────────────────────────────────────────────────────────
if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <jenkins-job-url>" >&2
  exit 1
fi

JOB_URL="${1%/}"
CONSOLE_TAIL="${CONSOLE_TAIL:-200}"

# ── Auth ─────────────────────────────────────────────────────────────────────
CURL_OPTS=(-sfS --connect-timeout 15 --max-time 30)

if [[ -n "${JENKINS_USER:-}" && -n "${JENKINS_TOKEN:-}" ]]; then
  CURL_OPTS+=(-u "${JENKINS_USER}:${JENKINS_TOKEN}")
elif [[ -n "${JENKINS_TOKEN:-}" ]]; then
  CURL_OPTS+=(-u ":${JENKINS_TOKEN}")
fi

# ── Fetch build overview ─────────────────────────────────────────────────────
API_TREE="lastBuild[number,result,url,timestamp,duration,changeSet[items[commitId,msg,author[fullName]]]],lastFailedBuild[number,url,timestamp],lastSuccessfulBuild[number,url,timestamp],lastUnstableBuild[number,url,timestamp]"

echo "Fetching build overview from ${JOB_URL}..." >&2
BUILD_JSON=$(curl "${CURL_OPTS[@]}" "${JOB_URL}/api/json?tree=${API_TREE}")

if [[ -z "${BUILD_JSON}" ]]; then
  echo "Error: Empty response from Jenkins API. Check URL and credentials." >&2
  exit 1
fi

# ── Extract key build numbers ────────────────────────────────────────────────
LAST_BUILD_NUM=$(echo "${BUILD_JSON}" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('lastBuild',{}).get('number',''))" 2>/dev/null || echo "")
LAST_BUILD_RESULT=$(echo "${BUILD_JSON}" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('lastBuild',{}).get('result','UNKNOWN'))" 2>/dev/null || echo "UNKNOWN")
LAST_FAILED_NUM=$(echo "${BUILD_JSON}" | python3 -c "import sys,json; d=json.load(sys.stdin); b=d.get('lastFailedBuild') or {}; print(b.get('number',''))" 2>/dev/null || echo "")
LAST_SUCCESS_NUM=$(echo "${BUILD_JSON}" | python3 -c "import sys,json; d=json.load(sys.stdin); b=d.get('lastSuccessfulBuild') or {}; print(b.get('number',''))" 2>/dev/null || echo "")

echo "Last build: #${LAST_BUILD_NUM} (${LAST_BUILD_RESULT})" >&2
echo "Last failed: #${LAST_FAILED_NUM:-none}" >&2
echo "Last success: #${LAST_SUCCESS_NUM:-none}" >&2

# ── Fetch console log of failed build (last N lines) ────────────────────────
CONSOLE_EXCERPT=""
FAILED_CONSOLE_BUILD=""

if [[ -n "${LAST_FAILED_NUM}" ]]; then
  FAILED_CONSOLE_BUILD="${LAST_FAILED_NUM}"
elif [[ "${LAST_BUILD_RESULT}" != "SUCCESS" && -n "${LAST_BUILD_NUM}" ]]; then
  FAILED_CONSOLE_BUILD="${LAST_BUILD_NUM}"
fi

if [[ -n "${FAILED_CONSOLE_BUILD}" ]]; then
  echo "Fetching console log for build #${FAILED_CONSOLE_BUILD} (last ${CONSOLE_TAIL} lines)..." >&2
  FULL_CONSOLE=$(curl "${CURL_OPTS[@]}" "${JOB_URL}/${FAILED_CONSOLE_BUILD}/consoleText" 2>/dev/null || echo "")
  if [[ -n "${FULL_CONSOLE}" ]]; then
    CONSOLE_EXCERPT=$(echo "${FULL_CONSOLE}" | tail -n "${CONSOLE_TAIL}")
  fi
fi

# ── Fetch recent build history (last 5) ─────────────────────────────────────
echo "Fetching recent build history..." >&2
HISTORY_TREE="builds[number,result,timestamp,changeSet[items[commitId,msg,author[fullName]]]]{0,5}"
HISTORY_JSON=$(curl "${CURL_OPTS[@]}" "${JOB_URL}/api/json?tree=${HISTORY_TREE}" 2>/dev/null || echo '{"builds":[]}')

# ── Fetch commits between failed and success builds (fix candidates) ────────
FIX_COMMITS="[]"
if [[ -n "${LAST_FAILED_NUM}" && -n "${LAST_SUCCESS_NUM}" ]]; then
  if [[ "${LAST_SUCCESS_NUM}" -gt "${LAST_FAILED_NUM}" ]]; then
    echo "Fetching fix candidates (builds #$((LAST_FAILED_NUM + 1)) to #${LAST_SUCCESS_NUM})..." >&2
    FIX_COMMITS=$(python3 -c "
import urllib.request, base64, json, sys

job_url = '${JOB_URL}'
start = ${LAST_FAILED_NUM} + 1
end = ${LAST_SUCCESS_NUM}
user = '${JENKINS_USER:-}'
token = '${JENKINS_TOKEN:-}'

auth_raw = (user + ':' + token) if user else (':' + token)
auth = base64.b64encode(auth_raw.encode()).decode()
headers = {'Authorization': 'Basic ' + auth} if (user or token) else {}

commits = []
for num in range(start, end + 1):
    url = f'{job_url}/{num}/api/json?tree=changeSet[items[commitId,msg,author[fullName]]]'
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=15) as r:
            data = json.loads(r.read())
            for item in (data.get('changeSet', {}) or {}).get('items', []):
                commits.append({
                    'build': num,
                    'commitId': item.get('commitId', '')[:8],
                    'msg': item.get('msg', '').split('\n')[0],
                    'author': item.get('author', {}).get('fullName', 'unknown')
                })
    except Exception:
        pass

print(json.dumps(commits))
" 2>/dev/null || echo "[]")
  fi
fi

# ── Assemble final JSON output ──────────────────────────────────────────────
python3 -c "
import json, sys

build_json = json.loads('''${BUILD_JSON}''')
history_json = json.loads('''${HISTORY_JSON}''')
fix_commits = json.loads('''${FIX_COMMITS}''')
console_excerpt = sys.stdin.read()

branch = '${JOB_URL}'.split('/')[-1].replace('%252F', '/').replace('%2F', '/')

output = {
    'job_url': '${JOB_URL}',
    'branch': branch,
    'last_build': build_json.get('lastBuild'),
    'last_failed_build': build_json.get('lastFailedBuild'),
    'last_successful_build': build_json.get('lastSuccessfulBuild'),
    'last_unstable_build': build_json.get('lastUnstableBuild'),
    'console_excerpt': console_excerpt if console_excerpt.strip() else None,
    'console_excerpt_build': '${FAILED_CONSOLE_BUILD}' if '${FAILED_CONSOLE_BUILD}' else None,
    'recent_builds': history_json.get('builds', []),
    'fix_candidates': fix_commits
}

print(json.dumps(output, indent=2))
" <<< "${CONSOLE_EXCERPT}"
