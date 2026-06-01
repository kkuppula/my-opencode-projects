# code-rag: Semantic Code Search Engine

---

## What is code-rag?

**code-rag** (Code Retrieval-Augmented Generation) is a **semantic code search engine** available as an MCP (Model Context Protocol) tool in this workspace. It indexes your codebase and lets you search it using **natural language** instead of exact keyword matching.

---

## Available code-rag Tools

| Tool | Purpose |
|------|---------|
| `code-rag_search_code` | Search code using natural language queries |
| `code-rag_find_related` | Find files semantically related to a given file |
| `code-rag_get_context` | Get expanded code context around a search result |
| `code-rag_index_stats` | View stats about the indexed codebase |

---

## How It Works

```
Traditional Search (grep/glob):     "findUserById"  → exact string match only

code-rag (semantic search):          "how are users fetched from the database"
                                     → finds relevant code even if those exact words aren't used
```

code-rag uses **vector embeddings** to understand the semantic meaning of code, storing them in a local **ChromaDB** vector database. When you query in natural language, it converts your query to a vector and finds the most semantically similar code chunks.

---

## Advantages Over Traditional Search

| Advantage | Traditional (grep/glob) | code-rag |
|-----------|------------------------|----------|
| **Query style** | Exact keywords/regex | Natural language |
| **Finds intent** | ❌ Only literal matches | ✅ Understands what you *mean* |
| **Discovery** | Need to know what to search for | Can explore unfamiliar code |
| **Related files** | Manual investigation | Automatic relationship detection |
| **Context-aware** | Line-level results | Chunk-level with surrounding context |
| **Cross-cutting concerns** | Multiple searches needed | Single query finds patterns across files |

---

## Example Queries

```
# Instead of guessing function names:
"how are course banners rendered"
"feature flag check pattern"
"date comparison logic"
"user role entitlement check"

# Find related files automatically:
find_related("/src/services/UserService.java")
→ Returns: UserController, UserRepository, UserDTO, UserTest...
```

---

## Why It's Mandatory in the Multi-Agent Pipeline

Every sub-agent in the pipeline is **required** to use code-rag as their **primary exploration tool** before falling back to grep/glob/read. This is because:

1. **Faster discovery** — finds relevant code in 1 query vs. 5–10 grep searches
2. **Better context** — understands code relationships, not just text matches
3. **Reduces missed files** — semantic search catches code that keyword search misses (e.g., different variable names for the same concept)
4. **Supports unfamiliar codebases** — agents don't need to know exact naming conventions upfront

---

## Is code-rag Running Locally?

**Yes! code-rag runs entirely on your local machine.**

### Local Setup Details

| Property | Value |
|----------|-------|
| **Database location** | `.opencode/mcp-servers/code-rag/.chroma` |
| **Database type** | ChromaDB (vector database) |
| **Total indexed chunks** | 224,534 |
| **Repos indexed** | `learn`, `ultra` |
| **Runs on** | Local machine (macOS) |

### Architecture

```
Your Machine (localhost)
┌─────────────────────────────────────────────────────┐
│                                                     │
│  OpenCode / Agent                                   │
│       │                                             │
│       ▼ (MCP protocol)                              │
│  code-rag MCP Server                                │
│       │                                             │
│       ▼                                             │
│  ChromaDB (.chroma/)                                │
│  ┌───────────────────────────────────┐              │
│  │ 224,534 code chunks              │              │
│  │ Vector embeddings                 │              │
│  │ learn repo + ultra repo           │              │
│  └───────────────────────────────────┘              │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Benefits of Local Execution

| Benefit | Details |
|---------|---------|
| ✅ **Privacy** | Your code never leaves your machine |
| ✅ **Speed** | No network latency — queries hit a local vector DB |
| ✅ **Offline capable** | Works without internet (once indexed) |
| ✅ **Persistent** | Index stored in `.chroma` — survives restarts |

---

## Summary

> **code-rag = "Google for your codebase"** — ask questions in plain English, get relevant code back, with relationship awareness and expanded context. It runs 100% locally, keeping your code private and searches fast.

---

*Generated: May 21, 2026*
