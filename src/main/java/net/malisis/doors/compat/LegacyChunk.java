package net.malisis.doors.compat;

import java.nio.*;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** An immutable view of old cells/origins captured before vanilla discards unknown numeric IDs. */
public final class LegacyChunk {
  public record Cell(BlockPos pos, String name, int metadata, Map<String, String> properties) {}

  public final LegacyRegistry registry;
  public final CompoundTag raw, level;
  public final Path region;
  public final String dimension;
  public final int x, z;
  private final Map<Long, CompoundTag> neighbours = new HashMap<>();
  public final Map<BlockPos, CompoundTag> tiles = new HashMap<>();
  public final Map<BlockPos, BlockPos> origins = new HashMap<>();
  public final Map<BlockPos, Cell> cells = new LinkedHashMap<>();

  public LegacyChunk(LegacyRegistry registry, CompoundTag raw, Path region, String dimension) {
    this.registry = registry;
    this.raw = raw.copy();
    this.region = region;
    this.dimension = dimension;
    level = this.raw.getCompound("Level");
    x = level.getInt("xPos");
    z = level.getInt("zPos");
    for (Tag value : level.getList("TileEntities", Tag.TAG_COMPOUND)) {
      var tag = (CompoundTag) value;
      tiles.put(pos(tag), tag);
    }
    byte[] bytes = this.raw.getByteArray("malisiscore:multiBlockOrigin");
    if (bytes.length == 0) bytes = level.getByteArray("malisiscore:multiBlockOrigin");
    var data = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    while (data.hasRemaining()) {
      if (data.remaining() < 12)
        throw new IllegalStateException("Truncated legacy multiblock origins");
      var at = oldPos(data.getLong());
      int length = data.getInt();
      if (length != 8 || data.remaining() < length)
        throw new IllegalStateException("Unknown legacy multiblock origin format");
      origins.put(at, oldPos(data.getLong()));
    }
    for (Tag value : level.getList("Sections", Tag.TAG_COMPOUND)) {
      var section = (CompoundTag) value;
      int sy = section.getByte("Y");
      var blocks = section.getByteArray("Blocks");
      if (blocks.length != 4096) continue;
      for (int index = 0; index < 4096; index++) {
        int id = (blocks[index] & 255) | (nibble(section.getByteArray("Add"), index) << 8);
        String name = registry.blocks.get(id);
        if (name != null && name.startsWith("malisisdoors:")) {
          var pos =
              new BlockPos(
                  (x << 4) + (index & 15),
                  (sy << 4) + (index >> 8),
                  (z << 4) + ((index >> 4) & 15));
          cells.put(
              pos,
              new Cell(
                  pos,
                  name,
                  nibble(section.getByteArray("Data"), index),
                  LegacyStates.properties(name, nibble(section.getByteArray("Data"), index))));
        }
      }
    }
    for (var entry : tiles.entrySet())
      if (legacyTile(entry.getValue().getString("id")) && !cells.containsKey(entry.getKey())) {
        var cell = cell(entry.getKey());
        if (cell != null) cells.put(entry.getKey(), cell);
      }
  }

  public boolean relevant() {
    return !cells.isEmpty();
  }

  public static BlockPos pos(CompoundTag tag) {
    return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
  }

  public static boolean legacyTile(String id) {
    return switch (id.substring(id.indexOf(':') + 1).toLowerCase(Locale.ROOT)) {
      case "doortileentity",
              "trapdoortileentity",
              "fencegatetileentity",
              "saloondoortileentity",
              "verticalhatchtileentity",
              "garagedoortileentity",
              "doorfactorytileentity",
              "customdoortileentity",
              "rustyhatchtileentity",
              "door3x3",
              "bigdoortileentity",
              "forcefieldtileentity" ->
          true;
      default -> false;
    };
  }

  /** 1.12 packs X/Y/Z; modern packs X/Z/Y. Never use BlockPos.of on old longs. */
  public static BlockPos oldPos(long value) {
    int y = (int) ((value >>> 26) & 4095);
    if (y >= 2048) y -= 4096;
    return new BlockPos((int) (value >> 38), y, (int) (value << 38 >> 38));
  }

  private static int nibble(byte[] bytes, int index) {
    return bytes.length == 0 ? 0 : ((bytes[index >> 1] & 255) >> ((index & 1) * 4)) & 15;
  }

  private CompoundTag chunk(BlockPos pos) {
    if ((pos.getX() >> 4) == x && (pos.getZ() >> 4) == z) return raw;
    long key = ((long) (pos.getX() >> 4) << 32) ^ (pos.getZ() >> 4 & 0xffffffffL);
    return neighbours.computeIfAbsent(
        key, k -> registry.sourceChunk(dimension, region, pos.getX() >> 4, pos.getZ() >> 4));
  }

  public CompoundTag tile(BlockPos pos) {
    if (tiles.containsKey(pos)) return tiles.get(pos);
    var chunk = chunk(pos);
    var list =
        chunk.contains("Level")
            ? chunk.getCompound("Level").getList("TileEntities", Tag.TAG_COMPOUND)
            : chunk.getList("block_entities", Tag.TAG_COMPOUND);
    for (var value : list) {
      var tag = (CompoundTag) value;
      if (pos(tag).equals(pos)) return tag;
    }
    return new CompoundTag();
  }

  public Cell cell(BlockPos pos) {
    if (cells.containsKey(pos)) return cells.get(pos);
    var chunk = chunk(pos);
    if (!chunk.contains("Level")) return null;
    for (Tag value : chunk.getCompound("Level").getList("Sections", Tag.TAG_COMPOUND)) {
      var section = (CompoundTag) value;
      if (section.getByte("Y") != (pos.getY() >> 4)) continue;
      var blocks = section.getByteArray("Blocks");
      if (blocks.length != 4096) return null;
      int index = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
      int id = (blocks[index] & 255) | (nibble(section.getByteArray("Add"), index) << 8);
      var name = registry.blocks.get(id);
      if (name == null)
        throw new IllegalStateException(
            "Legacy block ID " + id + " absent from the Forge save registry");
      int meta = nibble(section.getByteArray("Data"), index);
      Map<String, String> props;
      if (name.startsWith("malisisdoors:")) props = LegacyStates.properties(name, meta);
      else {
        var state = LegacyStates.material(registry, name, meta);
        var result = new HashMap<String, String>();
        var tag = state.getCompound("Properties");
        for (String key : tag.getAllKeys()) result.put(key, tag.getString(key));
        props = result;
      }
      return new Cell(pos, name, meta, props);
    }
    return null;
  }

  public CompoundTag modernState(BlockPos pos) {
    return ChunkPalettes.get(chunk(pos), pos);
  }

  public CompoundTag vanillaInput() {
    var input = raw.copy();
    var entities = input.getCompound("Level").getList("TileEntities", Tag.TAG_COMPOUND);
    entities.removeIf(tag -> legacyTile(((CompoundTag) tag).getString("id")));
    return input;
  }
}
