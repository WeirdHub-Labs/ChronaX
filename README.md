# ChronaX

**Based on Paper for generic performance and flexible API**

ChronaX is an experimental optimization branch of [Chrona](https://github.com/bindglam/Chrona).
It combines selected performance work from Leaf, Purpur, and other upstream forks, while keeping plugin compatibility as a primary goal.

## Project Direction

- Plugin compatibility first, then optimization.
- Keep Paper-compatible behavior and API surface where possible.
- Apply practical low-risk performance improvements for real servers.

## What ChronaX Focuses On

- Experimental performance tuning on top of Chrona.
- Compatibility-oriented async/parallel improvements.
- Hybrid fork approach (Chrona + Leaf + Purpur + other proven patches).

## Parallel Processing Support

ChronaX includes parallel execution paths for server performance:

- Chunk-system parallel generation controls (`chunk-system.gen-parallelism`).
- Chunk-system thread tuning (`chunk-system.io-threads`, `chunk-system.worker-threads`).
- Optional parallel world ticking (`leaf-overrides.async.parallel-world-ticking`).
- Runtime profile presets for compatibility/performance balance (`runtime-profile`).
- Shared CPU thread budgeting for async subsystems (`thread-budget.*`).
- Buffered read drain limits for PWT (`max-buffered-read-requests`, `max-read-requests-per-tick`).

Example defaults from `chronax.yml`:

```yml
chunk-system:
  gen-parallelism: on
  io-threads: 4
  worker-threads: 12

leaf-overrides:
  async:
    parallel-world-ticking:
      enabled: true
      threads: 8
```

## Credits

- Chrona: https://github.com/bindglam/Chrona
- Leaf: https://github.com/Winds-Studio/Leaf
- Purpur: https://github.com/PurpurMC/Purpur
- Paper: https://github.com/PaperMC/Paper
