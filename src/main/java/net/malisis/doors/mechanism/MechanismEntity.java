package net.malisis.doors.mechanism;

import java.util.*;
import net.malisis.core.animation.DoorTimeline;
import net.malisis.core.collision.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.joml.Matrix4f;

public final class MechanismEntity extends BlockEntity implements MovingGeometry {
  public final DoorTimeline animation = new DoorTimeline();
  private final AABB fixedBounds;
  public boolean breaking, swingBackward;
  public long swingStarted = Long.MIN_VALUE / 2;
  public BlockState frame = Blocks.QUARTZ_BLOCK.defaultBlockState();
  static long topologyRevision;
  private long topologyTick = Long.MIN_VALUE, topologyVersion;
  private MechanismEntity cachedTop;
  private int cachedHeight;
  private AABB garageBounds;
  private long closeSoundAt = Long.MAX_VALUE;
  private List<AABB> cached = List.of();
  private long collisionTick = Long.MIN_VALUE;
  private boolean collisionOpen;

  public MechanismEntity(BlockPos pos, BlockState state) {
    super(Mechanisms.ENTITY.get(), pos, state);
    fixedBounds = new AABB(pos).inflate(8);
  }

  public MechanismBlock block() {
    return (MechanismBlock) getBlockState().getBlock();
  }

  public MechanismSpec spec() {
    return block().spec;
  }

  public Direction facing() {
    return getBlockState().getValue(MechanismBlock.FACING);
  }

  public boolean isOpen() {
    return getBlockState().getValue(MechanismBlock.OPEN);
  }

  @Override
  public void onLoad() {
    super.onLoad();
    GeometryIndex.of(level).add(this);
  }

  @Override
  public void setRemoved() {
    if (level != null) GeometryIndex.of(level).remove(this);
    super.setRemoved();
  }

  @Override
  public AABB coverage() {
    if (spec().kind() == MechanismSpec.Kind.GARAGE) {
      topology();
      return garageBounds;
    }
    return fixedBounds;
  }

  @Override
  public boolean alive() {
    return !isRemoved() && level != null && level.hasChunkAt(worldPosition);
  }

