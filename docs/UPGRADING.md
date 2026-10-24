# Upgrading to MalisisDoors-compat

The public names are MalisisDoors-compat and MalisisCore-compat. Internal mod IDs remain `malisisdoors` and `malisiscore`; block/item IDs and asset namespaces remain unchanged. Replace the old JARs rather than installing both copies of the same mod.

## Staying on Minecraft 1.12.2

Use Doors 8.0.0 and Core 7.0.0 for Forge 1.12.2. The performance and public-name changes retain the original saved blocks, tile entities, metadata and items. No world conversion is needed.

## Moving an original 1.12.2 save to Minecraft 1.21.1

Use Doors-compat 9.1.0 and Core-compat 8.0.1 on both client and server, with Java 21 and NeoForge 21.1.264. Start from a complete copy of the original Forge save, including its original `level.dat`, rather than a save already opened without the required mods. Keep the untouched original as the rollback copy.

Doors automatically captures the original Forge block/item ID map in `world/malisis-compat/legacy-registry.nbt` before the first save. It retains that sidecar for old chunks visited later, including after a restart. Keep the entire `malisis-compat` directory with the world when moving or backing it up. An original level metadata file and original chunks containing Malisis data are also retained there before conversion.

Minecraft performs its normal chunk-format upgrade. Doors restores the original Malisis numeric block IDs, metadata, lower/upper halves, facing, hinges, open state and tile entities around that conversion. It translates custom-door movements, sound, timing, redstone policy, paired/proximity settings, access code and vanilla frame/panel materials. Factory inventories and custom item data are converted to native item components; the normal item parser handles inventories, players and other item holders. Large-door collision parts are restored in loaded chunks, preserving foreign blocks already occupying the old virtual footprint. Forcefields retain their bounds and shared identity across chunk boundaries. A transient five-second projector selection expires; stored energy is kept.

Already native 1.21.1 worlds keep their existing saved identities and do not run the legacy chunk converter. Rename-only updates do not rotate or replace existing doors.

## Scope and unsupported materials

This converts Malisis data. It does not supply replacements or data fixes for other removed 1.12.2 mods. Custom door materials from another mod require a compatible block/state mapping on the destination version. Vanilla metadata materials are flattened through Minecraft's data fixer; Malisis block metadata uses an oracle exported from the original runtime. A loaded third-party block with unchanged ID and zero metadata can use its default state. Other unsupported materials abort chunk loading with a diagnostic rather than turning the door into air or regenerating its chunk. Resolve that mod's migration before retrying from the original copy.

The native Core supplies the API needed by this Doors port. It does not make arbitrary old MalisisCore consumers run on a new loader. Hardware GPU/shader and production-modpack acceptance remain separate from the isolated conversion tests.

## Validation

The conversion fixture is produced by the original Doors 7.3.0 and Core 6.5.1 in an actual Forge 1.12.2 client. It contains all 47 directly placeable item families, non-default custom materials/settings, a populated Factory, a player custom-door item, a forcefield crossing a chunk boundary, and a distant large door reserved for a second native boot. Acceptance checks inspect actual native blocks and entities, properties, inventories, item parsing, forcefield members and reconstructed mechanism footprints. Additional checks cover signed old position packing, palette width boundaries and rejecting an unsupported material through the actual chunk-upgrade hook. A second server boot loads the previously untouched DataVersion 1343 remote chunk using the retained registry map.
