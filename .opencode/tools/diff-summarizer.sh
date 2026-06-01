#!/bin/bash
# Diff Summarizer Tool
# Takes a git diff and produces a structured change summary.
#
# Usage: ./diff-summarizer.sh <repo_path> [base_ref]
#   repo_path: Absolute path to the repository
#   base_ref:  Optional git ref to diff against (default: HEAD)
#
# Output: JSON with structured diff summary
#
# Examples:
#   ./diff-summarizer.sh /path/to/learn          # unstaged + staged changes
#   ./diff-summarizer.sh /path/to/learn HEAD~3   # last 3 commits

set -uo pipefail

REPO_PATH="${1:?Error: repo_path required}"
BASE_REF="${2:-}"

if [ ! -d "$REPO_PATH/.git" ]; then
  echo '{"success": false, "error": "Not a git repository: '"$REPO_PATH"'"}'
  exit 1
fi

cd "$REPO_PATH"

# Determine diff command
if [ -z "$BASE_REF" ]; then
  # Show all changes (staged + unstaged)
  DIFF_OUTPUT=$(git diff HEAD 2>/dev/null || git diff 2>/dev/null)
  DIFF_STAT=$(git diff HEAD --stat 2>/dev/null || git diff --stat 2>/dev/null)
  DIFF_COMMAND="git diff HEAD"
else
  DIFF_OUTPUT=$(git diff "$BASE_REF" 2>&1)
  DIFF_STAT=$(git diff "$BASE_REF" --stat 2>&1)
  DIFF_COMMAND="git diff $BASE_REF"
fi

# Count files changed
FILES_CHANGED=$(echo "$DIFF_STAT" | grep -c " |" || echo "0")

# Count insertions and deletions
INSERTIONS=$(echo "$DIFF_STAT" | tail -1 | grep -oP '(\d+) insertion' | grep -oP '\d+' || echo "0")
DELETIONS=$(echo "$DIFF_STAT" | tail -1 | grep -oP '(\d+) deletion' | grep -oP '\d+' || echo "0")

# List changed files with their change type
FILES_LIST=$(git diff ${BASE_REF:-HEAD} --name-status 2>/dev/null | while IFS=$'\t' read -r status file; do
  case "$status" in
    A) type="added" ;;
    M) type="modified" ;;
    D) type="deleted" ;;
    R*) type="renamed" ;;
    C*) type="copied" ;;
    *) type="unknown" ;;
  esac
  echo "    {\"file\": \"$file\", \"type\": \"$type\"}"
done | paste -sd ',' -)

# Categorize by type (source, test, config, other)
SOURCE_FILES=$(git diff ${BASE_REF:-HEAD} --name-only 2>/dev/null | grep -v -E '(test|Test|spec|Spec|__tests__)' | grep -v -E '\.(json|yaml|yml|xml|properties|config|md)$' | wc -l | tr -d ' ')
TEST_FILES=$(git diff ${BASE_REF:-HEAD} --name-only 2>/dev/null | grep -E '(test|Test|spec|Spec|__tests__)' | wc -l | tr -d ' ')
CONFIG_FILES=$(git diff ${BASE_REF:-HEAD} --name-only 2>/dev/null | grep -E '\.(json|yaml|yml|xml|properties|config)$' | wc -l | tr -d ' ')

# Escape diff stat for JSON
ESCAPED_STAT=$(printf '%s' "$DIFF_STAT" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))' 2>/dev/null || echo '""')

# Get a brief diff (first 3000 chars for context)
BRIEF_DIFF=$(echo "$DIFF_OUTPUT" | head -c 3000)
ESCAPED_DIFF=$(printf '%s' "$BRIEF_DIFF" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))' 2>/dev/null || echo '""')

cat <<EOF
{
  "success": true,
  "command": "$DIFF_COMMAND",
  "summary": {
    "files_changed": ${FILES_CHANGED:-0},
    "insertions": ${INSERTIONS:-0},
    "deletions": ${DELETIONS:-0},
    "net_lines": $((${INSERTIONS:-0} - ${DELETIONS:-0}))
  },
  "categories": {
    "source_files": ${SOURCE_FILES:-0},
    "test_files": ${TEST_FILES:-0},
    "config_files": ${CONFIG_FILES:-0}
  },
  "files": [${FILES_LIST:-}],
  "stat": $ESCAPED_STAT,
  "diff_preview": $ESCAPED_DIFF
}
EOF
