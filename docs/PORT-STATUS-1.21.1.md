# MalisisDoors 9.0.1 / MalisisCore 8.0.1 - native 1.21.1 release

The port restores all 49 original creative items on NeoForge 1.21.1. The previous 22-variant alpha was incomplete and is superseded by the final pair of JARs.

| Family | Restored content |
|---|---|
| Standard doors | Wood/iron sliding, jail, shoji, laboratory, factory; sixteen curtain colors |
| Large mechanisms | Eight wood/metal 3x3 doors, carriage, medieval, garage, high-tech |
| Hatches and swing doors | Rusty hatch, vertical hatch, paired contact-operated saloon doors |
| Flat blocks | Five wooden trapdoors, sliding trapdoor, camouflage gate, rusty ladder |
| Factory and accessories | Door Factory, custom door item, forcefield projector, rusty handle |

The actual creative tab contains all 49 items. Multiblock proxies and forcefield member cells remain hidden implementation blocks.

The Factory supports all eighteen original movement choices; frame/top/bottom materials including water, lava, fire and portals; thirteen sound families; opening/auto-close timing; paired leaves; proximity; standard, redstone-only, powered-lock and hand-only policies; and access codes. It creates custom doors and edits existing standard/vanilla door items. Native server menus validate inventory and configuration changes. Incorrect keypad codes are rejected by the server.

All door families support their placement, animation, interaction, collision and breaking behavior. Large structures share root/proxy state, and proxy breaking removes their structure. Rusty shaft ladders depend on their supporting walls. Garage columns share topology scans and support the world build-height range. Carriage/medieval crafting preserves the chosen frame block and consumes five blocks, including stacked 2x2 ingredients. Forcefields retain energy recharge/costs, corner selection expiry, three plane orientations, toggling, removal and persisted field identity.

Vanilla doors, trapdoors and fence gates animate using native baked models. A world setting disables that integration. Ordinary door items have configurable 3D rendering; specialized items retain their 3D models. Original MIT textures, OBJ models, sounds and translations are retained. Cached meshes rebuild on resource reload.

## Validation

The Java 21 build runs 19 Core timeline/cache contract checks and compares 20,736 vertex samples against output from the actual legacy animation engine: 10,944 standard/custom/curtain samples and 9,792 special-mechanism samples. The comparison caught and corrected chained transform ordering, spinning transforms, curtain taper, rusty-hatch pivots/easing and handle timing.

An isolated dedicated server passes 4,322 startup checks plus the asynchronous auto-close/proximity lifecycle checks. Coverage includes real item placement in four facings, paired hinge selection, redstone at roots and proxies, upper-half entity ownership, Factory create/edit/inventory paths, access code validation, actual player collision/passage, displaced ray selection behind an intervening wall, frame crafting consumption through native result slots, 66-segment garage topology, and multiblock teardown.

The isolated client connects to the dedicated server using the built JARs, observes continuous animation, resolves original textures, reloads resources and verifies cache rebuilding. It visits and renders all fifteen mechanism families, eight own flat-block families, all eighteen custom movements, vanilla animation fixtures and all three forcefield planes. Real Factory screen clicks send payloads that consume inputs and create output; real keypad actions reject an incorrect code and open for the correct code. An inventory gallery exercises all 49 items and the 3D item path.

A clean restart passes 751 saved-world checks for doors, multiblock proxies, Factory inventory, the saved access code and every forcefield member identity. Matched performance results are recorded with the release manifest and PERFORMANCE-COMPARISON.md. Measurements use Mesa llvmpipe software rendering and do not establish hardware FPS, GPU cost or shader acceptance.

## Compatibility

Install both final JARs with Java 21 and NeoForge 21.1.264, the tested version. This port uses native Minecraft/NeoForge APIs and is not a Forge 1.12.2 binary. Preserved registry names do not automatically convert old chunk formats, block states or NBT. A separate world migration remains necessary when moving the production 1.12.2 save.

The native Core provides the functionality needed by this Doors implementation. It does not preserve the complete legacy Core Java API or make unrelated old Malisis mods compatible with 1.21.1. Native menus and item presentation follow current Minecraft conventions; this is functional restoration rather than a pixel-identical copy of the old GUI.

## Source and Git history

Doors and Core remain separate local repositories based on the public Ordinastie repositories. Each has the optimized legacy release tagged `1.12.2`. The native branches follow those performance commits and preserve subsequent port commits. No GitHub fork or push was made. Source bundles accompany the release so the complete upstream and local history can be recovered.

## 9.0.1 corrections

See PATCH-9.0.1.md for corrected garage/large-door placement facing, garage-window blending and front-face handle UVs. Both JARs must be updated. Existing affected doors need re-placing to apply the corrected placement rule. Patch validation passes 4,602 server checks, 96 actual placement cases, 128 front/back UV comparisons and the native client workflow; screenshot pixel sampling verifies window blending. The performance table remains the measured 9.0.0 baseline.
