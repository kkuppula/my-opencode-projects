"""
Code RAG MCP Server
===================
Exposes semantic code search as MCP tools that AI agents can call.

Tools provided:
    - search_code: Semantic search across indexed repositories
    - find_related: Find files related to a given file
    - get_context: Get surrounding context for a specific file + line range

Usage:
    python server.py
"""

import json
from pathlib import Path

import chromadb
import yaml
from mcp.server.fastmcp import FastMCP

# Initialize
CONFIG_PATH = Path(__file__).parent / "config.yaml"
DB_PATH = str(Path(__file__).parent / ".chroma")

with open(CONFIG_PATH) as f:
    config = yaml.safe_load(f)

client = chromadb.PersistentClient(path=DB_PATH)

try:
    collection = client.get_collection("codebase")
except ValueError:
    print("⚠️  No index found. Run `python indexer.py` first.")
    collection = None

# Create MCP server
mcp = FastMCP("code-rag")


@mcp.tool()
def search_code(query: str, repo: str = None, language: str = None, top_k: int = 5) -> str:
    """
    Search the codebase semantically.

    Args:
        query: Natural language description of what you're looking for.
               Examples: "how are course banners rendered", "feature flag check pattern",
               "date comparison logic", "user role entitlement check"
        repo: Optional. Filter to a specific repo ("learn" or "ultra")
        language: Optional. Filter by language ("java", "typescript", "velocity", etc.)
        top_k: Number of results to return (default 5, max 20)

    Returns:
        JSON array of matching code chunks with file paths, line numbers, and snippets.
    """
    if collection is None:
        return json.dumps({"error": "Index not built. Run: python indexer.py"})

    top_k = min(top_k, config["search"]["max_top_k"])

    # Build where filter
    where_filter = None
    conditions = []
    if repo:
        conditions.append({"repo": {"$eq": repo}})
    if language:
        conditions.append({"language": {"$eq": language}})

    if len(conditions) == 1:
        where_filter = conditions[0]
    elif len(conditions) > 1:
        where_filter = {"$and": conditions}

    results = collection.query(
        query_texts=[query],
        n_results=top_k,
        where=where_filter,
    )

    if not results["documents"] or not results["documents"][0]:
        return json.dumps({"results": [], "message": "No matches found"})

    output = []
    for i, (doc, meta, distance) in enumerate(
        zip(results["documents"][0], results["metadatas"][0], results["distances"][0])
    ):
        # Calculate relevance score (ChromaDB returns distances, lower = better)
        relevance = round(1 - distance, 3)

        output.append(
            {
                "rank": i + 1,
                "relevance": relevance,
                "file": meta["file"],
                "lines": f"L{meta['start_line']}-{meta['end_line']}",
                "language": meta["language"],
                "repo": meta["repo"],
                "snippet": doc[:800],  # Truncate for context window efficiency
            }
        )

    return json.dumps(output, indent=2)


@mcp.tool()
def find_related(file_path: str, top_k: int = 5) -> str:
    """
    Find files semantically related to a given file.

    Useful for understanding dependencies, finding similar patterns,
    or discovering files that should be modified together.

    Args:
        file_path: Absolute path to the file to find relations for.
        top_k: Number of related files to return (default 5)

    Returns:
        JSON array of related files with relevance scores.
    """
    if collection is None:
        return json.dumps({"error": "Index not built. Run: python indexer.py"})

    try:
        content = Path(file_path).read_text(encoding="utf-8", errors="ignore")[:3000]
    except (OSError, FileNotFoundError):
        return json.dumps({"error": f"Cannot read file: {file_path}"})

    results = collection.query(
        query_texts=[content],
        n_results=top_k + 5,  # Extra to deduplicate same-file chunks
        where={"file": {"$ne": file_path}},
    )

    if not results["metadatas"] or not results["metadatas"][0]:
        return json.dumps({"results": [], "message": "No related files found"})

    # Deduplicate by file path (multiple chunks from same file)
    seen_files = set()
    output = []
    for meta, distance in zip(results["metadatas"][0], results["distances"][0]):
        if meta["file"] in seen_files:
            continue
        seen_files.add(meta["file"])

        output.append(
            {
                "file": meta["file"],
                "relevance": round(1 - distance, 3),
                "language": meta["language"],
                "repo": meta["repo"],
            }
        )

        if len(output) >= top_k:
            break

    return json.dumps(output, indent=2)


@mcp.tool()
def get_context(file_path: str, start_line: int, end_line: int, expand: int = 20) -> str:
    """
    Get code context around a specific location.

    Use this after search_code to get more context around a result.

    Args:
        file_path: Absolute path to the file.
        start_line: Starting line number (1-indexed).
        end_line: Ending line number.
        expand: Number of extra lines to include above and below (default 20).

    Returns:
        The code content with line numbers.
    """
    try:
        lines = Path(file_path).read_text(encoding="utf-8", errors="ignore").splitlines()
    except (OSError, FileNotFoundError):
        return json.dumps({"error": f"Cannot read file: {file_path}"})

    # Expand range
    actual_start = max(0, start_line - 1 - expand)
    actual_end = min(len(lines), end_line + expand)

    output_lines = []
    for i in range(actual_start, actual_end):
        output_lines.append(f"{i + 1:>5}: {lines[i]}")

    return "\n".join(output_lines)


@mcp.tool()
def index_stats() -> str:
    """
    Get statistics about the current code index.

    Returns:
        JSON with total chunks, per-repo counts, and per-language counts.
    """
    if collection is None:
        return json.dumps({"error": "Index not built. Run: python indexer.py"})

    total = collection.count()

    # Sample to get breakdown (ChromaDB doesn't have GROUP BY)
    # Get all metadata for stats
    stats = {
        "total_chunks": total,
        "database_path": DB_PATH,
        "repos_configured": list(config["repos"].keys()),
    }

    return json.dumps(stats, indent=2)


if __name__ == "__main__":
    mcp.run()
