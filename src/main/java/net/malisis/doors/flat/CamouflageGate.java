package net.malisis.doors.flat;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class CamouflageGate extends FenceGateBlock implements EntityBlock {
  public CamouflageGate(Properties properties) {
    super(WoodType.OAK, properties);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new FlatEntity(pos, state);
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return level.getBlockEntity(pos) instanceof FlatEntity flat
        ? flat.outline()
        : super.getShape(state, level, pos, context);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    var result = super.useWithoutItem(state, level, pos, player, hit);
    if (!level.isClientSide && level.getBlockEntity(pos) instanceof FlatEntity entity)
      entity.syncPartner();
    return result;
  }

  @Override
  protected void neighborChanged(
      BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
    super.neighborChanged(state, level, pos, block, from, moving);
    if (!level.isClientSide && level.getBlockEntity(pos) instanceof FlatEntity entity)
      entity.syncPartner();
  }
}
