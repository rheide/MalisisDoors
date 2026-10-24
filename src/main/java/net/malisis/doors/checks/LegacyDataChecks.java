package net.malisis.doors.checks;

import java.nio.file.*;
import java.util.*;
import net.malisis.doors.compat.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.*;

/** Boundary and failure-contract checks run only by the copied-save harness. */
final class LegacyDataChecks {
  static void run(net.minecraft.server.MinecraftServer server) throws Exception {
    for (int x : new int[] {-30000000, -1, 0, 1, 30000000})
      for (int y : new int[] {-64, 0, 64, 255})
        for (int z : new int[] {-30000000, -33, -1, 0, 30000000}) {
          long old =
              ((long) x & 0x3ffffff) << 38 | ((long) y & 0xfff) << 26 | ((long) z & 0x3ffffff);
          LegacyUpgradeVerification.check(
              LegacyChunk.oldPos(old).equals(new BlockPos(x, y, z)), "old position decode");
        }
    CompoundTag chunk = new CompoundTag();
    var replacements = new HashMap<BlockPos, CompoundTag>();
    for (int i = 0; i < 4096; i++) {
      var state = new CompoundTag();
      state.putString("Name", "test:state_" + (i % 17));
      replacements.put(new BlockPos(i & 15, i >> 8, (i >> 4) & 15), state);
    }
    ChunkPalettes.replace(chunk, replacements);
    var changed = new CompoundTag();
    changed.putString("Name", "test:replacement");
    BlockPos last = new BlockPos(15, 15, 15);
    ChunkPalettes.replace(chunk, Map.of(last, changed));
    for (var entry : replacements.entrySet())
      LegacyUpgradeVerification.check(
          ChunkPalettes.get(chunk, entry.getKey())
              .equals(entry.getKey().equals(last) ? changed : entry.getValue()),
          "palette preserves neighbours across 5-bit boundaries");
    Path registryRoot = LegacyItems.active.root;
    var files =
        Files.list(registryRoot.resolve("malisis-compat/legacy-chunks/minecraft_overworld"));
    CompoundTag raw = null;
    try (files) {
      for (Path path : files.toList()) {
        var candidate = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        for (var value : candidate.getCompound("Level").getList("TileEntities", Tag.TAG_COMPOUND)) {
          var tile = (CompoundTag) value;
          if (tile.getString("id").endsWith("customdoortileentity")) {
            tile.putString("frame", "missing_fixture:unsupported_material");
            raw = candidate;
            break;
          }
        }
        if (raw != null) break;
      }
    }
    LegacyUpgradeVerification.check(raw != null, "failure fixture available");
    boolean refused = false;
    try (var storage =
        new ChunkStorage(
            new RegionStorageInfo("malisis-test", Level.OVERWORLD, "chunk"),
            registryRoot.resolve("malisis-compat/failure-test/region"),
            DataFixers.getDataFixer(),
            false)) {
      try {
        storage.upgradeChunkTag(
            Level.OVERWORLD, () -> server.overworld().getDataStorage(), raw, Optional.empty());
      } catch (LegacyUpgradeFailure expected) {
        refused = true;
      }
    }
    LegacyUpgradeVerification.check(
        refused, "unsupported material aborts loading rather than regenerating a chunk");
  }
}
