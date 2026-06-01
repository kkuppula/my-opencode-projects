# Log Ocean

> AI-powered log visualization and incident analysis tool. Drop log files, get instant insights.

![Vanilla JS](https://img.shields.io/badge/vanilla-JS%2FCSS%2FHTML-f7df1e)
![AI Powered](https://img.shields.io/badge/AI-Ollama%20%2F%20Local%20LLM-purple)
![Zero Dependencies](https://img.shields.io/badge/dependencies-zero-green)

---

## What It Does

**Log Ocean** transforms raw log files into an interactive visualization with AI-powered analysis:

- **Timeline visualization** — See log density over time with severity heatmaps
- **Swimlane view** — Track events across services/sources
- **AI incident explanation** — Get root cause analysis in plain English
- **Causal chain detection** — Understand which failure caused downstream failures
- **Auto-categorization** — AI tags each log entry (connection, timeout, auth, recovery, etc.)
- **Runbook generation** — Get step-by-step remediation steps
- **Natural language chat** — Ask questions about your logs: "What caused the errors?"

---

## Features

| Feature | Description |
|---------|-------------|
| **Timeline Replay** | Replay incident progression with audio alerts for errors |
| **Severity Filtering** | Filter by DEBUG, INFO, WARN, ERROR, FATAL |
| **Regex Search** | Real-time filtering with regex support |
| **Multiple Formats** | Auto-detects JSON, Spring Boot, Syslog, Apache, generic timestamps |
| **Streaming AI** | Real-time streaming responses from local LLM |
| **Dark Theme** | Easy on the eyes for late-night debugging |
| **Zero Dependencies** | Single HTML file, no build step, no npm |

---

## AI Capabilities

All AI features run **locally** via [Ollama](https://ollama.ai) — your logs never leave your machine.

| Action | What It Does |
|--------|--------------|
| **Explain Incident** | Summarizes root cause, impact, timeline, and recovery |
| **Find Causal Chain** | Maps ServiceA → ServiceB → ServiceC failure propagation |
| **Auto-Tag** | Categorizes every log entry by type (connection, timeout, auth, etc.) |
| **Runbook** | Generates step-by-step remediation guide |
| **Chat** | Ask natural language questions about your logs |

---

## Quick Start

### 1. Install Ollama (one-time)

```bash
brew install ollama
ollama pull qwen3-vl
ollama serve
```

### 2. Serve the dashboard

```bash
# Clone or download this repo
cd log-ocean
python3 -m http.server 8080
```

### 3. Open in browser

```
http://localhost:8080
```

Drop a log file or paste log text — Log Ocean handles the rest.

---

## Supported Log Formats

Log Ocean auto-detects format from the first few lines:

| Format | Example |
|--------|---------|
| **JSON (structured)** | `{"timestamp": "...", "level": "ERROR", "message": "..."}` |
| **Spring Boot** | `2024-01-15 10:23:45.123 ERROR [service] --- [thread] Class : Message` |
| **Syslog** | `Jan 15 10:23:45 hostname service[pid]: message` |
| **Apache** | `127.0.0.1 - - [15/Jan/2024:10:23:45] "GET /path" 500 1234` |
| **Generic ISO** | `2024-01-15T10:23:45.123Z [ERROR] message` |

---

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `Space` | Play/Pause timeline replay |
| `/` | Focus search box |
| `E` | Filter to errors only |
| `R` | Reset all filters |
| `C` | Toggle chat panel |

---

## Architecture

```
index.html (single file, ~1500 lines)
├── CSS — Dark theme, responsive layout
├── HTML — Drop zone, timeline, swimlanes, log table, stats panel, chat
└── JavaScript
    ├── Log parsers (JSON, Spring Boot, Syslog, Apache, generic)
    ├── Timeline renderer (Canvas)
    ├── Minimap with brush selection
    ├── Swimlane visualization
    ├── Virtual scrolling log table
    ├── Stats panel (severity distribution, sparklines, top errors)
    ├── Ollama streaming client
    ├── AI analysis (explain, correlate, categorize, runbook)
    ├── Chat interface with suggestions
    ├── Timeline replay with audio (Web Audio API)
    └── Particle background animation
```

---

## Why Local AI?

| Benefit | Details |
|---------|---------|
| **Privacy** | Logs never leave your machine |
| **Speed** | No network latency |
| **Free** | No API costs or rate limits |
| **Offline** | Works without internet |

---

## Requirements

- Modern browser (Chrome/Firefox/Safari)
- [Ollama](https://ollama.ai) running locally (for AI features)
- `qwen3-vl` model (or modify `OLLAMA_MODEL` in the code)

---

## License

MIT

---

*Built with vanilla HTML/CSS/JS. No frameworks were harmed in the making of this tool.*
