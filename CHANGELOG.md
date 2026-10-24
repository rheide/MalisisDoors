# Changelog

## 9.0.0 - Minecraft 1.21.1 / NeoForge

Restore the complete original creative inventory: 22 standard doors and curtains, 15 large/model mechanisms, eight trapdoor/gate/ladder blocks, the Door Factory, custom door item, forcefield projector and rusty handle. Preserve original registry names and MIT assets. Hidden multiblock parts and forcefield cells are deliberately absent from the item tab.

Port all 18 Factory movements, frame/top/bottom materials, special panel materials, four redstone policies, paired leaves, auto-close, proximity and access codes. Native menus synchronize authoritative inventory/configuration and reject incorrect access codes. Restore all sound families and frame-material crafting, including stacked ingredients in the 2x2 grid.

Animate vanilla doors, trapdoors and fence gates through native models, with a world setting to retain ordinary vanilla rendering. Provide configurable 3D item rendering. Dynamic collision, displaced selection and outlines follow moving geometry; walls occlude interaction. Multiblock breaking clears the whole structure. Garage columns share topology scans and support the world's build-height range.

Use bounded, resource-reload-aware immutable meshes, original OBJ models, cached animation state, renderer-owned scratch matrices and native batched render types. Forcefields render one additive quad per loaded controller rather than repeating the field on every member.

This is a native implementation for NeoForge and requires the matching native MalisisCore JAR. Legacy Java APIs and direct 1.12.2 world-file conversion are outside this release's compatibility contract.
