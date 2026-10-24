package net.malisis.doors.compat;

import net.malisis.doors.custom.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;

/** Preserve persisted legacy settings without passing them through a modern GUI. */
public final class LegacySettings {
  public static CompoundTag configuration(
      LegacyRegistry registry, CompoundTag old, boolean factory) {
    var out = new CompoundTag();
    out.putString(
        "movement",
        old.contains(factory ? "doorMovement" : "movement")
            ? old.getString(factory ? "doorMovement" : "movement")
            : "rotating_door");
    out.putString(
        "sound",
        old.contains(factory ? "doorSound" : "sound")
            ? old.getString(factory ? "doorSound" : "sound")
            : "vanilla_door");
    out.putInt("openingTicks", old.contains("openingTime") ? old.getInt("openingTime") : 6);
    out.putInt("autoCloseTicks", old.getInt("autoCloseTime"));
    out.putBoolean("paired", old.contains("doubleDoor") ? old.getBoolean("doubleDoor") : true);
    out.putBoolean("proximity", old.getBoolean("proximityDetection"));
    int policy =
        old.contains("redstoneBehavior")
            ? old.getInt("redstoneBehavior")
            : old.getBoolean("redstone") ? 1 : 0;
    if (policy < 0 || policy >= DoorConfiguration.Redstone.values().length)
      throw new IllegalStateException("Unknown legacy redstone policy " + policy);
    out.putString("redstone", DoorConfiguration.Redstone.values()[policy].name());
    out.putString("code", old.getString("code"));
    out.put(
        "frame",
        LegacyStates.material(
            registry, old, "frame", "frameMetadata", Blocks.OAK_PLANKS.defaultBlockState()));
    out.put(
        "top",
        LegacyStates.material(
            registry, old, "topMaterial", "topMaterialMetadata", Blocks.GLASS.defaultBlockState()));
    out.put(
        "bottom",
        LegacyStates.material(
            registry,
            old,
            "bottomMaterial",
            "bottomMaterialMetadata",
            Blocks.GLASS.defaultBlockState()));
    if (!DoorMotion.known(out.getString("movement"))
        || !DoorConfiguration.SOUNDS.contains(out.getString("sound")))
      throw new IllegalStateException(
          "Unknown legacy movement/sound; refusing to discard its configuration: " + out);
    return out;
  }

  public static boolean open(CompoundTag old) {
    int state = old.getInt("state");
    return state == 2 || state == 3;
  }
}
