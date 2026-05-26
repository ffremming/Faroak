# Performance Testing

Run from the project root:

```sh
./run-performance-tests.sh
```

The script compiles every Java file with UTF-8 source encoding and runs:

```sh
java -cp /tmp/gamebuild resources.testing.perf.PerformanceRunner
```

Default output is `perf-results/latest.json`. Pass runner options through the
script:

```sh
./run-performance-tests.sh --samples 300 --warmup 180
./run-performance-tests.sh --report-only --out perf-results/experiment.json
```

`--report-only` always exits 0 after writing JSON. Without it, the runner exits
1 when a case reaches `FAIL`.

## What It Measures

- `boot`: GamePanel construction, image loading, and initial world generation.
- `world-simulate`: isolated `WorldRuntime.simulate` cost.
- `game-update`: one full `GamePanel.update` tick.
- `visibility-query`: camera culling and visible list creation without drawing.
- `render`: `Camera.draw` into an offscreen image.
- `full-frame`: one update plus one draw; this is the FPS proxy.
- `movement-chunk-lag`: forced chunk-boundary movement and resulting tick spikes.
- `full-frame-gc-pressure`: full frames plus heap and GC counters.

## Reading Results

The JSON is intentionally stable for AI agents:

- Start with cases where `status` is `FAIL`, then `WARN`.
- Use `p95_us`, `p99_us`, `max_us`, and `budget_miss_rate` for lag; averages hide stutter.
- Compare `full-frame` against `game-update` and `render` to locate the dominant cost.
- Use `cases[].hotspots` to see exact hot methods (`class.method` + `file:line`) and each method's estimated budget share (`budget_pct`).
- Compare `heap_used_delta_kb` and `gc_collections_delta` between runs to spot allocation churn.
