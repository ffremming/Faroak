#!/usr/bin/env bash
set -euo pipefail

BUILD_DIR="${BUILD_DIR:-/tmp/gamebuild}"
SOURCE_LIST="${SOURCE_LIST:-/tmp/game_sources.txt}"
PERF_OUT="${PERF_OUT:-perf-results/latest.json}"

find resources -name '*.java' > "$SOURCE_LIST"
javac -encoding UTF-8 -d "$BUILD_DIR" @"$SOURCE_LIST"
java -cp "$BUILD_DIR" resources.testing.perf.PerformanceRunner --out "$PERF_OUT" "$@"
