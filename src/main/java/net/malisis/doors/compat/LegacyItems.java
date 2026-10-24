package net.malisis.doors.compat;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Blocks;

/** Item components are upgraded for players, containers, dropped items and Factory slots alike. */
public final class LegacyItems {
  public static volatile LegacyRegistry active;

  public static CompoundTag fromLegacy(LegacyRegistry registry, CompoundTag old) {
    var input = old.copy();
    var sourceTag = old.getCompound("tag").copy();
    var fixed =
        (CompoundTag)
            DataFixers.getDataFixer()
                .update(
                    References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, input),
                    1343,
                    SharedConstants.getCurrentVersion().getDataVersion().getVersion())
                .getValue();
    // Preserve the original mod-owned fields even if a vanilla item fix does not know them.
    if (old.getString("id").startsWith("malisisdoors:") && !sourceTag.isEmpty()) {
      var components = fixed.getCompound("components");
      var data = components.getCompound("minecraft:custom_data");
      data.merge(sourceTag);
      components.put("minecraft:custom_data", data);
      fixed.put("components", components);
    }
    if (old.contains("Slot")) fixed.put("Slot", old.get("Slot").copy());
    return current(registry, fixed);
  }

  public static CompoundTag current(LegacyRegistry registry, CompoundTag stack) {
    if (registry == null) return stack;
    String id = stack.getString("id");
    var components = stack.getCompound("components");
    var data = components.getCompound("minecraft:custom_data");
    if (data.isEmpty() || data.getInt("malisisCompatItemVersion") >= 1) return stack;
    boolean owned = id.startsWith("malisisdoors:");
    boolean settings = data.contains("openingTime") || data.contains("movement");
    boolean frame = owned && data.contains("block", Tag.TAG_STRING) && data.contains("metadata");
    boolean projector = id.equals("malisisdoors:forcefielditem");
    if (!owned && !id.startsWith("minecraft:")) return stack;
    if (!settings && !frame && !projector) return stack;
    var output = stack.copy();
    components = components.copy();
    data = data.copy();
    if (settings && !data.contains("malisisDoor"))
      data.put("malisisDoor", LegacySettings.configuration(registry, data, false));
    if (frame && !data.contains("frame", Tag.TAG_COMPOUND))
      data.put(
          "frame",
          LegacyStates.material(
              registry, data, "block", "metadata", Blocks.QUARTZ_BLOCK.defaultBlockState()));
    if (projector && data.contains("start")) {
      data.remove("start");
      data.remove("time");
    } // A pre-upgrade five-second selection is expired; stored energy is retained.
    data.putInt("malisisCompatItemVersion", 1);
    components.put("minecraft:custom_data", data);
    output.put("components", components);
    return output;
  }
}
