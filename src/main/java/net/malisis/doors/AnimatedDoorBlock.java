package net.malisis.doors;

import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class AnimatedDoorBlock extends DoorBlock implements EntityBlock {
  public final DoorSpec spec;

  public AnimatedDoorBlock(DoorSpec spec, BlockBehaviour.Properties properties) {
    super(spec.redstoneOnly() ? BlockSetType.IRON : BlockSetType.OAK, properties);
    this.spec = spec;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new DoorBlockEntity(pos, state) : null;
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return type == MalisisDoors.DOOR_ENTITY.get()
        ? (world, pos, s, tile) -> ((DoorBlockEntity) tile).tick()
        : null;
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    var state = super.getStateForPlacement(context);
    if (state == null) return null;
    var config =
        net.malisis.doors.custom.DoorConfiguration.readItem(
            context.getItemInHand(),
            context.getLevel().registryAccess(),
            net.malisis.doors.custom.DoorConfiguration.defaults(spec));
    if (!config.code().isEmpty()
        || config.redstone() == net.malisis.doors.custom.DoorConfiguration.Redstone.HAND_ONLY
        || config.redstone() == net.malisis.doors.custom.DoorConfiguration.Redstone.REDSTONE_LOCK)
      state = state.setValue(OPEN, false);
    return state;
  }

  @Override
  public void setPlacedBy(
      Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
    super.setPlacedBy(level, pos, state, placer, stack);
    if (level.getBlockEntity(pos) instanceof DoorBlockEntity door) {
      door.loadItem(stack);
      net.malisis.core.collision.GeometryIndex.of(level).add(door);
    }
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    return DoorInteractions.hand(this, state, level, pos, player);
  }

  @Override
  public void setOpen(
      @Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {
    DoorInteractions.setOpen(this, level, state, pos, entity, open);
  }

  @Override
  protected void neighborChanged(
      BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
    DoorInteractions.neighbor(this, state, level, pos);
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    var door = DoorInteractions.find(level, pos, state);
    return door != null ? door.outline(pos) : super.getShape(state, level, pos, context);
  }

  @Override
  public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
    if (!spec.id().equals("customdoor") || state.getValue(HALF) == DoubleBlockHalf.UPPER) return 0;
    var door = DoorInteractions.find(level, pos, state);
    return door == null
        ? 0
        : Math.max(
            door.configuration.frame().getLightEmission(),
            Math.max(
                door.configuration.top().getLightEmission(),
                door.configuration.bottom().getLightEmission()));
  }

  @Override
  public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
    var door = DoorInteractions.find(level, pos, state);
    return door != null ? door.item() : super.getCloneItemStack(level, pos, state);
  }

  @Override
  protected java.util.List<ItemStack> getDrops(
      BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
    var tile =
        params.getOptionalParameter(
            net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
    if (state.getValue(HALF) == DoubleBlockHalf.LOWER && tile instanceof DoorBlockEntity door)
      return java.util.List.of(door.item());
    return super.getDrops(state, params);
  }
}
