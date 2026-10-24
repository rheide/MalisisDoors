# Doors-compat 9.1.0 validation

Built with Java 21 and NeoForge 21.1.264, paired with Core-compat 8.0.1.

- Build: 10,944 original-engine door movement comparisons, 9,792 mechanism comparisons, and 19 Core checks pass.
- Original save producer: actual Forge 1.12.2 client with original Doors 7.3.0/Core 6.5.1; 47 original placeable item families plus a remote large-door instance. Non-default custom settings/materials, populated Factory, player custom-door stack and a cross-chunk forcefield are included.
- First native upgrade: 4,807 checks pass, including real block/entity and property checks, custom materials/settings, Factory counts, vanilla item-data-fixer plus actual ItemStack parser, field members, reconstructed footprints, signed position packing, 5-bit palette boundary preservation and unsupported-material rejection through ChunkStorage.
- Second boot: 4,825 checks pass. The remote chunk was confirmed to retain DataVersion 1343 before this run; it converts using the sidecar registry after native level metadata has been saved.
- Actual client on the upgraded world: 47 original-family entities received, custom settings/materials retained, standard/mechanism meshes rendered, resource reload completed. The converted-world screenshot was inspected. Mesa llvmpipe software rendering; this is not hardware FPS/shader acceptance.
- Native regression: 4,602 server checks, 96 actual yaw/sneak placement cases and 128 front/back emitted-UV comparisons. The native client workflow covers all creative items, mechanism/flat families, Factory payloads, access-code handling and resource reload.

Original world and chunk backups are retained in the isolated test saves. Production server saves and processes were not changed by these checks. Performance measurements remain those in PERFORMANCE-COMPARISON.md; the migration release does not claim a new FPS measurement. Production-modpack migration and third-party custom-material mappings remain outside this isolated acceptance result.
