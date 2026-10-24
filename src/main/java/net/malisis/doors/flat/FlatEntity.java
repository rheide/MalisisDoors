package net.malisis.doors.flat;

import java.util.*;
import net.malisis.core.animation.DoorTimeline;
import net.malisis.core.collision.*;
import net.malisis.doors.mechanism.MechanismPose;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.joml.Matrix4f;

public final class FlatEntity extends BlockEntity implements MovingGeometry {
  public final DoorTimeline animation = new DoorTimeline();
  private final AABB bounds;
  private long cachedTick = Long.MIN_VALUE;
  private BlockState cachedState;
  private List<AABB> cached = List.of();

  public FlatEntity(BlockPos pos, BlockState state) {
    super(FlatBlocks.ENTITY.get(), pos, state);
    bounds = new AABB(pos).inflate(2);
  }

  public boolean trap() {
    return getBlockState().getBlock() instanceof TrapDoorBlock;
  }

  public boolean ladder() {
    return getBlockState().getBlock() instanceof LadderBlock;
  }

  public boolean sliding() {
    return getBlockState().getBlock() instanceof AnimatedTrapdoor block && block.sliding;
  }

  public float progress(float partial) {
    return ladder()
        ? 0
        : animation.update(
            getBlockState().getValue(BlockStateProperties.OPEN),
            level.getGameTime() + partial,
            sliding() ? 12 : 6);
  }

  public Direction facing() {
    return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (!ladder()
        && (!net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(getBlockState().getBlock())
                .getNamespace()
                .equals("minecraft")
            || net.malisis.doors.vanilla.Settings.modifyVanillaDoors)) {
      animation.reset(getBlockState().getValue(BlockStateProperties.OPEN), level.getGameTime());
      GeometryIndex.of(level).add(this);
    }
  }

  @Override
  public void setRemoved() {
    if (level != null) GeometryIndex.of(level).remove(this);
    super.setRemoved();
  }

  public FlatEntity partner() {
    if (trap() || ladder()) return null;
    for (Direction direction :
        new Direction[] {facing().getClockWise(), facing().getCounterClockWise()}) {
      var neighbor = level.getBlockEntity(worldPosition.relative(direction));
      if (neighbor instanceof FlatEntity other
          && !other.trap()
          && other.getBlockState().is(getBlockState().getBlock())
          && other.facing().getAxis() == facing().getAxis()) return other;
    }
    return null;
  }

  public void syncPartner() {
    var partner = partner();
    if (partner == null) return;
    var state = getBlockState();
    var other = partner.getBlockState();
    boolean open = state.getValue(FenceGateBlock.OPEN);
    if (other.getValue(FenceGateBlock.OPEN) != open)
      level.setBlock(
          partner.worldPosition,
          other.setValue(FenceGateBlock.OPEN, open).setValue(FenceGateBlock.FACING, facing()),
          3);
  }

  public BlockState camouflage() {
    if (trap() || ladder() || !(getBlockState().getBlock() instanceof CamouflageGate))
      return getBlockState();
    Direction a = facing().getClockWise(), b = a.getOpposite();
    var x = worldPosition.relative(a);
    if (level.getBlockState(x).is(getBlockState().getBlock())) x = x.relative(a);
    var y = worldPosition.relative(b);
    if (level.getBlockState(y).is(getBlockState().getBlock())) y = y.relative(b);
    var left = level.getBlockState(x);
    var right = level.getBlockState(y);
    return !left.isAir() && left.equals(right) ? left : getBlockState();
  }

  public Matrix4f pose(Matrix4f matrix, int part, float p) {
    if (trap()) {
      MechanismPose.facing(matrix, facing().getOpposite());
      boolean top = getBlockState().getValue(TrapDoorBlock.HALF) == Half.TOP;
      float y = top ? .8125f : 0;
      if (sliding()) matrix.translate(0, y, .8125f * p);
      else {
        float pivot = top ? .90625f : .09375f;
        matrix
            .translate(0, pivot, .90625f)
            .rotateX((float) Math.toRadians((top ? -90 : 90) * p))
            .translate(0, -pivot, -.90625f)
            .translate(0, y, 0);
      }
    } else if (ladder()) {
      MechanismPose.facing(matrix, facing());
      matrix
          .translate(.5f, 0, .5f)
          .rotateY((float) Math.PI / 2)
          .translate(-.5f, 0, -.5f)
          .translate(-1, 0, 0);
    } else {
      MechanismPose.facing(matrix, facing());
      if (getBlockState().getValue(FenceGateBlock.IN_WALL)) matrix.translate(0, -.1875f, 0);
      var partner = partner();
      boolean left = part == 0;
      if (partner != null) {
        var local = facing().getCounterClockWise();
        left = partner.worldPosition.relative(local).equals(worldPosition);
        MechanismPose.aroundY(matrix, (left ? 90 : -90) * p, left ? .9375f : .0625f, .5f);
      } else MechanismPose.aroundY(matrix, (left ? -90 : 90) * p, left ? .0625f : .9375f, .5f);
    }
    return matrix;
  }

  @Override
  public AABB coverage() {
    return bounds;
  }

  @Override
  public boolean alive() {
    return !isRemoved()
        && level != null
        && level.hasChunkAt(worldPosition)
        && (!net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(getBlockState().getBlock())
                .getNamespace()
                .equals("minecraft")
            || net.malisis.doors.vanilla.Settings.modifyVanillaDoors);
  }

  @Override
  public List<AABB> collisionBoxes() {
    if (ladder()) return List.of();
    long tick = level.getGameTime();
    var state = getBlockState();
    if (tick == cachedTick && cachedState == state) return cached;
    cachedTick = tick;
    cachedState = state;
    float p = progress(0);
    List<AABB> boxes = new ArrayList<>();
    Matrix4f matrix = new Matrix4f();
    if (trap()) {
      pose(matrix, 0, p);
      boxes.add(
          MechanismPose.bounds(new AABB(0, 0, 0, 1, sliding() ? .09375 : .1875, 1), matrix)
              .move(worldPosition));
    } else {
      var pair = partner();
      for (int half = 0; half < (pair == null ? 2 : 1); half++) {
        pose(matrix, half, p);
        boxes.add(
            MechanismPose.bounds(
                    new AABB(
                        pair == null ? half * .5 : 0,
                        .3125,
                        .4375,
                        pair == null ? (half + 1) * .5 : 1,
                        1.5,
                        .5625),
                    matrix)
                .move(worldPosition));
      }
    }
    return cached = List.copyOf(boxes);
  }

  @Override
  public BlockPos interactionPosition(Vec3 hit) {
    return worldPosition;
  }

  public VoxelShape outline() {
    return net.malisis.core.collision.CellShapes.outline(selectionBoxes(), worldPosition);
  }
}
