package net.malisis.doors;

import java.util.*;
import net.malisis.core.animation.DoorTimeline;
import net.malisis.core.collision.*;
import net.malisis.doors.custom.*;
import net.malisis.doors.mechanism.MechanismPose;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.joml.Matrix4f;

public final class DoorBlockEntity extends BlockEntity implements MovingGeometry {
  public final AABB renderBounds;
  public final DoorTimeline animation = new DoorTimeline();
  public DoorConfiguration configuration;
  public boolean customized;
  private long closeSoundAt = Long.MAX_VALUE;
  private long openedAt, collisionTick = Long.MIN_VALUE;
  private boolean collisionOpen, transitionStarted;
  private long neighborhoodTick = Long.MIN_VALUE;
  private BlockState neighborhoodState;
  private DoorBlockEntity cachedPartner;
  private boolean cachedCentered;
  private List<AABB> cached = List.of();

  public DoorBlockEntity(BlockPos pos, BlockState state) {
    super(MalisisDoors.DOOR_ENTITY.get(), pos, state);
    renderBounds = new AABB(pos).inflate(3).expandTowards(0, 3, 0);
    configuration =
        DoorConfiguration.defaults(
            net.malisis.doors.vanilla.VanillaAnimations.spec(state.getBlock()));
    animation.reset(state.getValue(DoorBlock.OPEN), 0);
  }

  public DoorBlock block() {
    return (DoorBlock) getBlockState().getBlock();
  }

  public DoorSpec spec() {
    return net.malisis.doors.vanilla.VanillaAnimations.spec(getBlockState().getBlock());
  }

  public float progress(float partial) {
    return animation.update(
        getBlockState().getValue(DoorBlock.OPEN),
        getLevel().getGameTime() + partial,
        configuration.openingTicks());
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (!transitionStarted)
      animation.reset(getBlockState().getValue(DoorBlock.OPEN), level.getGameTime());
    if (!spec().id().startsWith("minecraft:")
        || net.malisis.doors.vanilla.Settings.modifyVanillaDoors) GeometryIndex.of(level).add(this);
  }

  @Override
  public void setRemoved() {
    if (level != null) GeometryIndex.of(level).remove(this);
    super.setRemoved();
  }

  public void started(boolean open) {
    transitionStarted = true;
    animation.update(open, level.getGameTime(), configuration.openingTicks());
    if (open) openedAt = level.getGameTime();
    closeSoundAt =
        open
            ? Long.MAX_VALUE
            : level.getGameTime()
                + (long)
                    Math.ceil(animation.sample(level.getGameTime()) * configuration.openingTicks());
    invalidate();
    setChanged();
  }

  public void invalidate() {
    collisionTick = Long.MIN_VALUE;
    neighborhoodTick = Long.MIN_VALUE;
  }

