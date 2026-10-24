# Legacy animation oracle

These Java 8 exporters run the actual 1.12.2 MalisisDoors/Core animation engine. They use `AnimationPlanChecks` fixtures from the optimized legacy worktree. The emitted CSVs are checked into `src/test/resources` and consumed by the native Java 21 parity checks.

1. In the legacy worktree, run `compileAnimationChecks writeBenchmarkClasspath` with Java 8.
2. Compile these exporters with that generated classpath plus `legacy/build/animation-checks/classes`.
3. Run `net.malisis.doors.checks.ExportMotion <legacy-motion.csv>` and `net.malisis.doors.checks.ExportMechanisms <legacy-mechanisms.csv>` using Java 8 and the same classpath plus the exporters' class directory.
4. Run `./gradlew verifyMotion verifyMechanisms` in the native worktree using Java 21.

The standard CSV covers 19 movements, both hinges, single and paired leaves, both halves and nine progress samples. The special-mechanism CSV covers 3x3/carriage doors, rusty/vertical hatches and saloon doors, with seventeen progress samples and their applicable hinge, mounting and swing-direction combinations. Comparison tolerance is 0.0001 blocks. These checks test geometry; client screenshots and interaction/collision checks cover the real runtime paths separately.

`ExportDoorSurfaces <placement.csv> <large-door-surfaces.csv>` additionally records actual `DirectionalComponent.PLACER` results for four player yaws and both sneak states, plus the original 3x3 renderer's front/back/edge vertices and UVs. The opt-in server tests use those placement results; client tests compare emitted front/back UVs for all eight variants to the original mesh.
