# MalisisDoors-compat 9.1 - Minecraft 1.21.1

Native NeoForge port of MalisisDoors, with all 49 original creative items restored. Install both `malisisdoors-compat-1.21.1-9.1.0.jar` and `malisiscore-compat-1.21.1-8.0.1.jar` on client and server, using Java 21 and NeoForge 21.1.264 or a compatible newer 21.1 build.

The inventory includes all standard sliding, jail, shoji, laboratory and factory doors; all sixteen curtains; eight 3x3 doors; carriage and medieval doors; garage doors; the high-tech door; saloon doors; vertical and rusty hatches; six trapdoors; the camouflage gate; rusty ladder and handle; the Door Factory and custom door; and the forcefield projector.

The Door Factory supports all eighteen movements, material panels, sound families, opening and auto-close times, paired doors, proximity activation, four redstone policies and access codes. Carriage and medieval doors accept an oak or spruce door plus five identical frame blocks; ingredients may be stacked in the 2x2 grid. Forcefields use two corner selections, recharge automatically, toggle on use and are removed by sneak-use with the projector.

Vanilla doors, trapdoors and gates animate using their native models. The server setting `modifyVanillaDoors` controls this behavior and requires a world restart. The client setting `use3DItems` controls 3D rendering for ordinary door items. Specialized items retain their 3D models.

Moving collision and ray selection share the render transforms. Displaced leaves remain clickable, intervening walls block clicks, and breaking a multiblock part clears its structure. Bounded meshes, cached topology and renderer-owned scratch poses reduce repeated work and allocation. Resource reload rebuilds texture-dependent caches.

Build with Java 21: `./gradlew build --no-daemon`. The sibling `core-modern` is built as a separate dependency JAR. The build compares animation vertices against fixtures exported by the actual legacy engine. Additional opt-in client/server checks are documented in [the feature and validation report](docs/PORT-STATUS-1.21.1.md); matched measurements and their limits are in [the performance report](docs/PERFORMANCE-COMPARISON.md).

Version 9.1.0 automatically upgrades original 1.12.2 Malisis blocks, entities and custom item data. Internal IDs stay unchanged despite the public rename. See [UPGRADING.md](docs/UPGRADING.md) for the preserved registry sidecar, conversion scope and handling unsupported third-party materials. The native Core supplies this port's support API; unrelated legacy Core consumers need their own ports.

Upstream history and MIT licensing remain in the separate Doors/Core repositories. Original assets are covered by LICENSE.txt; the NeoForge template license is retained in TEMPLATE_LICENSE.txt.
