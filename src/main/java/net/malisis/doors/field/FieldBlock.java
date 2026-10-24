package net.malisis.doors.field;

import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public final class FieldBlock extends Block implements EntityBlock {
  public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
  public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

  public FieldBlock(Properties properties) {
    super(properties);
    registerDefaultState(
        stateDefinition.any().setValue(AXIS, Direction.Axis.Z).setValue(OPEN, false));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(AXIS, OPEN);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new FieldEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return !level.isClientSide && type == FieldContent.ENTITY.get()
        ? (world, pos, s, tile) -> ((FieldEntity) tile).tick()
        : null;
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.block();
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    if (state.getValue(OPEN)) return Shapes.empty();
    return switch (state.getValue(AXIS)) {
      case X -> Block.box(7.9, 0, 0, 8.1, 16, 16);
      case Y -> Block.box(0, 7.9, 0, 16, 8.1, 16);
      case Z -> Block.box(0, 0, 7.9, 16, 16, 8.1);
    };
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
    if (!state.is(next.getBlock())
        && !level.isClientSide
        && level.getBlockEntity(pos) instanceof FieldEntity entity) {
      var origin = entity.origin(true);
      if (origin != null && !origin.destroying) origin.destroy();
    }
    super.onRemove(state, level, pos, next, moving);
  }

  @Override
  public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
    return new ItemStack(FieldContent.PROJECTOR.get());
  }
}
