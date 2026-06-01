# code-rag: Semantic Code Search MCP Server

> Search your codebase using natural language. Built as an MCP server for AI coding assistants.

![MCP Server](https://img.shields.io/badge/MCP-Server-blue)
![ChromaDB](https://img.shields.io/badge/ChromaDB-Vector%20DB-orange)
![RAG](https://img.shields.io/badge/RAG-Pipeline-purple)

---

## What It Does

**code-rag** is a semantic code search engine that lets you query code with natural language:

```
"how are users fetched from the database"
"feature flag check pattern"
"authentication middleware"
"error handling in API routes"
```

Instead of exact keyword matching, it understands the *meaning* of your query and finds relevant code.

---

## How It Works

```
┌─────────────────────────────────────────────────────────────┐
│  Your Query: "how do we validate user permissions"         │
│                           │                                 │
│                           ▼                                 │
│              ┌────────────────────────┐                     │
│              │   Embed Query          │                     │
│              │   (vector embedding)   │                     │
│              └────────────────────────┘                     │
│                           │                                 │
│                           ▼                                 │
│              ┌────────────────────────┐                     │
│              │   ChromaDB             │                     │
│              │   (vector similarity)  │                     │
│              └────────────────────────┘                     │
│                           │                                 │
│                           ▼                                 │
│              ┌────────────────────────┐                     │
│              │   Top K Code Chunks    │                     │
│              │   (ranked by relevance)│                     │
│              └────────────────────────┘                     │
└─────────────────────────────────────────────────────────────┘
```

---

## Features

| Feature | Description |
|---------|-------------|
| **Natural Language Search** | Query code semantically, not just keywords |
| **Related Files** | Find files semantically related to a given file |
| **Context Expansion** | Get surrounding code for search results |
| **Multi-Repo Support** | Index multiple repositories |
| **MCP Protocol** | Works with Claude, OpenCode, and other MCP clients |
| **Local Execution** | 100% local — your code never leaves your machine |

---

## MCP Tools

| Tool | Description |
|------|-------------|
| `search_code` | Search code with natural language |
| `find_related` | Find files related to a given file |
| `get_context` | Expand context around a search result |
| `index_stats` | View indexing statistics |

---

## Installation

### 1. Clone and set up

```bash
cd code-rag
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 2. Configure repos to index

Edit `config.yaml`:

```yaml
repos:
  my-backend:
    path: /path/to/your/backend
    extensions: [".java", ".py", ".go"]
    exclude_dirs: [".git", "build", "node_modules"]
    description: Backend services

  my-frontend:
    path: /path/to/your/frontend
    extensions: [".ts", ".tsx", ".js"]
    exclude_dirs: [".git", "node_modules", "dist"]
    description: Frontend app
```

### 3. Run the indexer

```bash
./run-indexer.sh
```

### 4. Start the MCP server

```bash
python3 server.py
```

---

## OpenCode Integration

Add to your `opencode.json`:

```json
{
  "mcp": {
    "code-rag": {
      "type": "local",
      "command": [
        "./.opencode/mcp-servers/code-rag/venv/bin/python3",
        "./.opencode/mcp-servers/code-rag/server.py"
      ],
      "enabled": true
    }
  }
}
```

---

## Configuration

### config.yaml

```yaml
repos:
  # Define repos to index
  my-repo:
    path: /absolute/path/to/repo
    extensions: [".java", ".py", ".ts"]
    exclude_dirs: [".git", "node_modules"]

indexing:
  chunk_size: 60           # Lines per chunk
  chunk_overlap: 20        # Overlapping lines
  max_file_size: 50000     # Skip large files
  embedding_model: default # "default", "openai", or "ollama"

search:
  default_top_k: 5
  max_top_k: 20
```

### Embedding Models

| Model | Configuration |
|-------|---------------|
| **Default** | ChromaDB built-in (all-MiniLM-L6-v2) — no setup needed |
| **OpenAI** | Set `embedding_model: openai` and `OPENAI_API_KEY` |
| **Ollama** | Set `embedding_model: ollama` and run `ollama pull nomic-embed-text` |

---

## Files

```
code-rag/
├── server.py         # MCP server implementation
├── indexer.py        # Code indexing logic
├── config.yaml       # Repository configuration
├── run-indexer.sh    # Indexer runner script
├── requirements.txt  # Python dependencies
└── .chroma/          # Vector database (created on first index)
```

---

## Example Queries

| Query | What It Finds |
|-------|---------------|
| "how are users fetched from database" | User repository, DAO, ORM code |
| "authentication middleware" | Auth interceptors, guards, filters |
| "error handling patterns" | Try-catch blocks, error boundaries, exception handlers |
| "API rate limiting" | Throttling logic, rate limit middleware |
| "date comparison logic" | Date utilities, temporal comparisons |

---

## Why Semantic Search?

| Traditional Search | code-rag |
|-------------------|----------|
| Requires exact keywords | Understands intent |
| Miss renamed variables | Finds conceptually similar code |
| Multiple searches needed | Single query, comprehensive results |
| No relationship awareness | Finds related files automatically |

---

## Requirements

- Python 3.10+
- ChromaDB
- MCP-compatible client (OpenCode, Claude, etc.)

---

## License

MIT
