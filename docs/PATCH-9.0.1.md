# Doors 9.0.1 / Core 8.0.1

Fixes three differences reported after the initial native release:

- Garage doors and large mechanisms now use the original player-relative facing rule. A normal placement faces toward the player; sneak-placement reverses it. Garage retraction and the large double-door swing therefore use the same side as 1.12.2. This applies to garage, all eight 3x3 variants, carriage, medieval and high-tech doors. Door collision and selection use the same facing as rendering.
- Garage windows retain their original alpha-200 texture and now use native blended rendering in both world and item views, restoring transparency.
- The 3x3 front-face UVs now keep each leaf's handles at the centre seam, matching the reverse face and the original 1.12.2 mesh. This correction is in Core's cached cuboid mesh builder.

Update BOTH JARs: MalisisDoors 9.0.1 requires MalisisCore 8.0.1.
Existing placed blocks retain their saved orientation. Re-place affected doors once to apply the corrected placement facing; there is no automatic world rewrite.

Validation uses eight player yaw/sneak combinations exported by the actual 1.12.2 placement code, real native item placements and opening-side checks, and emitted front/back UVs compared with the original large-door meshes. The regression scene also places red and blue walls behind garage windows for a native client transparency check. The build and full dedicated-server/client checks pass (4,602 server checks, 96 additional placement cases, 128 front/back UV comparisons). Screenshot pixel sampling confirms red/blue backdrop colour passes through the garage windows.

The performance comparison remains the measured 9.0.0 release baseline. This visual/correctness patch has not been reprofiled.
