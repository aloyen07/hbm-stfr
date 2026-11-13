#!/usr/bin/env bash
# Check the whole voice dataset; unchanged recordings are reused automatically.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
exec voicelines/.venv/bin/python scripts/build_voice_assets.py "$@"
