package net.malisis.doors.compat;

import com.mojang.logging.LogUtils;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.*;
import net.minecraft.nbt.*;
import org.slf4j.Logger;

/** Preserve the old Forge ID map before vanilla rewrites level.dat. */
public final class LegacyRegistry {
  private static final Logger LOG = LogUtils.getLogger();
  private static final Map<Path, Optional<LegacyRegistry>> WORLDS = new ConcurrentHashMap<>();
  public final Path root;
  public final Map<Integer, String> blocks;
  public final Map<String, Integer> blockIds;
  public final Map<Integer, String> items;

  private LegacyRegistry(Path root, CompoundTag data) {
    this.root = root;
    blocks = ids(data.getList("blocks", Tag.TAG_COMPOUND));
    items = ids(data.getList("items", Tag.TAG_COMPOUND));
    var reverse = new HashMap<String, Integer>();
    blocks.forEach((id, name) -> reverse.put(name, id));
    blockIds = Map.copyOf(reverse);
  }

  private static Map<Integer, String> ids(ListTag tags) {
    var map = new HashMap<Integer, String>();
    for (var value : tags) {
      var tag = (CompoundTag) value;
      map.put(tag.getInt("V"), tag.getString("K"));
    }
    return Map.copyOf(map);
  }

  public static void release(Path root) {
    WORLDS.remove(root.toAbsolutePath().normalize());
  }

  public static Optional<LegacyRegistry> forWorld(Path root) {
    return WORLDS.computeIfAbsent(root.toAbsolutePath().normalize(), LegacyRegistry::loadWorld);
  }

  public static Optional<LegacyRegistry> forRegion(Path region) {
    for (Path parent = region.toAbsolutePath().normalize().getParent();
        parent != null;
        parent = parent.getParent())
      if (Files.isRegularFile(parent.resolve("level.dat"))
          || Files.isRegularFile(parent.resolve("malisis-compat/legacy-registry.nbt")))
        return forWorld(parent);
    return Optional.empty();
  }

  private static Optional<LegacyRegistry> loadWorld(Path root) {
    Path saved = root.resolve("malisis-compat/legacy-registry.nbt");
    try {
      if (Files.exists(saved)) return Optional.of(new LegacyRegistry(root, readTag(saved)));
      for (String candidate : List.of("level.dat", "level.dat_old")) {
        Path source = root.resolve(candidate);
        if (!Files.isRegularFile(source)) continue;
        var file = readTag(source);
        var registry = file.getCompound("FML").getCompound("Registries");
        var blocks = registry.getCompound("minecraft:blocks").getList("ids", Tag.TAG_COMPOUND);
        boolean malisis = false;
        for (var value : blocks)
          if (((CompoundTag) value).getString("K").startsWith("malisisdoors:")) {
            malisis = true;
            break;
          }
        if (!malisis) continue;
        var data = new CompoundTag();
        data.putInt("format", 1);
        data.put("blocks", blocks.copy());
        data.put(
            "items",
            registry.getCompound("minecraft:items").getList("ids", Tag.TAG_COMPOUND).copy());
        Files.createDirectories(saved.getParent());
        Path backup = saved.getParent().resolve("original-" + candidate);
        if (!Files.exists(backup)) Files.copy(source, backup);
        writeNew(saved, data);
        LOG.info("Preserved legacy Malisis registry mappings in {}", saved);
        return Optional.of(new LegacyRegistry(root, data));
      }
      return Optional.empty();
    } catch (IOException error) {
      throw new IllegalStateException("Cannot preserve legacy Malisis registry for " + root, error);
    }
  }

  private static CompoundTag readTag(Path path) throws IOException {
    return NbtIo.readCompressed(path, NbtAccounter.create(64L * 1024 * 1024));
  }

  public static synchronized void writeNew(Path destination, CompoundTag data) throws IOException {
    if (Files.exists(destination)) return;
    Files.createDirectories(destination.getParent());
    Path temp = Files.createTempFile(destination.getParent(), ".malisis-", ".nbt");
    try {
      NbtIo.writeCompressed(data, temp);
      try {
        Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE);
      } catch (FileAlreadyExistsException ignored) {
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  public void backupChunk(String dimension, CompoundTag raw) {
    var level = raw.getCompound("Level");
    var file =
        root.resolve("malisis-compat/legacy-chunks")
            .resolve(dimension.replaceAll("[^a-zA-Z0-9._-]", "_"))
            .resolve("c." + level.getInt("xPos") + "." + level.getInt("zPos") + ".nbt");
    try {
      writeNew(file, raw);
    } catch (IOException error) {
      throw new IllegalStateException("Cannot back up old Malisis chunk before conversion", error);
    }
  }

  /** Prefer immutable pre-conversion backups when a neighbour may already be saving natively. */
  public CompoundTag sourceChunk(String dimension, Path region, int x, int z) {
    Path backup =
        root.resolve("malisis-compat/legacy-chunks")
            .resolve(dimension.replaceAll("[^a-zA-Z0-9._-]", "_"))
            .resolve("c." + x + "." + z + ".nbt");
    try {
      if (Files.isRegularFile(backup)) return readTag(backup);
      CompoundTag current;
      try {
        current = chunk(region, x, z);
      } catch (IllegalStateException failure) {
        if (Files.isRegularFile(backup)) return readTag(backup);
        throw failure;
      }
      return Files.isRegularFile(backup) ? readTag(backup) : current;
    } catch (IOException failure) {
      throw new IllegalStateException("Cannot read original Malisis neighbour backup", failure);
    }
  }

  /** Read-only neighbour lookup; never asks the server to load or generate chunks. */
  public CompoundTag chunk(Path regionDirectory, int x, int z) {
    Path region =
        regionDirectory.resolve("r." + Math.floorDiv(x, 32) + "." + Math.floorDiv(z, 32) + ".mca");
    if (!Files.isRegularFile(region)) return new CompoundTag();
    try (var file = new RandomAccessFile(region.toFile(), "r")) {
      int slot = Math.floorMod(x, 32) + 32 * Math.floorMod(z, 32);
      file.seek(slot * 4L);
      int location = file.readInt();
      if (location == 0) return new CompoundTag();
      long sector = (location >>> 8) * 4096L;
      file.seek(sector);
      int length = file.readInt();
      if (length < 1 || length > 64 * 1024 * 1024)
        throw new IOException("Invalid legacy region chunk length");
      int compression = file.readUnsignedByte();
      if ((compression & 128) != 0) throw new IOException("External legacy chunks are unsupported");
      byte[] bytes = new byte[length - 1];
      file.readFully(bytes);
      InputStream stream = new ByteArrayInputStream(bytes);
      if (compression == 1) stream = new GZIPInputStream(stream);
      else if (compression == 2) stream = new InflaterInputStream(stream);
      else if (compression != 3) throw new IOException("Unknown legacy chunk compression");
      try (var input = new DataInputStream(stream)) {
        return NbtIo.read(input, NbtAccounter.create(64L * 1024 * 1024));
      }
    } catch (IOException error) {
      throw new IllegalStateException(
          "Cannot inspect neighbouring legacy Malisis chunk " + x + "," + z, error);
    }
  }
}
