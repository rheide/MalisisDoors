package net.malisis.doors.compat;

import java.nio.charset.StandardCharsets;
import java.util.*;
import net.malisis.doors.*;
import net.malisis.doors.field.*;
import net.malisis.doors.mechanism.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Restore Malisis cells after vanilla's normal world-format conversion. */
public final class LegacyDoorsUpgrade {
  private final LegacyChunk input;
  private final Map<BlockPos, CompoundTag> states = new LinkedHashMap<>(),
      entities = new LinkedHashMap<>();

  public LegacyDoorsUpgrade(LegacyChunk input) {
    this.input = input;
  }

  private static CompoundTag entity(String id, BlockPos pos) {
    var tag = new CompoundTag();
    tag.putString("id", "malisisdoors:" + id);
    tag.putInt("x", pos.getX());
    tag.putInt("y", pos.getY());
    tag.putInt("z", pos.getZ());
    return tag;
  }

  private void state(BlockPos pos, BlockState state) {
    states.put(pos, NbtUtils.writeBlockState(state));
  }

  public CompoundTag restore(CompoundTag current) {
    for (var cell : input.cells.values()) convert(cell);
    ChunkPalettes.replace(current, states);
    var list = current.getList("block_entities", Tag.TAG_COMPOUND);
    list.removeIf(
        value ->
            entities.containsKey(LegacyChunk.pos((CompoundTag) value))
                || states.containsKey(LegacyChunk.pos((CompoundTag) value)));
    entities.values().forEach(list::add);
    current.put("block_entities", list);
    current.putInt("malisisCompatChunkVersion", 1);
    return current;
  }

  private void convert(LegacyChunk.Cell cell) {
    var pos = cell.pos();
    var origin = input.origins.get(pos);
    var old = input.tile(pos);
    String name = cell.name();
    if (name.equals("malisisdoors:forcefielddoor")) {
      field(pos, origin == null ? pos : origin);
      return;
    }
    if (origin != null && !origin.equals(pos)) {
      state(pos, Mechanisms.PART.get().defaultBlockState());
      var tag = entity("door_part", pos);
      tag.putLong("root", origin.asLong());
      entities.put(pos, tag);
      return;
    }
    var props = new HashMap<>(cell.properties());
    if (props.getOrDefault("half", "").equals("upper")) {
      var below = input.cell(pos.below());
      if (below == null)
        throw new IllegalStateException("Missing legacy lower door half at " + pos);
      var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(name));
      if (block instanceof MechanismBlock) {
        state(pos, Mechanisms.PART.get().defaultBlockState());
        var tag = entity("door_part", pos);
        tag.putLong("root", pos.below().asLong());
        entities.put(pos, tag);
      }
      return;
    }
    BlockState target;
    if (name.startsWith("malisisdoors:")) target = LegacyStates.malisis(name, props);
    else
      target =
          NbtUtils.readBlockState(
              BuiltInRegistries.BLOCK.asLookup(),
              LegacyStates.material(input.registry, name, cell.metadata()));
    var block = target.getBlock();
    if (block instanceof DoorBlock) {
      var upper = input.cell(pos.above());
      if (upper == null)
        throw new IllegalStateException("Missing legacy upper door half at " + pos);
      props.put("hinge", upper.properties().getOrDefault("hinge", "left"));
      props.put("powered", upper.properties().getOrDefault("powered", "false"));
      target =
          target
              .setValue(
                  DoorBlock.HINGE,
                  props.get("hinge").equals("right") ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT)
              .setValue(DoorBlock.POWERED, Boolean.parseBoolean(props.get("powered")))
              .setValue(
                  DoorBlock.OPEN,
                  old.contains("state")
                      ? LegacySettings.open(old)
                      : Boolean.parseBoolean(props.getOrDefault("open", "false")))
              .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
      state(pos, target);
      state(pos.above(), target.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
      var tag = entity("door", pos);
      tag.put("configuration", LegacySettings.configuration(input.registry, old, false));
      tag.putBoolean("customized", true);
      tag.put("malisisLegacySource", old.copy());
      entities.put(pos, tag);
      return;
    }
    if (block instanceof MechanismBlock mechanism) {
      if (props.containsKey("half")) {
        var upper = input.cell(pos.above());
        if (upper != null)
          target =
              target.setValue(
                  MechanismBlock.HINGE,
                  upper.properties().getOrDefault("hinge", "left").equals("right")
                      ? DoorHingeSide.RIGHT
                      : DoorHingeSide.LEFT);
      }
      target = target.setValue(MechanismBlock.OPEN, LegacySettings.open(old));
      state(pos, target);
      var tag = entity("mechanism", pos);
      tag.putBoolean("legacyStructure", true);
      tag.put("malisisLegacySource", old.copy());
      if (mechanism.spec.kind() == MechanismSpec.Kind.LARGE
          || mechanism.spec.kind() == MechanismSpec.Kind.CARRIAGE
          || mechanism.spec.kind() == MechanismSpec.Kind.MEDIEVAL)
        tag.put(
            "frame",
            LegacyStates.material(
                input.registry, old, "block", "metadata", Blocks.QUARTZ_BLOCK.defaultBlockState()));
      entities.put(pos, tag);
      return;
    }
    state(pos, target);
    if (name.equals("malisisdoors:door_factory")) {
      var tag = entity("door_factory", pos);
      tag.putBoolean("createMode", old.getBoolean("isCreate"));
      tag.put("configuration", LegacySettings.configuration(input.registry, old, true));
      var items = new ListTag();
      for (var value : old.getList("Items", Tag.TAG_COMPOUND))
        items.add(LegacyItems.fromLegacy(input.registry, (CompoundTag) value));
      tag.put("Items", items);
      entities.put(pos, tag);
      return;
    }
    if (block instanceof TrapDoorBlock
        || block instanceof FenceGateBlock
        || block instanceof LadderBlock) {
      if (target.hasProperty(BlockStateProperties.OPEN) && old.contains("state")) {
        target = target.setValue(BlockStateProperties.OPEN, LegacySettings.open(old));
        state(pos, target);
      }
      entities.put(pos, entity("flat_door", pos));
      return;
    }
    throw new IllegalStateException("Unsupported legacy Malisis block " + name);
  }

