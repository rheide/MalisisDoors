package net.malisis.doors.compat;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Change sparse converted cells without disturbing other palette entries or bit packing. */
public final class ChunkPalettes {
  public static CompoundTag get(CompoundTag chunk, BlockPos pos) {
    for (var value : chunk.getList("sections", Tag.TAG_COMPOUND)) {
      var section = (CompoundTag) value;
      if (section.getByte("Y") != (pos.getY() >> 4)) continue;
      var states = section.getCompound("block_states");
      var palette = states.getList("palette", Tag.TAG_COMPOUND);
      if (palette.size() == 1) return palette.getCompound(0);
      if (palette.isEmpty()) return new CompoundTag();
      int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1)),
          per = 64 / bits,
          index = ((pos.getY() & 15) << 8) | ((pos.getZ() & 15) << 4) | (pos.getX() & 15);
      long[] data = states.getLongArray("data");
      return palette.getCompound(
          (int) ((data[index / per] >>> ((index % per) * bits)) & ((1L << bits) - 1)));
    }
    return new CompoundTag();
  }

  public static void replace(CompoundTag chunk, Map<BlockPos, CompoundTag> replacements) {
    Map<Integer, Map<Integer, CompoundTag>> sections = new HashMap<>();
    for (var entry : replacements.entrySet()) {
      var p = entry.getKey();
      sections
          .computeIfAbsent(p.getY() >> 4, k -> new HashMap<>())
          .put(((p.getY() & 15) << 8) | ((p.getZ() & 15) << 4) | (p.getX() & 15), entry.getValue());
    }
    var list = chunk.getList("sections", Tag.TAG_COMPOUND);
    for (var sectionEntry : sections.entrySet()) {
      CompoundTag section = null;
      for (var value : list)
        if (((CompoundTag) value).getByte("Y") == sectionEntry.getKey()) {
          section = (CompoundTag) value;
          break;
        }
      if (section == null) {
        section = new CompoundTag();
        section.putByte("Y", sectionEntry.getKey().byteValue());
        list.add(section);
      }
      var states = section.getCompound("block_states");
      var palette = states.getList("palette", Tag.TAG_COMPOUND);
      if (palette.isEmpty()) {
        var air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
      }
      int oldBits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1)),
          oldPerLong = 64 / oldBits;
      long[] packed = states.getLongArray("data");
      int[] values = new int[4096];
      if (palette.size() > 1) {
        if (packed.length < (4096 + oldPerLong - 1) / oldPerLong)
          throw new IllegalStateException("Invalid upgraded chunk palette");
        for (int i = 0; i < 4096; i++)
          values[i] =
              (int)
                  ((packed[i / oldPerLong] >>> ((i % oldPerLong) * oldBits))
                      & ((1L << oldBits) - 1));
      }
      Map<CompoundTag, Integer> indexes = new HashMap<>();
      for (int i = 0; i < palette.size(); i++) indexes.put(palette.getCompound(i), i);
      for (var entry : sectionEntry.getValue().entrySet()) {
        Integer index = indexes.get(entry.getValue());
        if (index == null) {
          index = palette.size();
          palette.add(entry.getValue().copy());
          indexes.put(entry.getValue(), index);
        }
        values[entry.getKey()] = index;
      }
      int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(palette.size() - 1)),
          perLong = 64 / bits;
      long[] output = new long[(4096 + perLong - 1) / perLong];
      for (int i = 0; i < 4096; i++)
        output[i / perLong] |= (long) values[i] << ((i % perLong) * bits);
      states.put("palette", palette);
      states.putLongArray("data", output);
      section.put("block_states", states);
    }
    chunk.put("sections", list);
    chunk.remove("Heightmaps");
    chunk.putBoolean("isLightOn", false);
  }
}
