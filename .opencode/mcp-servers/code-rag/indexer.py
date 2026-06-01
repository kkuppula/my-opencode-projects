"""
Code RAG Indexer
================
Indexes repository code into ChromaDB for semantic search.

Usage:
    python indexer.py                  # Index all repos from config.yaml
    python indexer.py --repo learn     # Index only the Learn repo
    python indexer.py --reset          # Clear and rebuild the entire index
"""

import argparse
import hashlib
import time
from pathlib import Path

import chromadb
import yaml


def load_config():
    """Load configuration from config.yaml."""
    config_path = Path(__file__).parent / "config.yaml"
    with open(config_path) as f:
        return yaml.safe_load(f)


def get_collection(reset=False):
    """Get or create the ChromaDB collection."""
    db_path = str(Path(__file__).parent / ".chroma")
    client = chromadb.PersistentClient(path=db_path)

    if reset:
        try:
            client.delete_collection("codebase")
            print("🗑️  Cleared existing index")
        except ValueError:
            pass

    collection = client.get_or_create_collection(
        name="codebase",
        metadata={"hnsw:space": "cosine"},
    )
    return collection


def should_skip_file(file_path: Path, exclude_dirs: list, max_size: int) -> bool:
    """Determine if a file should be skipped."""
    # Check excluded directories
    parts = file_path.parts
    for excluded in exclude_dirs:
        if excluded in parts:
            return True

    # Check file size
    try:
        if file_path.stat().st_size > max_size:
            return True
    except OSError:
        return True

    return False


def chunk_file(file_path: Path, chunk_size: int, chunk_overlap: int) -> list:
    """
    Split a file into overlapping chunks with metadata.

    Each chunk contains:
    - content: the actual code text
    - file: absolute file path
    - start_line: first line number (1-indexed)
    - end_line: last line number
    - language: inferred from extension
    """
    try:
        content = file_path.read_text(encoding="utf-8", errors="ignore")
    except (OSError, UnicodeDecodeError):
        return []

    lines = content.splitlines()
    if not lines:
        return []

    # Infer language from extension
    ext_to_lang = {
        ".java": "java",
        ".ts": "typescript",
        ".tsx": "typescript",
        ".js": "javascript",
        ".html": "html",
        ".vm": "velocity",
        ".properties": "properties",
        ".xml": "xml",
        ".gradle": "groovy",
        ".scss": "scss",
        ".json": "json",
    }
    language = ext_to_lang.get(file_path.suffix, "unknown")

    chunks = []
    step = chunk_size - chunk_overlap

    for i in range(0, len(lines), step):
        chunk_lines = lines[i : i + chunk_size]
        if not chunk_lines:
            continue

        # Add file context as header for better embeddings
        header = f"// File: {file_path.name} | Lines {i + 1}-{i + len(chunk_lines)}\n"
        chunk_content = header + "\n".join(chunk_lines)

        chunks.append(
            {
                "content": chunk_content,
                "file": str(file_path),
                "start_line": i + 1,
                "end_line": i + len(chunk_lines),
                "language": language,
                "filename": file_path.name,
            }
        )

    return chunks


def index_repo(collection, repo_name: str, repo_config: dict, indexing_config: dict):
    """Index a single repository into the collection."""
    repo_path = Path(repo_config["path"])
    extensions = repo_config["extensions"]
    exclude_dirs = repo_config.get("exclude_dirs", [".git"])
    max_size = indexing_config.get("max_file_size", 50000)
    chunk_size = indexing_config.get("chunk_size", 60)
    chunk_overlap = indexing_config.get("chunk_overlap", 20)

    if not repo_path.exists():
        print(f"⚠️  Repo path does not exist: {repo_path}")
        return 0

    print(f"\n📂 Indexing: {repo_name} ({repo_path})")
    print(f"   Extensions: {extensions}")
    print(f"   Chunk size: {chunk_size} lines, overlap: {chunk_overlap} lines")

    all_chunks = []
    file_count = 0

    for ext in extensions:
        for file_path in repo_path.rglob(f"*{ext}"):
            if should_skip_file(file_path, exclude_dirs, max_size):
                continue
            chunks = chunk_file(file_path, chunk_size, chunk_overlap)
            all_chunks.extend(chunks)
            file_count += 1

    if not all_chunks:
        print(f"   No files found to index")
        return 0

    print(f"   Found {file_count} files → {len(all_chunks)} chunks")

    # Batch upsert (ChromaDB supports batches up to 5461)
    batch_size = 500
    for i in range(0, len(all_chunks), batch_size):
        batch = all_chunks[i : i + batch_size]

        ids = []
        documents = []
        metadatas = []

        for chunk in batch:
            doc_id = hashlib.md5(
                f"{chunk['file']}:{chunk['start_line']}".encode()
            ).hexdigest()
            ids.append(doc_id)
            documents.append(chunk["content"])
            metadatas.append(
                {
                    "file": chunk["file"],
                    "start_line": chunk["start_line"],
                    "end_line": chunk["end_line"],
                    "language": chunk["language"],
                    "filename": chunk["filename"],
                    "repo": repo_name,
                }
            )

        collection.upsert(ids=ids, documents=documents, metadatas=metadatas)
        print(f"   Indexed batch {i // batch_size + 1}/{(len(all_chunks) - 1) // batch_size + 1}")

    return len(all_chunks)


def main():
    parser = argparse.ArgumentParser(description="Index code repositories for RAG search")
    parser.add_argument("--repo", type=str, help="Index only this repo (from config.yaml)")
    parser.add_argument("--reset", action="store_true", help="Clear and rebuild the entire index")
    args = parser.parse_args()

    config = load_config()
    collection = get_collection(reset=args.reset)

    start_time = time.time()
    total_chunks = 0

    repos = config["repos"]
    if args.repo:
        if args.repo not in repos:
            print(f"❌ Unknown repo: {args.repo}. Available: {list(repos.keys())}")
            return
        repos = {args.repo: repos[args.repo]}

    for repo_name, repo_config in repos.items():
        chunks = index_repo(collection, repo_name, repo_config, config["indexing"])
        total_chunks += chunks

    elapsed = time.time() - start_time
    print(f"\n✅ Done! Indexed {total_chunks} chunks in {elapsed:.1f}s")
    print(f"   Database: {Path(__file__).parent / '.chroma'}")


if __name__ == "__main__":
    main()