  private void field(BlockPos pos, BlockPos origin) {
    var source = input.tile(origin);
    BlockPos root, max;
    UUID identity;
    boolean opened;
    if (source.getString("id").equals("malisisdoors:forcefield")) {
      root = BlockPos.of(source.getLong("root"));
      max = BlockPos.of(source.getLong("max"));
      identity = source.getUUID("field");
      opened = input.modernState(origin).getCompound("Properties").getString("open").equals("true");
    } else {
      var bounds = source.contains("multiBlock") ? source.getCompound("multiBlock") : source;
      if (!bounds.contains("maxX"))
        throw new IllegalStateException("Missing legacy forcefield bounds at " + origin);
      root =
          origin.offset(
              (int) Math.floor(bounds.getDouble("minX")),
              (int) Math.floor(bounds.getDouble("minY")),
              (int) Math.floor(bounds.getDouble("minZ")));
      max =
          origin.offset(
              (int) Math.ceil(bounds.getDouble("maxX")) - 1,
              (int) Math.ceil(bounds.getDouble("maxY")) - 1,
              (int) Math.ceil(bounds.getDouble("maxZ")) - 1);
      identity =
          UUID.nameUUIDFromBytes(
              (input.dimension + ":" + origin.asLong()).getBytes(StandardCharsets.UTF_8));
      opened = source.getBoolean("opened");
    }
    var axis =
        root.getY() == max.getY()
            ? net.minecraft.core.Direction.Axis.Y
            : root.getX() == max.getX()
                ? net.minecraft.core.Direction.Axis.X
                : net.minecraft.core.Direction.Axis.Z;
    state(
        pos,
        FieldContent.FIELD
            .get()
            .defaultBlockState()
            .setValue(FieldBlock.AXIS, axis)
            .setValue(FieldBlock.OPEN, opened));
    var tag = entity("forcefield", pos);
    tag.putLong("root", root.asLong());
    tag.putLong("max", max.asLong());
    tag.putUUID("field", identity);
    entities.put(pos, tag);
  }
}
