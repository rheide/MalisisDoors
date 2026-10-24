# Door rendering performance comparison

Original: Minecraft 1.12.2, Doors 7.3.0 / Core 6.5.1. Optimized legacy: Doors 8.0.0 / Core 7.0.0. Native port: Minecraft 1.21.1, Doors 9.0.0 / Core 8.0.0.

All fixtures contain 64 roots on an 8x8 grid, four blocks apart, facing south. Garage columns have three segments. Each closed, opened and fixed-midpoint phase records 180 frames after at least 12 seconds and 90 warmup frames. Camera: 15,79,-13, yaw 0, pitch 23. Clients use 1280x720, view distance 6, fancy graphics, AO, no shaders, no OptiFine, no mipmaps and no vsync. Emitted door vertices and callback counts match the legacy runs in every phase.

The machine uses Mesa llvmpipe software OpenGL. These are CPU/render-submission measurements, not hardware GPU/FPS or shader acceptance. Java 8/Forge and Java 21/NeoForge use different engines, so cross-version frame times include engine differences. Legacy callbacks include immediate buffer flushes, while modern callbacks enqueue vertices and flush them later outside the callback. Callback reductions therefore describe allocation and submission cost; they are not total render-cost or GPU speedups. The complete frame interval includes the later flush. The legacy runs used an integrated server; modern uses an isolated dedicated server. Callback timings isolate the renderer more closely than total frames, but allocation instrumentation and shared-host noise still apply. Original and optimized legacy timings were collected earlier in this session; modern timings were collected after the complete port.

| Fixture | Callback ms: original / optimized 1.12.2 / 1.21.1 | Callback KiB: original / optimized 1.12.2 / 1.21.1 | Frame ms: original / optimized 1.12.2 / 1.21.1 |
|---|---:|---:|---:|
| jail_door_closed | 0.674 / 0.391 / 0.050 | 1638.5 / 187.0 / 0.1 | 5.552 / 5.204 / 3.643 |
| jail_door_opened | 0.680 / 0.394 / 0.047 | 1639.0 / 187.0 / 0.1 | 5.520 / 5.128 / 2.821 |
| jail_door_midpoint | 0.674 / 0.400 / 0.045 | 1635.0 / 187.0 / 0.1 | 5.525 / 5.456 / 2.729 |
| wood_sliding_door_closed | 0.699 / 0.436 / 0.042 | 1635.0 / 187.0 / 0.1 | 5.657 / 5.837 / 2.689 |
| wood_sliding_door_opened | 0.686 / 0.408 / 0.071 | 1635.0 / 187.0 / 0.1 | 5.416 / 5.124 / 3.741 |
| wood_sliding_door_midpoint | 0.687 / 0.399 / 0.053 | 1635.0 / 187.0 / 0.1 | 5.400 / 5.033 / 3.307 |
| shoji_door_closed | 0.684 / 0.403 / 0.061 | 1635.0 / 187.0 / 0.1 | 5.400 / 5.046 / 3.510 |
| shoji_door_opened | 0.679 / 0.401 / 0.054 | 1635.0 / 187.0 / 0.1 | 5.402 / 5.073 / 3.100 |
| shoji_door_midpoint | 0.673 / 0.414 / 0.060 | 1635.0 / 187.0 / 0.1 | 5.359 / 5.411 / 3.933 |
| garage_door_closed | 2.826 / 1.501 / 0.101 | 8338.5 / 2213.0 / 4.6 | 7.749 / 6.204 / 4.316 |
| garage_door_opened | 2.857 / 1.531 / 0.087 | 8325.0 / 2199.5 / 4.6 | 7.792 / 6.293 / 3.722 |
| garage_door_midpoint | 2.937 / 1.719 / 0.080 | 8325.0 / 2199.5 / 4.6 | 8.621 / 6.694 / 3.206 |
| big_door_iron_3x3_closed | 1.690 / 1.452 / 0.038 | 3367.0 / 1632.5 / 1.6 | 6.514 / 6.332 / 2.845 |
| big_door_iron_3x3_opened | 1.655 / 1.449 / 0.039 | 3367.0 / 1626.5 / 1.6 | 6.396 / 6.285 / 2.839 |
| big_door_iron_3x3_midpoint | 1.678 / 1.418 / 0.038 | 3367.0 / 1626.5 / 1.6 | 6.525 / 6.218 / 2.822 |
| customdoor_closed | 1.960 / 1.258 / 0.106 | 4335.0 / 461.0 / 3.1 | 7.183 / 7.050 / 2.838 |
| customdoor_opened | 1.976 / 1.206 / 0.124 | 4335.0 / 461.0 / 3.1 | 7.070 / 6.281 / 3.244 |
| customdoor_midpoint | 1.980 / 1.210 / 0.126 | 4335.0 / 461.0 / 3.1 | 7.237 / 6.340 / 3.735 |

Against optimized 1.12.2, native callback time fell 82.6%–97.4% and callback allocation fell 99.33%–99.95% across these fixtures.

The gains come from immutable cached meshes, native buffered rendering, renderer-owned scratch poses, per-tick partner/centering lookups, shared garage-column topology and bounded animation/resource caches. The old renderer repeatedly reconstructed/transformed mutable geometry and performed more immediate rendering work.

Draw-call counters were not available reliably in the legacy run and are not reported. The port uses native batched render types, but that architecture alone does not establish a measured GPU draw-call reduction.

Raw CSVs, environment records, screenshots and completion markers are retained under benchmarks/{baseline-measured,optimized-measured,optimized-3x3-recheck,modern-release-measured}. The first optimized legacy 3x3 result overlapped compilation and is replaced by its focused rerun. The contaminated initial modern showroom run was discarded; the clean pre-scratch-pose run is retained separately as modern-matched-clean. No performance run connected to production.