  public void loadItem(ItemStack stack) {
    var data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      var tag = data.copyTag();
      if (tag.contains("frame"))
        frame =
            NbtUtils.readBlockState(
                level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),
                tag.getCompound("frame"));
    }
    sync();
  }

  public ItemStack item() {
    var stack = new ItemStack(block());
    CompoundTag tag = new CompoundTag();
    tag.put("frame", NbtUtils.writeBlockState(frame));
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    return stack;
  }

  public void sync() {
    setChanged();
    collisionTick = Long.MIN_VALUE;
    if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
  }

  public Matrix4f facingPose(Matrix4f matrix) {
    MechanismPose.facing(matrix, facing());
    if (spec().kind() == MechanismSpec.Kind.SALOON
        && getBlockState().getValue(MechanismBlock.HINGE)
            == net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT)
      matrix.translate(.5f, 0, .5f).rotateY((float) Math.PI).translate(-.5f, 0, -.5f);
    if (spec().kind() == MechanismSpec.Kind.RUSTY_HATCH)
      matrix.translate(.5f, 0, .5f).rotateY((float) -Math.PI / 2).translate(-.5f, 0, -.5f);
    return matrix;
  }

  public Matrix4f partPose(Matrix4f matrix, String name, float p) {
    boolean left =
        getBlockState().getValue(MechanismBlock.HINGE)
            == net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT;
    if (spec().kind() == MechanismSpec.Kind.SALOON)
      return MechanismPose.saloon(matrix, p, left, swingBackward);
    if (spec().kind() == MechanismSpec.Kind.VERTICAL)
      return MechanismPose.vertical(matrix, name, p, left);
    return MechanismPose.part(
        matrix, spec().kind(), name, p, getBlockState().getValue(MechanismBlock.TOP));
  }

  public boolean shouldLadder(BlockPos pos) {
    if (spec().kind() != MechanismSpec.Kind.RUSTY_HATCH) return false;
    if (facing().getAxis() == Direction.Axis.Z && pos.getZ() == worldPosition.getZ()
        || facing().getAxis() == Direction.Axis.X && pos.getX() == worldPosition.getX())
      return false;
    var wall = pos.relative(facing());
    return level.getBlockState(wall).isFaceSturdy(level, wall, facing().getOpposite());
  }

  public void swing(net.minecraft.world.entity.player.Player player) {
    if (level.isClientSide || level.getGameTime() - swingStarted < spec().ticks()) return;
    var delta = player.position().subtract(Vec3.atCenterOf(worldPosition));
    swingBackward = delta.x * facing().getStepX() + delta.z * facing().getStepZ() > 0;
    setOpen(!isOpen());
    sync();
    var side =
        getBlockState().getValue(MechanismBlock.HINGE)
                == net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT
            ? facing().getClockWise()
            : facing().getCounterClockWise();
    var other = level.getBlockEntity(worldPosition.relative(side));
    if (other instanceof MechanismEntity partner && partner.block() == block()) {
      partner.swingBackward = swingBackward;
      partner.setOpen(isOpen());
      partner.swingStarted = swingStarted;
      partner.sync();
    }
  }

  public boolean powered() {
    for (var cell : block().footprint(worldPosition, getBlockState()))
      if (level.hasChunkAt(cell) && level.hasNeighborSignal(cell)) return true;
    return false;
  }

  public void toggle() {
    open(!isOpen());
  }

  public void open(boolean open) {
    if (level == null || level.isClientSide) return;
    if (spec().kind() == MechanismSpec.Kind.GARAGE) {
      Set<BlockPos> visited = new HashSet<>();
      List<MechanismEntity> doors = new ArrayList<>();
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      queue.add(worldPosition);
      boolean powered = false;
      while (!queue.isEmpty()) {
        BlockPos p = queue.removeFirst();
        if (!visited.add(p)) continue;
        if (!level.hasChunkAt(p)
            || !(level.getBlockEntity(p) instanceof MechanismEntity other)
            || other.block() != block()
            || other.facing() != facing()) continue;
        doors.add(other);
        powered |= other.powered();
        queue.add(p.above());
        queue.add(p.below());
        queue.add(p.relative(facing().getClockWise()));
        queue.add(p.relative(facing().getCounterClockWise()));
      }
      for (var door : doors) door.setOpen(open || powered);
    } else setOpen(open);
  }

  private void setOpen(boolean open) {
    if (isOpen() != open) {
      if (spec().kind() == MechanismSpec.Kind.SALOON) swingStarted = level.getGameTime();
      animation.update(open, level.getGameTime(), duration());
      net.malisis.doors.sound.DoorSounds.play(level, worldPosition, soundFamily(), open, false);
      closeSoundAt =
          open
              ? Long.MAX_VALUE
              : level.getGameTime()
                  + (long) Math.ceil(animation.sample(level.getGameTime()) * duration());
      level.setBlock(worldPosition, getBlockState().setValue(MechanismBlock.OPEN, open), 3);
      sync();
    }
  }

  public int duration() {
    return spec().kind() == MechanismSpec.Kind.GARAGE ? columnHeight() * 10 : spec().ticks();
  }

  public float progress(float partial) {
    if (spec().kind() == MechanismSpec.Kind.SALOON)
      return (float)
          Math.max(0, Math.min(1, (level.getGameTime() + partial - swingStarted) / spec().ticks()));
    return animation.update(isOpen(), level.getGameTime() + partial, duration());
  }

  private void topology() {
    if (level == null) {
      cachedTop = this;
      cachedHeight = 1;
      garageBounds = new AABB(worldPosition).inflate(2);
      return;
    }
    long tick = level.getGameTime();
    if (topologyTick == tick && topologyVersion == topologyRevision) return;
    MechanismEntity top = this;
    while (top.worldPosition.getY() + 1 < level.getMaxBuildHeight()
        && level.hasChunkAt(top.worldPosition.above())
        && level.getBlockEntity(top.worldPosition.above()) instanceof MechanismEntity other
        && other.block() == block()
        && other.facing() == facing()) top = other;
    int height = 1;
    while (top.worldPosition.getY() - height >= level.getMinBuildHeight()
        && level.hasChunkAt(top.worldPosition.below(height))
        && level.getBlockEntity(top.worldPosition.below(height)) instanceof MechanismEntity other
        && other.block() == block()
        && other.facing() == facing()) height++;
    for (int down = 0; down < height; down++)
      if (level.getBlockEntity(top.worldPosition.below(down)) instanceof MechanismEntity segment) {
        segment.cachedTop = top;
        segment.cachedHeight = height;
        segment.topologyTick = tick;
        segment.topologyVersion = topologyRevision;
        segment.garageBounds =
            segment == top
                ? new AABB(top.worldPosition)
                    .expandTowards(0, 1, 0)
                    .expandTowards(0, 1 - height, 0)
                    .inflate(height + 1, 1, height + 1)
                : new AABB(segment.worldPosition);
      }
  }

  public MechanismEntity columnTop() {
    topology();
    return cachedTop;
  }

  public int columnHeight() {
    topology();
    return cachedHeight;
  }

  public String soundFamily() {
    return switch (spec().kind()) {
      case CARRIAGE, MEDIEVAL -> "carriage_door";
      case LARGE ->
          spec().id().contains("rusty") || spec().id().contains("iron")
              ? "rustyDoor"
              : "vanilla_door";
      case RUSTY_HATCH -> "rusty_hatch";
      case HITECH -> "pneumatic_door";
      case VERTICAL -> "rusty_hatch";
      case GARAGE -> "glass_door";
      case SALOON -> "silent_door";
    };
  }

  public void tick() {
    if (spec().kind() == MechanismSpec.Kind.GARAGE) {
      topology();
      GeometryIndex.of(level).add(this);
    }
    animation.update(isOpen(), level.getGameTime(), duration());
    if (!level.isClientSide && level.getGameTime() >= closeSoundAt) {
      closeSoundAt = Long.MAX_VALUE;
      if (!isOpen())
        net.malisis.doors.sound.DoorSounds.play(level, worldPosition, soundFamily(), false, true);
    }
  }

  public void breakStructure() {
    if (breaking || level == null || level.isClientSide) return;
    breaking = true;
    for (BlockPos p : block().footprint(worldPosition, getBlockState()))
      if (!p.equals(worldPosition)
          && level.getBlockEntity(p) instanceof PartEntity part
          && part.root.equals(worldPosition)) level.removeBlock(p, false);
  }

  @Override
  public List<AABB> collisionBoxes() {
    return spec().kind() == MechanismSpec.Kind.SALOON ? List.of() : geometryBoxes();
  }

  private List<AABB> geometryBoxes() {
    long tick = level.getGameTime();
    if (tick == collisionTick && collisionOpen == isOpen()) return cached;
    collisionTick = tick;
    collisionOpen = isOpen();
    List<AABB> boxes = new ArrayList<>();
    float p = progress(0);
    Matrix4f global = facingPose(new Matrix4f()), local = new Matrix4f(), total = new Matrix4f();
    switch (spec().kind()) {
      case GARAGE -> {
        var top = columnTop();
        if (top != this) return cached = List.of();
        int count = columnHeight();
        for (int down = 0; down < count; down++) {
          MechanismPose.garage(local, count, down, p);
          total.set(global).mul(local);
          boxes.add(
              MechanismPose.bounds(new AABB(0, 0, .40625, 1, 1, .59375), total)
                  .move(worldPosition));
        }
      }
      case LARGE -> {
        for (boolean right : new boolean[] {false, true}) {
          var name = right ? "right" : "left";
          MechanismPose.part(local, spec().kind(), name, p, false);
          total.set(global).mul(local);
          boxes.add(
              MechanismPose.bounds(new AABB(right ? .5 : -1, 0, .8125, right ? 2 : .5, 3, 1), total)
                  .move(worldPosition));
        }
      }
      case CARRIAGE, MEDIEVAL -> {
        boxes.add(MechanismPose.bounds(new AABB(0, 4, .8125, 4, 5, 1), global).move(worldPosition));
        for (boolean right : new boolean[] {false, true}) {
          MechanismPose.part(local, spec().kind(), right ? "right" : "left", p, false);
          total.set(global).mul(local);
          boxes.add(
              MechanismPose.bounds(new AABB(right ? 2 : .5, 0, .8125, right ? 3.5 : 2, 4, 1), total)
                  .move(worldPosition));
        }
      }
      case HITECH -> {
        for (boolean right : new boolean[] {false, true}) {
          MechanismPose.part(local, spec().kind(), right ? "doorright" : "doorleft", p, false);
          total.set(global).mul(local);
          boxes.add(
              MechanismPose.bounds(
                      new AABB(right ? .5 : -1, 0, .375, right ? 2 : .5, 2, .625), total)
                  .move(worldPosition));
        }
      }
      case SALOON -> {
        partPose(local, "plane", p);
        total.set(global).mul(local);
        boxes.add(
            MechanismPose.bounds(new AABB(0, .25, .46875, 1, 1.75, .53125), total)
                .move(worldPosition));
      }
      case VERTICAL -> {
        partPose(local, "door", p);
        total.set(global).mul(local);
        boxes.add(
            MechanismPose.bounds(new AABB(.0625, 0, 0, .9375, 2, .1875), total)
                .move(worldPosition));
        for (AABB bar :
            List.of(new AABB(0, 0, 0, .0625, 2, .1875), new AABB(.9375, 0, 0, 1, 2, .1875)))
          boxes.add(MechanismPose.bounds(bar, global).move(worldPosition));
      }
      case RUSTY_HATCH -> {
        MechanismPose.part(
            local, spec().kind(), "door", p, getBlockState().getValue(MechanismBlock.TOP));
        total.set(global).mul(local);
        boxes.add(
            MechanismPose.bounds(new AABB(.125, -.0625, .125, 1.875, .203, 1.875), total)
                .move(worldPosition));
        for (AABB frame :
            List.of(
                new AABB(0, 0, 0, .125, .125, 2),
                new AABB(1.875, 0, 0, 2, .125, 2),
                new AABB(.125, 0, 0, 1.875, .125, .125),
                new AABB(.125, 0, 1.875, 1.875, .125, 2))) {
          total.set(global);
          if (getBlockState().getValue(MechanismBlock.TOP)) total.translate(0, .875f, 0);
          boxes.add(MechanismPose.bounds(frame, total).move(worldPosition));
        }
      }
    }
    if (spec().kind() == MechanismSpec.Kind.RUSTY_HATCH)
      for (var cell : block().footprint(worldPosition, getBlockState()))
        if (shouldLadder(cell)) {
          MechanismPose.facing(local, facing().getOpposite());
          boxes.add(MechanismPose.bounds(new AABB(0, 0, 0, 1, 1, .1875), local).move(cell));
        }
    cached = List.copyOf(boxes);
    return cached;
  }

  @Override
  public List<AABB> selectionBoxes() {
    return geometryBoxes();
  }

  @Override
  public BlockPos interactionPosition(Vec3 hit) {
    BlockPos best = worldPosition;
    double distance = Double.POSITIVE_INFINITY;
    for (var p : block().footprint(worldPosition, getBlockState())) {
      double d = p.distToCenterSqr(hit);
      if (d < distance) {
        distance = d;
        best = p;
      }
    }
    return best;
  }

  public VoxelShape selectionShape(BlockPos cell) {
    return net.malisis.core.collision.CellShapes.outline(selectionBoxes(), cell);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    tag.put("frame", NbtUtils.writeBlockState(frame));
    tag.putLong("swingStarted", swingStarted);
    tag.putBoolean("swingBackward", swingBackward);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.loadAdditional(tag, lookup);
    frame =
        tag.contains("frame")
            ? NbtUtils.readBlockState(
                lookup.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),
                tag.getCompound("frame"))
            : Blocks.QUARTZ_BLOCK.defaultBlockState();
    collisionTick = Long.MIN_VALUE;
    swingStarted = tag.contains("swingStarted") ? tag.getLong("swingStarted") : Long.MIN_VALUE / 2;
    swingBackward = tag.getBoolean("swingBackward");
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
    return saveWithoutMetadata(lookup);
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }
}
