package net.malisis.doors.compat;

import com.google.gson.*;
import com.mojang.serialization.Dynamic;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Decode the actual 1.12 metadata oracle, not guesses about old direction bits. */
public final class LegacyStates {
  private static final JsonObject TABLE = table();
  private static final Map<Integer, CompoundTag> VANILLA = new ConcurrentHashMap<>();

  private static JsonObject table() {
    try (var stream =
        LegacyStates.class.getResourceAsStream(
            "/data/malisisdoors/compat/legacy-block-properties.json")) {
      if (stream == null) throw new IOException("Missing legacy state oracle");
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
          .getAsJsonObject();
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static Map<String, String> properties(String name, int meta) {
    var entries = TABLE.getAsJsonArray(name);
    if (entries == null)
      throw new IllegalArgumentException("No legacy metadata mapping for " + name);
    var result = new HashMap<String, String>();
    entries
        .get(meta & 15)
        .getAsJsonObject()
        .entrySet()
        .forEach(e -> result.put(e.getKey(), e.getValue().getAsString()));
    return result;
  }

  public static BlockState malisis(String name, Map<String, String> props) {
    var id = ResourceLocation.parse(name);
    if (!BuiltInRegistries.BLOCK.containsKey(id))
      throw new IllegalStateException("Unsupported legacy block " + name);
    BlockState state = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
    for (var entry : props.entrySet()) {
      String key =
          switch (entry.getKey()) {
            case "direction" -> "facing";
            case "power" -> "powered";
            case "wall" -> "in_wall";
            default -> entry.getKey();
          };
      var property = state.getBlock().getStateDefinition().getProperty(key);
      if (property != null) state = set(state, property, entry.getValue());
    }
    return state;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static BlockState set(BlockState state, Property property, String text) {
    var value = property.getValue(text);
    if (value.isEmpty())
      throw new IllegalStateException(
          "Cannot map legacy property " + property.getName() + "=" + text);
    return state.setValue(property, (Comparable) value.get());
  }

  public static CompoundTag material(
      LegacyRegistry registry, CompoundTag data, String name, String meta, BlockState fallback) {
    if (!data.contains(name)) return NbtUtils.writeBlockState(fallback);
    String block =
        data.contains(name, Tag.TAG_ANY_NUMERIC)
            ? registry.blocks.get(data.getInt(name))
            : data.getString(name);
    if (block == null || block.isEmpty())
      throw new IllegalStateException("Unknown legacy material " + data.get(name));
    return material(registry, block, data.getInt(meta));
  }

  public static CompoundTag material(LegacyRegistry registry, String name, int metadata) {
    Integer oldId = registry.blockIds.get(name);
    if (name.startsWith("minecraft:") && oldId != null && oldId < 256) {
      int stateId = (oldId << 4) | (metadata & 15);
      return VANILLA
          .computeIfAbsent(
              stateId,
              key -> {
                var flat = BlockStateData.getTag(key).convert(NbtOps.INSTANCE);
                var fixed =
                    DataFixers.getDataFixer()
                        .update(
                            References.BLOCK_STATE,
                            new Dynamic<>(NbtOps.INSTANCE, flat.getValue()),
                            1519,
                            SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                return ((CompoundTag) fixed.getValue()).copy();
              })
          .copy();
    }
    if (name.startsWith("malisisdoors:"))
      return NbtUtils.writeBlockState(malisis(name, properties(name, metadata)));
    var id = ResourceLocation.tryParse(name);
    if (id != null && BuiltInRegistries.BLOCK.containsKey(id) && metadata == 0)
      return NbtUtils.writeBlockState(BuiltInRegistries.BLOCK.get(id).defaultBlockState());
    throw new IllegalStateException(
        "No lossless material mapping for "
            + name
            + " metadata "
            + metadata
            + "; retain the old save and provide its replacement mod/state mapping");
  }
}
