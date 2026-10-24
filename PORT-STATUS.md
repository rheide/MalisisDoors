> Development update: 45 block variants are now implemented across standard doors, curtains, fifteen mechanism types, and eight trapdoor/gate/ladder types. The expanded dedicated server passed 3,470 checks and the client rendered all families, including resource reload. The report below describes the earlier exported 22-variant alpha. The current branch is still in progress; see PORTING-CHECKLIST.md.

# Malisis Doors 1.21.1: experimental working subset

This is a native NeoForge alpha, not a complete port of MalisisDoors or the legacy MalisisCore API. The time-boxed attempt produced a usable subset for a fresh test world. Do not treat it as a conversion tool for an existing 1.12.2 world.

Install both JARs into a Minecraft 1.21.1 / NeoForge 21.1.264 instance using Java 21:

- `malisisdoors-1.21.1-9.0.0-alpha.1.jar`
- `malisiscore-1.21.1-8.0.0-alpha.1.jar`

Both are copied to `/home/rheide/malisis`. No production deployment was performed.

## Included

| Content | Native behavior |
|---|---|
| Wooden and iron sliding doors | Animated horizontal travel, original textures and duration; iron requires redstone |
| Jail door | Animated horizontal travel, original textures and 12-tick duration; requires redstone |
| Shoji door | Animated horizontal travel, original textures and 20-tick duration |
| Laboratory door | Animated upward travel, original textures and 12-tick duration |
| Factory door | Panels split upward/downward, original textures and 12-tick duration |
| All 16 curtain colours | Animated gathering toward the hinge; legacy `curtain_silver` identifier retained |
| Placement and items | Native two-block placement in all four directions, flat original item icons, creative tab |
| Opening | Hand and redstone behavior, paired opposite-hinge leaves (except unpaired laboratory door), upper/lower state synchronization |
| Resources | Original licensed textures, converted original recipe patterns/ingredients/output counts, loot tables, English display names |
| Rendering | Cached immutable meshes, shared native buffers, live light, reversible per-door animation, resource-reload invalidation, conservative moving-door render bounds |

The separate Core JAR supplies the new animation timeline, bounded cache and immutable panel mesh. It does **not** implement the other legacy Core modules or promise binary/source compatibility with other Malisis mods.

## Unfinished and known differences

- Garage doors; 3x3, carriage, medieval and high-tech doors; custom programmable doors; Door Factory and its GUI/inventory; saloon doors; hatches, trapdoors, ladders, camouflage fence gates; forcefields and their items.
- Replacement animation for Minecraft's vanilla doors, trapdoors and fence gates.
- The legacy descriptor/customization/editor/networking APIs and most of MalisisCore's original features.
- Original per-family sounds and configurable 3D item rendering. This alpha uses native door sounds and the original flat icons.
- Continuous collision with moving panels and interaction at their displaced positions. Collision changes at the open/closed state; open doorway targeting stays at the original doorway. Upward/split panels beyond the original cells have no extra carried collision. This is not legacy collision parity.
- Complete language conversion, all mod integrations, production-modpack/shader/hardware-GPU testing, and conversion of old block/tile NBT into native 1.21.1 states.
- A matched 1.12.2 versus 1.21.1 performance benchmark. The legacy measurements remain in `PERFORMANCE-1.12.2.md`; the modern cache design alone does not establish an FPS improvement.

## Validation evidence

`benchmarks/modern-native/results/server-verification.txt` records packaged-JAR server checks: real item placement of all 22 variants in all four facings, hand/redstone policy, paired state, two-block state, one renderer entity per leaf, original recipe availability/ingredient resolution, and collision orientation.

`benchmarks/modern-native/results/client-verification.txt` records an actual local client connection to that dedicated server, 88 loaded door entities, all 66 panel sprites resolving, rendering all 22 descriptor meshes, observed intermediate animation poses, and a resource reload that replaced cached meshes. Screenshots are in the adjacent `screenshots/` directory. The client used Mesa llvmpipe software rendering; this is functional rendering evidence, not hardware FPS acceptance. The test VM lacks the optional narrator library; Minecraft continued and completed the checks.

`modern-build.log` records the Java 21 build and 19 timeline reversal/cache contract checks. Server and client run the actual built mod JARs in isolated `benchmarks/modern-native/{server,client}/mods/` directories. Server shutdown saves the isolated fixture world.

## Build and reproduce

From `modern/`:

```
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew build --no-daemon
```

This builds Doors plus the sibling `core-modern/` library. The MDK/toolchain reference is [NeoForge's 1.21.1 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle); the renderer uses the [native block entity renderer API](https://docs.neoforged.net/docs/1.21.1/blockentities/ber/).

The optional verification harness is inactive unless `malisis.verify.out` is explicitly set. Its server fixture replaces blocks in the isolated test world, so run it only through the dedicated test launcher with its own game directory. `tools/run-modern-verification.py` prepares those directories and uses private pristine ASM runtime JARs: the shared legacy Gradle cache has stripped module descriptors. Shared caches and production runtimes were left alone.
