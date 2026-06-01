#!/usr/bin/env bash
# fetch-iterations.sh — Fetch sprint/iteration data from Azure DevOps.
#
# Usage:
#   fetch-iterations.sh [--team <team-name>] [--timeframe past|current|future] [--top N]
#
# Output: JSON array of iterations with name, path, startDate, finishDate, timeFrame.

set -euo pipefail

TEAM=""
TIMEFRAME=""
TOP=10

while [[ $# -gt 0 ]]; do
  case "$1" in
    --team) TEAM="$2"; shift 2 ;;
    --timeframe) TIMEFRAME="$2"; shift 2 ;;
    --top) TOP="$2"; shift 2 ;;
    *) shift ;;
  esac
done

# Configure via environment variables or edit these defaults
ORG="${ADO_ORG:-your-organization}"
PROJECT="${ADO_PROJECT:-your-project}"
API_VERSION="7.0"

if [[ -z "${ADO_PAT:-}" ]]; then
  source ~/.zprofile 2>/dev/null || true
fi
if [[ -z "${ADO_PAT:-}" ]]; then
  echo '{"error": "ADO_PAT not set."}' >&2
  exit 1
fi

AUTH_HEADER="Authorization: Basic $(printf ":%s" "${ADO_PAT}" | base64)"

if [[ -n "${TEAM}" ]]; then
  URL="https://dev.azure.com/${ORG}/${PROJECT}/${TEAM}/_apis/work/teamsettings/iterations?api-version=${API_VERSION}"
else
  URL="https://dev.azure.com/${ORG}/${PROJECT}/_apis/work/teamsettings/iterations?api-version=${API_VERSION}"
fi

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -H "${AUTH_HEADER}" \
  "${URL}" \
  2>/dev/null)

HTTP_CODE=$(echo "${RESPONSE}" | tail -1)
BODY=$(echo "${RESPONSE}" | sed '$d')

if [[ "${HTTP_CODE}" != "200" ]]; then
  echo "${BODY}" >&2
  exit 1
fi

TIMEFRAME_ARG="${TIMEFRAME}" TOP_ARG="${TOP}" python3 -c "
import json, sys, os

data = json.loads(sys.argv[1])
iterations = data.get('value', [])

timeframe = os.environ.get('TIMEFRAME_ARG', '')
top_n = int(os.environ.get('TOP_ARG', '10'))

if timeframe:
    iterations = [i for i in iterations if i.get('attributes', {}).get('timeFrame', '').lower() == timeframe.lower()]

iterations = iterations[-top_n:]

result = []
for it in iterations:
    attrs = it.get('attributes', {})
    result.append({
        'name': it.get('name', ''),
        'path': it.get('path', ''),
        'startDate': attrs.get('startDate', ''),
        'finishDate': attrs.get('finishDate', ''),
        'timeFrame': attrs.get('timeFrame', '')
    })

print(json.dumps(result, indent=2))
" "${BODY}"
