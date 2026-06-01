#!/usr/bin/env bash
# query-workitems.sh — Execute a WIQL query against Azure DevOps and return full work item details as JSON.
#
# Usage:
#   query-workitems.sh "<WIQL query>"
#   query-workitems.sh "<WIQL query>" --top 50
#
# Environment:
#   ADO_PAT   — Personal Access Token (Work Items Read scope)
#
# Output: JSON array of work item objects with all requested fields.

set -euo pipefail

WIQL="${1:?Usage: query-workitems.sh \"<WIQL query>\" [--top N]}"
shift

TOP=200
while [[ $# -gt 0 ]]; do
  case "$1" in
    --top) TOP="$2"; shift 2 ;;
    *) shift ;;
  esac
done

# Configure via environment variables or edit these defaults
ORG="${ADO_ORG:-your-organization}"
PROJECT="${ADO_PROJECT:-your-project}"
API_VERSION="7.0"
BASE_URL="https://dev.azure.com/${ORG}/${PROJECT}/_apis/wit"

if [[ -z "${ADO_PAT:-}" ]]; then
  source ~/.zprofile 2>/dev/null || true
fi
if [[ -z "${ADO_PAT:-}" ]]; then
  echo '{"error": "ADO_PAT not set. Export it or add to ~/.zprofile."}' >&2
  exit 1
fi

AUTH_HEADER="Authorization: Basic $(printf ":%s" "${ADO_PAT}" | base64)"

WIQL_JSON=$(python3 -c "import json,sys; print(json.dumps({'query': sys.argv[1]}))" "${WIQL}")

WIQL_RESPONSE=$(curl -s -w "\n%{http_code}" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  "${BASE_URL}/wiql?api-version=${API_VERSION}&\$top=${TOP}" \
  -d "${WIQL_JSON}" \
  2>/dev/null)

HTTP_CODE=$(echo "${WIQL_RESPONSE}" | tail -1)
BODY=$(echo "${WIQL_RESPONSE}" | sed '$d')

if [[ "${HTTP_CODE}" != "200" ]]; then
  echo "${BODY}" | python3 -c "
import sys, json
body = sys.stdin.read()
print(json.dumps({'error': 'WIQL query failed (HTTP ${HTTP_CODE})', 'details': body}))
" >&2
  exit 1
fi

IDS=$(echo "${BODY}" | python3 -c "
import json, sys
data = json.load(sys.stdin)
ids = [str(wi['id']) for wi in data.get('workItems', [])]
print(','.join(ids))
")

if [[ -z "${IDS}" ]]; then
  echo "[]"
  exit 0
fi

# ADO batch API: max 200 IDs per call; parse SELECT fields for $fields param
FIELDS=$(python3 -c "
import sys, re
wiql = sys.argv[1]
m = re.search(r'SELECT\s+(.*?)\s+FROM', wiql, re.IGNORECASE | re.DOTALL)
if m:
    fields = re.findall(r'\[([^\]]+)\]', m.group(1))
    base = ['System.Id', 'System.Title', 'System.State', 'System.WorkItemType']
    print(','.join(dict.fromkeys(base + fields)))
else:
    print('System.Id,System.Title,System.State,System.WorkItemType')
" "${WIQL}")

ITEMS_FILE=$(mktemp)
echo "[]" > "${ITEMS_FILE}"
trap "rm -f '${ITEMS_FILE}'" EXIT

while [[ -n "${IDS}" ]]; do
  BATCH=$(echo "${IDS}" | python3 -c "
import sys
ids = sys.stdin.read().strip().split(',')
print(','.join(ids[:200]))
")
  REMAINING=$(echo "${IDS}" | python3 -c "
import sys
ids = sys.stdin.read().strip().split(',')
print(','.join(ids[200:]))
")

  curl -s \
    -H "${AUTH_HEADER}" \
    "${BASE_URL}/workitems?ids=${BATCH}&fields=${FIELDS}&api-version=${API_VERSION}" \
    2>/dev/null | python3 -c "
import json, sys
existing = json.load(open(sys.argv[1]))
new_data = json.load(sys.stdin)
for item in new_data.get('value', []):
    flat = {'id': item['id'], 'url': item.get('url', '')}
    flat.update(item.get('fields', {}))
    existing.append(flat)
with open(sys.argv[1], 'w') as f:
    json.dump(existing, f)
" "${ITEMS_FILE}"

  IDS="${REMAINING}"
done

cat "${ITEMS_FILE}"
