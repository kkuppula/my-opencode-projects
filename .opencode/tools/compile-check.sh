#!/bin/bash
# Compile Check Tool
# Runs compilation for the target repo and returns structured results.
#
# Usage: ./compile-check.sh <repo_path> <stack>
#   repo_path: Absolute path to the repository
#   stack:     "java" or "typescript"
#
# Output: JSON with compilation result
#
# Examples:
#   ./compile-check.sh /path/to/learn java
#   ./compile-check.sh /path/to/ultra typescript

set -uo pipefail

REPO_PATH="${1:?Error: repo_path required}"
STACK="${2:?Error: stack required (java|typescript)}"

# Validate repo exists
if [ ! -d "$REPO_PATH" ]; then
  echo '{"success": false, "error": "Repository path does not exist: '"$REPO_PATH"'", "command": "N/A", "output": ""}'
  exit 1
fi

START_TIME=$(date +%s)

case "$STACK" in
  java|Java|java/gradle|Java/Gradle)
    # Try Gradle compile
    if [ -f "$REPO_PATH/gradlew" ]; then
      COMMAND="./gradlew compileJava"
      OUTPUT=$(cd "$REPO_PATH" && ./gradlew compileJava 2>&1)
      EXIT_CODE=$?
    elif [ -f "$REPO_PATH/mvnw" ]; then
      COMMAND="./mvnw compile"
      OUTPUT=$(cd "$REPO_PATH" && ./mvnw compile 2>&1)
      EXIT_CODE=$?
    else
      echo '{"success": false, "error": "No gradlew or mvnw found", "command": "N/A", "output": ""}'
      exit 1
    fi
    ;;
  typescript|TypeScript|ts|typescript/angular|TypeScript/Angular/Nx)
    COMMAND="npx tsc --noEmit"
    OUTPUT=$(cd "$REPO_PATH" && npx tsc --noEmit 2>&1)
    EXIT_CODE=$?
    ;;
  *)
    echo '{"success": false, "error": "Unknown stack: '"$STACK"'. Use java or typescript.", "command": "N/A", "output": ""}'
    exit 1
    ;;
esac

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Extract error count if compilation failed
ERROR_COUNT=0
if [ $EXIT_CODE -ne 0 ]; then
  case "$STACK" in
    java|Java|java/gradle|Java/Gradle)
      ERROR_COUNT=$(echo "$OUTPUT" | grep -c "error:" || echo "0")
      ;;
    typescript|TypeScript|ts|typescript/angular|TypeScript/Angular/Nx)
      ERROR_COUNT=$(echo "$OUTPUT" | grep -c "error TS" || echo "0")
      ;;
  esac
fi

# Truncate output for JSON safety (max 5000 chars)
TRUNCATED_OUTPUT=$(echo "$OUTPUT" | tail -100 | head -c 5000)

# Escape for JSON
ESCAPED_OUTPUT=$(printf '%s' "$TRUNCATED_OUTPUT" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))' 2>/dev/null || echo '""')

if [ $EXIT_CODE -eq 0 ]; then
  SUCCESS="true"
  STATUS="pass"
else
  SUCCESS="false"
  STATUS="fail"
fi

cat <<EOF
{
  "success": $SUCCESS,
  "status": "$STATUS",
  "command": "$COMMAND",
  "exit_code": $EXIT_CODE,
  "error_count": $ERROR_COUNT,
  "duration_seconds": $DURATION,
  "stack": "$STACK",
  "repo_path": "$REPO_PATH",
  "output": $ESCAPED_OUTPUT
}
EOF
