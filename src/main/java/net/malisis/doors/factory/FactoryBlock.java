package net.malisis.doors.factory;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public final class FactoryBlock extends HorizontalDirectionalBlock implements EntityBlock {
  public FactoryBlock(Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
  }

  @Override
  protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return simpleCodec(FactoryBlock::new);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new FactoryEntity(pos, state);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (player.isShiftKeyDown()) return InteractionResult.PASS;
    if (player instanceof ServerPlayer server
        && level.getBlockEntity(pos) instanceof FactoryEntity factory)
      server.openMenu(
          factory,
          buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeNbt(factory.configuration.save());
            buffer.writeBoolean(factory.createMode);
          });
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
    if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof FactoryEntity factory) {
      Containers.dropContents(level, pos, factory);
      level.updateNeighbourForOutputSignal(pos, this);
    }
    super.onRemove(state, level, pos, next, moving);
  }
}
