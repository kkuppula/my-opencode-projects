# PR Review Dashboard

> A single-file, AI-powered pull request triage dashboard. No frameworks, no build step, no dependencies — serve with `python3 -m http.server` and open in your browser.

![Vanilla JS](https://img.shields.io/badge/vanilla-JS%2FCSS%2FHTML-f7df1e) ![Single File](https://img.shields.io/badge/single%20file-2800%20lines-blue) ![Zero Cost](https://img.shields.io/badge/AI-zero%20cost-green)

---

## What It Does

Pulls all PRs assigned to you for review from GitHub and organizes them into a **three-column Kanban board**:

| Inbox | Review Now | Review Later |
|-------|-----------|--------------|
| Untriaged PRs, sorted by AI priority | PRs you're actively reviewing | Deferred — come back to these |

Drag PRs between columns with one click. State persists in `localStorage`.

---

## Features

### 🧠 AI Priority Scoring (Zero Cost)

Every PR gets a **0–100 priority score** computed from:
- Age (older = higher urgency)
- Size (files changed, additions/deletions)
- Reviewer count (fewer reviewers = more urgent)
- Labels (bug/hotfix/security = boost)
- Draft status (drafts = deprioritized)

No AI tokens used — pure heuristic, instant.

### 💬 AI Summaries

Click "Summarize with AI" on any card to get a 2-3 sentence summary of what changed and what to focus on during review.

**Two providers, both free:**

| Provider | Model | Cost | Privacy |
|----------|-------|------|---------|
| **Local Ollama** (default) | `qwen3-vl:latest` | Free forever | 100% private, runs on your machine |
| **GitHub Models** (fallback) | `gpt-4o-mini` | Free tier (150 req/day) | Sent to Azure, auth via your PAT |

Switch between providers with the dropdown in the header.

### 💡 Smart Suggestions

Contextual advice on each card — e.g., "Large PR — consider reviewing in chunks" or "Stale PR — author may need a nudge."

### 🔍 PR Risk Score

AI-powered security and quality analysis per PR:

1. Fetches the PR diff from GitHub
2. Sends to your AI provider with a security-focused prompt
3. Returns: 🚨 **HIGH** / ⚠️ **MEDIUM** / ✅ **LOW** with specific reasons

Evaluates: security vulnerabilities, breaking API changes, missing error handling, test coverage gaps, performance concerns.

Results cached in `localStorage` — scan once, see forever.

### 📜 Live PR Timeline

Animated activity feed per PR showing:
- Commits, reviews, comments
- Label changes, assignments
- Force pushes, review requests

Fetches from GitHub's Timeline API with relative timestamps.

### 📄 Inline Diff Preview

Preview the diff directly on any PR card without leaving the dashboard. Shows colorized additions/deletions (truncated to 3000 chars for quick scanning). Full diff available via "Open in GitHub."

### 👥 Team Review Load

Horizontal bar chart showing how many PRs each author has in your review queue:

- 🟢 **1 PR** — low load
- 🟡 **2 PRs** — moderate
- 🟠 **3–4 PRs** — high
- 🔴 **5+ PRs** — critical

Useful for: identifying who's flooding your queue, prioritizing reviewers who rarely ask, and balancing your review time.

### 📋 My PRs

Switch to the "My PRs" tab to see all your own open pull requests — track what's waiting on reviewers without leaving the dashboard.

### 👥 Team PRs

Look up any team member's PRs — both authored and assigned for review. Searchable dropdown with all contributors from your review queue.

### ⏰ Stale PRs

Dedicated panel surfacing PRs open 7+ days with no activity in 3+ days. Includes nudge suggestions to ping authors or close abandoned PRs.

### 📦 Smart Batching

Groups PRs by repo + author so you can review related changes together. Shows total line count per batch for time estimation.

### 👻 Ghost Reviewers

Detects reviewers who were assigned 2+ days ago but haven't submitted a review. Helps identify bottlenecks in your review pipeline.

### 🏓 Ping-Pong PRs

Flags PRs with 3+ rounds of changes-requested/re-review cycles. Highlights PRs that may need a synchronous discussion instead of more async rounds.

### 🔗 PR Dependencies

Scans file lists across all PRs to find overlapping files — PRs touching the same files are potential merge conflicts. Shows file-level conflict map.

### 🏷️ Custom Tags

Add your own tags to any PR card (e.g., "needs-sync", "blocked", "quick-win"). Tags persist in `localStorage` and appear on cards. Click `+ tag` on any card to add.

### ☑️ Bulk Actions

Checkbox on every card for batch operations. Select multiple PRs, then move them all to Review Now, Later, or Inbox in one click. Select All / Clear shortcuts included.

### 🌙 Dark Mode

Full dark theme toggle. Persists across sessions. Press the moon/sun icon in the header.

### 🎯 Focus Mode

Hides filters, feature toggles, and heatmap — leaves only the Kanban board for distraction-free triage.

### 🎉 Queue Zero Celebration

Confetti animation when you clear both Inbox and Review Now. You've earned it.

### 📤 Export

Export your entire triage state (Inbox, Review Now, Later) as Markdown — includes PR titles, links, repos, authors, and custom tags. Copy to clipboard for standup notes or Slack.

### ✅ CI Status Badges

Each PR card shows CI pipeline status: ✓ CI (green), ✗ CI (red), ⏳ CI (pending).

### 🔄 Auto-Refresh

Toggle auto-refresh to re-fetch PRs every 5 minutes without manual intervention.

### 🎤 Voice Control

Hands-free triage via browser Speech Recognition. Press `v` or click the mic button.

| Command | Action |
|---------|--------|
| "refresh" / "reload" | Reload all PRs |
| "show critical" | Filter to Critical priority |
| "show high" | Filter to High priority |
| "my changes" / "I requested changes" | Show PRs where you requested changes |
| "changes requested" | Show all PRs with changes requested |
| "clear" / "show all" / "reset" | Remove all filters |
| "team" / "heatmap" | Toggle team review load |
| "triage" / "review now" | Move top-priority inbox PR to Review Now |

### 🏷️ Advanced Filters

| Filter | Options |
|--------|---------|
| **Priority** | Critical, High, Medium, Low |
| **Labels** | Auto-populated from your PRs |
| **Repos** | Auto-populated from your PRs |
| **Status** | 🔴 Changes Requested, 🙋 My Changes Requested, ✅ Approved, ⏳ Pending |
| **Text search** | Filter by title, author, or repo |

All filters combine (AND logic). Count indicator shows "Showing X of Y PRs".

---

## Setup

### Prerequisites

- A [GitHub Personal Access Token](https://github.com/settings/tokens) with `repo` scope
- A modern browser (Chrome recommended for voice control)

### Option A: Local AI (Recommended)

```bash
# One-time setup
brew install ollama
ollama pull qwen3-vl
ollama serve
```

Then serve the dashboard via a local HTTP server (required for CORS — opening `file://` directly will block Ollama requests):

```bash
python3 -m http.server 8080 -d /path/to/pr-dashboard
```

Open `http://localhost:8080`, paste your PAT, done.

> **⚠️ Important:** Do NOT open `index.html` directly as a `file://` URL. Browsers block requests from `null` origin to `localhost` services. Always serve via `python3 -m http.server` or any local web server.

### Option B: GitHub Models Only

Serve the dashboard the same way (or open `index.html` directly — no CORS issue since GitHub Models uses your PAT over HTTPS). Paste your PAT, switch the AI dropdown to "GitHub Models." No local setup needed — uses the free tier (150 req/day).

### macOS Ollama Tips

If `ollama serve` says "address already in use", Ollama is already running (e.g. via the macOS app). To restart:

```bash
pkill ollama && ollama serve
```

To check Ollama is running: `curl http://localhost:11434` — should return "Ollama is running".

---

## Architecture

```
index.html (single file, ~2800 lines)
├── CSS — all styles inline, dark mode support
├── HTML — auth screen, filter bar, Kanban board, feature panels, heatmap
└── JavaScript
    ├── GitHub REST API client (PAT auth, rate limit tracking)
    ├── AI client (Ollama native API + GitHub Models OpenAI-compatible)
    ├── Priority engine (heuristic scoring)
    ├── Filter system (priority, label, repo, status, text search)
    ├── Triage state (localStorage persistence)
    ├── Timeline viewer (GitHub Timeline API)
    ├── Diff preview (inline colorized diffs)
    ├── Risk scanner (AI-powered diff analysis)
    ├── Team heatmap (author distribution)
    ├── My PRs view (authored PRs tracking)
    ├── Team PRs view (per-member PR lookup)
    ├── Stale PR detector (7+ day old, 3+ day inactive)
    ├── Smart batching (repo + author grouping)
    ├── Ghost reviewer detector (assigned but silent)
    ├── Ping-pong detector (3+ review rounds)
    ├── PR dependency scanner (overlapping files)
    ├── Custom tags (per-PR user labels)
    ├── Bulk actions (multi-select triage)
    ├── Export (Markdown clipboard export)
    ├── Dark mode (theme toggle + persistence)
    ├── Focus mode (distraction-free board)
    ├── Queue Zero celebration (confetti)
    ├── Voice control (Web Speech API)
    └── Auto-refresh (5-minute interval)
```

**Zero dependencies.** No npm, no webpack, no React. Copy the file anywhere and it works.

---

## API Usage

| Action | API Calls | Notes |
|--------|-----------|-------|
| Refresh | 1 (search) + 1/PR (details) + 1/PR (reviews) | ~40 calls for 20 PRs |
| AI Summary | 1 GitHub API (diff) + 1 AI call | Cached after first use |
| Risk Scan | 1 GitHub API (diff) + 1 AI call | Cached after first use |
| Timeline | 1 per PR (on-demand) | Only when clicked |
| Diff Preview | 1 per PR (on-demand) | Only when clicked |
| Dependencies | 1 per PR (file list) | Batched, on-demand |
| My PRs | 1 (search) | On tab switch |
| Team PRs | 2 (search assigned + authored) | On member select |

**GitHub API rate limit:** 5,000 requests/hour with PAT. Dashboard shows remaining quota in the footer.

**AI token usage (GitHub Models free tier):**
- 150 requests/day
- ~3,000 tokens per summary or risk scan
- Cached results don't re-call

**Ollama:** Unlimited. Runs locally, no quotas.

---

## Sharing

Send the single `index.html` file to anyone. They paste their own PAT, and it works with their assigned PRs. No server, no deployment, no accounts.

---

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `r` | Refresh PRs |
| `v` | Toggle voice control |

---

*Built with vanilla HTML/CSS/JS. No frameworks were harmed in the making of this dashboard.*
