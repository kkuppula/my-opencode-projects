#!/bin/bash
# Test Runner Tool
# Runs tests for a specific class/module and returns structured results.
#
# Usage: ./test-runner.sh <repo_path> <stack> <test_target>
#   repo_path:   Absolute path to the repository
#   stack:       "java" or "typescript"
#   test_target: Class name, file path, or module to test
#
# Output: JSON with test execution results
#
# Examples:
#   ./test-runner.sh /path/to/learn java "CourseSettingsControllerTest"
#   ./test-runner.sh /path/to/learn java "**/CourseSettingsControllerTest"
#   ./test-runner.sh /path/to/ultra typescript "libs/shared/src/lib/my-component"

set -uo pipefail

REPO_PATH="${1:?Error: repo_path required}"
STACK="${2:?Error: stack required (java|typescript)}"
TEST_TARGET="${3:?Error: test_target required (class name or path)}"

if [ ! -d "$REPO_PATH" ]; then
  echo '{"success": false, "error": "Repository path does not exist", "tests_run": 0, "tests_passed": 0, "tests_failed": 0}'
  exit 1
fi

START_TIME=$(date +%s)

case "$STACK" in
  java|Java|java/gradle|Java/Gradle)
    if [ -f "$REPO_PATH/gradlew" ]; then
      COMMAND="./gradlew test --tests \"$TEST_TARGET\""
      OUTPUT=$(cd "$REPO_PATH" && ./gradlew test --tests "$TEST_TARGET" 2>&1)
      EXIT_CODE=$?
    else
      echo '{"success": false, "error": "No gradlew found", "tests_run": 0, "tests_passed": 0, "tests_failed": 0}'
      exit 1
    fi

    # Parse Gradle test output
    TESTS_RUN=$(echo "$OUTPUT" | grep -oP '(\d+) tests?' | head -1 | grep -oP '\d+' || echo "0")
    TESTS_FAILED=$(echo "$OUTPUT" | grep -oP '(\d+) failed' | grep -oP '\d+' || echo "0")
    TESTS_SKIPPED=$(echo "$OUTPUT" | grep -oP '(\d+) skipped' | grep -oP '\d+' || echo "0")
    TESTS_PASSED=$((TESTS_RUN - TESTS_FAILED - TESTS_SKIPPED))
    ;;

  typescript|TypeScript|ts|typescript/angular|TypeScript/Angular/Nx)
    # Try Jest first, then Karma
    if [ -f "$REPO_PATH/jest.config.ts" ] || [ -f "$REPO_PATH/jest.config.js" ]; then
      COMMAND="npx jest --testPathPattern=\"$TEST_TARGET\" --json"
      OUTPUT=$(cd "$REPO_PATH" && npx jest --testPathPattern="$TEST_TARGET" 2>&1)
      EXIT_CODE=$?
    elif command -v nx &> /dev/null || [ -f "$REPO_PATH/nx.json" ]; then
      COMMAND="npx nx test --testPathPattern=\"$TEST_TARGET\""
      OUTPUT=$(cd "$REPO_PATH" && npx nx test --testPathPattern="$TEST_TARGET" 2>&1)
      EXIT_CODE=$?
    else
      COMMAND="npx jest \"$TEST_TARGET\""
      OUTPUT=$(cd "$REPO_PATH" && npx jest "$TEST_TARGET" 2>&1)
      EXIT_CODE=$?
    fi

    TESTS_RUN=$(echo "$OUTPUT" | grep -oP 'Tests:\s+.*?(\d+) total' | grep -oP '\d+(?= total)' || echo "0")
    TESTS_FAILED=$(echo "$OUTPUT" | grep -oP '(\d+) failed' | grep -oP '\d+' || echo "0")
    TESTS_SKIPPED=$(echo "$OUTPUT" | grep -oP '(\d+) skipped' | grep -oP '\d+' || echo "0")
    TESTS_PASSED=$(echo "$OUTPUT" | grep -oP '(\d+) passed' | grep -oP '\d+' || echo "0")
    ;;

  *)
    echo '{"success": false, "error": "Unknown stack: '"$STACK"'", "tests_run": 0, "tests_passed": 0, "tests_failed": 0}'
    exit 1
    ;;
esac

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Truncate output
TRUNCATED_OUTPUT=$(echo "$OUTPUT" | tail -80 | head -c 4000)
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
  "test_target": "$TEST_TARGET",
  "tests_run": ${TESTS_RUN:-0},
  "tests_passed": ${TESTS_PASSED:-0},
  "tests_failed": ${TESTS_FAILED:-0},
  "tests_skipped": ${TESTS_SKIPPED:-0},
  "duration_seconds": $DURATION,
  "stack": "$STACK",
  "repo_path": "$REPO_PATH",
  "output": $ESCAPED_OUTPUT
}
EOF
