#!/bin/bash
# Teams Pipeline Notification Script
# Sends an Adaptive Card to a Teams channel via Incoming Webhook
#
# Usage: ./notify-teams.sh <stage> <status> <summary> [report_file]
#   stage:       Pipeline | Routing | Dispatching | Discovery | Implementation | Testing | Verification | Final
#   status:      started | routing | dispatching | success | failure | warning
#   summary:     Brief summary text (key findings)
#   report_file: Optional path to the full report markdown file
#
# Environment:
#   TEAMS_WEBHOOK_URL - Optional. Overrides the hardcoded default webhook URL.
#
# Notification lifecycle:
#   Pipeline (started)      → Requirement received, pipeline kicked off
#   Routing (routing)       → Repo(s) identified and routing decision made
#   Dispatching (dispatching) → Sub-agent being launched
#   Discovery (success)     → Discovery agent completed
#   Implementation (success) → Implementation agent completed
#   Testing (success)       → Test agent completed
#   Verification (success)  → Verification agent completed
#   Final (success)         → Composite report assembled, feature delivered
#
# Example:
#   ./notify-teams.sh "Routing" "routing" "Routed to Backend (Learn) — matched: REST, service"
#   ./notify-teams.sh "Discovery" "success" "Found 3 files to modify" reports/01-discovery-report.md

set -euo pipefail

STAGE="${1:?Error: stage required (Discovery|Implementation|Testing|Verification|Final)}"
STATUS="${2:?Error: status required (success|failure|warning)}"
SUMMARY="${3:?Error: summary text required}"
REPORT_FILE="${4:-}"

# Webhook URL: use environment variable if set, otherwise fall back to hardcoded default
TEAMS_WEBHOOK_URL="${TEAMS_WEBHOOK_URL:-https://anthologyinc.webhook.office.com/webhookb2/1523d9c3-603b-42c0-a06d-6fa106a303d5@75853e87-aaca-4625-a00a-5a7ca2fe1a72/IncomingWebhook/41d2320704664ab99dc1ce9ad2ddc0c1/486a175b-53ce-43d6-99fe-e94165848e0c/V2WACdGE_qDPUtunuPsltwWqABC-3qXOJB4daTYrgc7041}"

# Silently skip if webhook URL is explicitly empty (don't block the pipeline)
if [ -z "${TEAMS_WEBHOOK_URL:-}" ]; then
  exit 0
fi

# Status emoji and color
case "$STATUS" in
  success)     ICON="✅"; COLOR="Good" ;;
  failure)     ICON="❌"; COLOR="Attention" ;;
  warning)     ICON="⚠️"; COLOR="Warning" ;;
  routing)     ICON="🧭"; COLOR="Accent" ;;
  dispatching) ICON="🚀"; COLOR="Accent" ;;
  started)     ICON="▶️"; COLOR="Default" ;;
  *)           ICON="ℹ️"; COLOR="Default" ;;
esac

# Extract report file content and highlight risks/open questions
REPORT_FILENAME=""
RISKS_CONTENT=""
FULL_REPORT=""
if [ -n "$REPORT_FILE" ] && [ -f "$REPORT_FILE" ]; then
  REPORT_FILENAME=$(basename "$REPORT_FILE")

  # Read full report content (cap at ~15KB to stay within Adaptive Card payload limit of ~28KB)
  FULL_REPORT=$(head -c 15000 "$REPORT_FILE")
  if [ "$(wc -c < "$REPORT_FILE")" -gt 15000 ]; then
    FULL_REPORT="${FULL_REPORT}