  public void loadItem(ItemStack stack) {
    configuration = DoorConfiguration.readItem(stack, level.registryAccess(), configuration);
    customized = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA) != null;
    sync();
  }

  public ItemStack item() {
    var stack = new ItemStack(getBlockState().getBlock());
    if (customized || spec().id().equals("customdoor"))
      DoorConfiguration.writeItem(stack, configuration);
    return stack;
  }

  public void sync() {
    invalidate();
    setChanged();
    if (level != null) {
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
      if (spec().id().equals("customdoor"))
        level.getChunkSource().getLightEngine().checkBlock(worldPosition);
    }
  }

  public boolean powered() {
    return level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above());
  }

  private void neighbors() {
    if (level == null) return;
    long tick = level.getGameTime();
    var state = getBlockState();
    if (tick == neighborhoodTick && state == neighborhoodState) return;
    neighborhoodTick = tick;
    neighborhoodState = state;
    cachedPartner = null;
    cachedCentered = false;
    var facing = state.getValue(DoorBlock.FACING);
    var clockwise = facing.getClockWise();
    var counter = clockwise.getOpposite();
    var side = state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT ? clockwise : counter;
    if (configuration.paired()
        && level.hasChunkAt(worldPosition.relative(side))
        && level.getBlockEntity(worldPosition.relative(side)) instanceof DoorBlockEntity other
        && other.getBlockState().is(state.getBlock())
        && other.getBlockState().getValue(DoorBlock.FACING) == facing
        && other.getBlockState().getValue(DoorBlock.HINGE) != state.getValue(DoorBlock.HINGE)
        && other.configuration.paired()
        && other.configuration.movement().equals(configuration.movement())) cachedPartner = other;
    var a = level.getBlockState(worldPosition.relative(clockwise)).getBlock();
    var b = level.getBlockState(worldPosition.relative(counter)).getBlock();
    cachedCentered =
        a instanceof FenceBlock
            || a instanceof WallBlock
            || a instanceof IronBarsBlock
            || b instanceof FenceBlock
            || b instanceof WallBlock
            || b instanceof IronBarsBlock;
  }

  public DoorBlockEntity partner() {
    neighbors();
    return cachedPartner != null && !cachedPartner.isRemoved() ? cachedPartner : null;
  }

  public boolean centered() {
    neighbors();
    return cachedCentered;
  }

  public Matrix4f globalPose(Matrix4f matrix) {
    MechanismPose.facing(matrix, getBlockState().getValue(DoorBlock.FACING));
    if (centered()) matrix.translate(0, 0, .40625f);
    return matrix;
  }

  public Matrix4f partPose(Matrix4f matrix, int half, float progress) {
    return DoorMotion.pose(
        matrix,
        configuration.movement(),
        progress,
        getBlockState().getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT,
        half,
        partner() != null);
  }

  public void tick() {
    if (level.isClientSide) return;
    if (level.getGameTime() >= closeSoundAt) {
      closeSoundAt = Long.MAX_VALUE;
      if (!getBlockState().getValue(DoorBlock.OPEN))
        net.malisis.doors.sound.DoorSounds.play(
            level, worldPosition, configuration.sound(), false, true);
    }
    boolean open = getBlockState().getValue(DoorBlock.OPEN);
    if (configuration.proximity()) {
      var bounds =
          MechanismPose.bounds(new AABB(0, 0, -2, 1, 2, 2), globalPose(new Matrix4f()))
              .move(worldPosition);
      boolean player =
          !level
              .getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, bounds)
              .isEmpty();
      var other = partner();
      if (!player && other != null) {
        var second =
            MechanismPose.bounds(new AABB(0, 0, -2, 1, 2, 2), other.globalPose(new Matrix4f()))
                .move(other.worldPosition);
        player =
            !level
                .getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, second)
                .isEmpty();
      }
      if (player) {
        if (!open)
          DoorInteractions.setOpen(block(), level, getBlockState(), worldPosition, null, true);
        return;
      }
    }
    if (open
        && configuration.autoCloseTicks() > 0
        && level.getGameTime() - openedAt > configuration.autoCloseTicks())
      DoorInteractions.setOpen(block(), level, getBlockState(), worldPosition, null, false);
  }

  @Override
  public AABB coverage() {
    return renderBounds;
  }

  @Override
  public boolean alive() {
    return !isRemoved()
        && level != null
        && level.hasChunkAt(worldPosition)
        && (!spec().id().startsWith("minecraft:")
            || net.malisis.doors.vanilla.Settings.modifyVanillaDoors);
  }

  private List<AABB> geometry() {
    long tick = level.getGameTime();
    boolean open = getBlockState().getValue(DoorBlock.OPEN);
    if (tick == collisionTick && collisionOpen == open) return cached;
    collisionTick = tick;
    collisionOpen = open;
    float p = progress(0);
    boolean left = getBlockState().getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT;
    if (!DoorMotion.visible(configuration.movement(), p, left)
        && !configuration.movement().startsWith("spinning_")) return cached = List.of();
    List<AABB> boxes = new ArrayList<>(2);
    Matrix4f global = globalPose(new Matrix4f()), local = new Matrix4f(), total = new Matrix4f();
    for (int half = 0; half < 2; half++) {
      partPose(
          local, half, configuration.movement().startsWith("spinning_") && p >= .99999f ? 0 : p);
      total.set(global).mul(local);
      float width =
          configuration.movement().equals("curtain") && half == 0 ? 1 - (1 - 3f / 16) * p : 1;
      boxes.add(
          MechanismPose.bounds(
                  new AABB(left ? 1 - width : 0, 0, 0, left ? 1 : width, 1, 3f / 16), total)
              .move(worldPosition));
    }
    return cached = List.copyOf(boxes);
  }

  @Override
  public List<AABB> collisionBoxes() {
    float p = progress(0);
    boolean left = getBlockState().getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT;
    return DoorMotion.solid(configuration.movement(), p, left) ? geometry() : List.of();
  }

  @Override
  public List<AABB> selectionBoxes() {
    return geometry();
  }

  @Override
  public BlockPos interactionPosition(Vec3 hit) {
    return hit.y >= worldPosition.getY() + 1 ? worldPosition.above() : worldPosition;
  }

  public VoxelShape outline(BlockPos cell) {
    return net.malisis.core.collision.CellShapes.outline(selectionBoxes(), cell);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    tag.put("configuration", configuration.save());
    tag.putBoolean("customized", customized);
    tag.putLong("openedAt", openedAt);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.loadAdditional(tag, lookup);
    configuration = DoorConfiguration.read(tag.getCompound("configuration"), lookup, configuration);
    customized = tag.getBoolean("customized");
    openedAt = tag.getLong("openedAt");
    invalidate();
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
