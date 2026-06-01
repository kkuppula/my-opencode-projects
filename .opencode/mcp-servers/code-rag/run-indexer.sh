#!/bin/bash
# Run the indexer in background
# Usage: ./run-indexer.sh [--repo learn|ultra] [--reset]

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

source venv/bin/activate

echo "🚀 Starting indexer... (logging to indexer.log)"
echo "   Monitor with: tail -f $SCRIPT_DIR/indexer.log"
echo ""

python3 indexer.py "$@" 2>&1 | tee indexer.log