---
*[Truncated — full report: ${REPORT_FILE}]*"
  fi

  # Extract sections matching: Open Questions, Risks, Known Risks, Risk Assessment, Concerns
  # Uses awk to capture content between matching headers and the next header (##)
  RISKS_CONTENT=$(awk '
    /^##.*([Rr]isk|[Oo]pen.[Qq]uestion|[Cc]oncern|[Uu]nresolved|[Bb]locker)/ { capture=1; print; next }
    /^## / { capture=0 }
    capture { print }
  ' "$REPORT_FILE" | head -c 3000 || true)

  # If no dedicated sections found, look for inline risk/question markers (⚠️, ❓, RISK:, Q:)
  if [ -z "$RISKS_CONTENT" ]; then
    RISKS_CONTENT=$(grep -E '(⚠️|❓|RISK:|WARNING:|OPEN:|Q:|^\- \[.\] .*(risk|question|blocker|concern))' "$REPORT_FILE" | head -30 || true)
  fi

  # If still nothing found, report is clean
  if [ -z "$RISKS_CONTENT" ]; then
    RISKS_CONTENT="No open questions or risks identified."
  fi
fi

# Escape special characters for JSON
escape_json() {
  printf '%s' "$1" | python3 -c 'import json,sys; print(json.dumps(sys.stdin.read()))'
}

SUMMARY_ESCAPED=$(escape_json "$SUMMARY")
RISKS_ESCAPED=$(escape_json "$RISKS_CONTENT")
REPORT_ESCAPED=$(escape_json "$FULL_REPORT")
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Build Adaptive Card payload
PAYLOAD=$(cat <<EOF
{
  "type": "message",
  "attachments": [
    {
      "contentType": "application/vnd.microsoft.card.adaptive",
      "contentUrl": null,
      "content": {
        "\$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
        "type": "AdaptiveCard",
        "version": "1.4",
        "body": [
          {
            "type": "Container",
            "style": "${COLOR}",
            "items": [
              {
                "type": "TextBlock",
                "text": "${ICON} Pipeline Stage: **${STAGE}**",
                "weight": "Bolder",
                "size": "Medium",
                "wrap": true
              }
            ]
          },
          {
            "type": "FactSet",
            "facts": [
              { "title": "Status", "value": "${STATUS}" },
              { "title": "Stage", "value": "${STAGE}" },
              { "title": "Report File", "value": "${REPORT_FILENAME:-N/A}" },
              { "title": "Timestamp", "value": "${TIMESTAMP}" }
            ]
          },
          {
            "type": "TextBlock",
            "text": "**Summary**",
            "weight": "Bolder",
            "spacing": "Medium",
            "wrap": true
          },
          {
            "type": "TextBlock",
            "text": ${SUMMARY_ESCAPED},
            "wrap": true
          }
EOF
)

# Add risks/open questions section if report was provided
if [ -n "$RISKS_CONTENT" ]; then
  PAYLOAD="${PAYLOAD},"
  PAYLOAD="${PAYLOAD}
          {
            \"type\": \"TextBlock\",
            \"text\": \"**⚠️ Open Questions & Risks**\",
            \"weight\": \"Bolder\",
            \"spacing\": \"Medium\",
            \"wrap\": true,
            \"color\": \"Warning\"
          },
          {
            \"type\": \"TextBlock\",
            \"text\": ${RISKS_ESCAPED},
            \"wrap\": true,
            \"fontType\": \"Monospace\",
            \"size\": \"Small\"
          }"
fi

# Add full report content in a collapsible-style section
if [ -n "$FULL_REPORT" ]; then
  PAYLOAD="${PAYLOAD},"
  PAYLOAD="${PAYLOAD}
          {
            \"type\": \"TextBlock\",
            \"text\": \"**📄 Full Report: ${REPORT_FILENAME}**\",
            \"weight\": \"Bolder\",
            \"spacing\": \"Large\",
            \"wrap\": true,
            \"separator\": true
          },
          {
            \"type\": \"TextBlock\",
            \"text\": ${REPORT_ESCAPED},
            \"wrap\": true,
            \"fontType\": \"Monospace\",
            \"size\": \"Small\"
          }"
fi

# Close the card
PAYLOAD="${PAYLOAD}
        ]
      }
    }
  ]
}
"

# Send to Teams
HTTP_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD" \
  "$TEAMS_WEBHOOK_URL")

if [ "$HTTP_RESPONSE" = "200" ] || [ "$HTTP_RESPONSE" = "202" ]; then
  echo "✅ Teams notification sent: ${STAGE} (${STATUS})"
else
  echo "❌ Failed to send Teams notification. HTTP status: ${HTTP_RESPONSE}"
  exit 1
fi
