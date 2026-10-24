# Changelog

## 8.0.0 - 2026-10-24

- Cache bounded renderer-owned animation plans across all original movement families; reset caches on world unload and texture stitching. Keep third-party movement implementations on their original path.
- Isolate spinning-door transformation instances so cached poses cannot be changed by another door.
- Remove duplicate fence-gate setup and repeated parameter initialization.
- Avoid allocating garage-door sets for opening duration and render bounds; walk columns iteratively.
- Preserve the 7.3.0 content, registry identifiers, saved data and interactions.
- Build with Forge 1.12.2-14.23.5.2860 and Java 8. Optimized dependency: local MalisisCore 7.0.0.
- Validation: 69,174 animation geometry/visibility comparisons across 25 movement classes; 2,080 garage-column checks; isolated client fixtures and dedicated-server startup/shutdown. See the local benchmark report for measured performance and scope.
