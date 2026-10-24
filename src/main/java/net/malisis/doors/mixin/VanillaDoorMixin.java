package net.malisis.doors.mixin;

import net.malisis.doors.*;
import net.malisis.doors.vanilla.VanillaAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(DoorBlock.class)
public abstract class VanillaDoorMixin extends Block implements EntityBlock {
  protected VanillaDoorMixin(Properties properties) {
    super(properties);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return VanillaAnimations.enabled(this)
            && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
        ? new DoorBlockEntity(pos, state)
        : null;
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    return VanillaAnimations.enabled(this) && type == MalisisDoors.DOOR_ENTITY.get()
        ? (world, pos, s, tile) -> ((DoorBlockEntity) tile).tick()
        : null;
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return VanillaAnimations.enabled(this) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return VanillaAnimations.enabled(this) && DoorInteractions.find(level, pos, state) != null
        ? Shapes.empty()
        : super.getCollisionShape(state, level, pos, context);
  }

  @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
  private void malisis$shape(
      BlockState state,
      BlockGetter level,
      BlockPos pos,
      CollisionContext context,
      CallbackInfoReturnable<VoxelShape> result) {
    if (VanillaAnimations.enabled(this)) {
      var door = DoorInteractions.find(level, pos, state);
      if (door != null) result.setReturnValue(door.outline(pos));
    }
  }

  @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
  private void malisis$hand(
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      BlockHitResult hit,
      CallbackInfoReturnable<InteractionResult> result) {
    if (VanillaAnimations.enabled(this) && DoorInteractions.find(level, pos, state) != null)
      result.setReturnValue(
          DoorInteractions.hand((DoorBlock) (Object) this, state, level, pos, player));
  }

  @Inject(method = "setOpen", at = @At("HEAD"), cancellable = true)
  private void malisis$open(
      Entity source,
      Level level,
      BlockState state,
      BlockPos pos,
      boolean open,
      CallbackInfo callback) {
    if (VanillaAnimations.enabled(this) && DoorInteractions.find(level, pos, state) != null) {
      DoorInteractions.setOpen((DoorBlock) (Object) this, level, state, pos, source, open);
      callback.cancel();
    }
  }

  @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
  private void malisis$power(
      BlockState state,
      Level level,
      BlockPos pos,
      Block source,
      BlockPos from,
      boolean moving,
      CallbackInfo callback) {
    if (VanillaAnimations.enabled(this) && DoorInteractions.find(level, pos, state) != null) {
      DoorInteractions.neighbor((DoorBlock) (Object) this, state, level, pos);
      callback.cancel();
    }
  }

  @Inject(method = "setPlacedBy", at = @At("TAIL"))
  private void malisis$placed(
      Level level,
      BlockPos pos,
      BlockState state,
      LivingEntity placer,
      ItemStack stack,
      CallbackInfo callback) {
    var door = DoorInteractions.find(level, pos, state);
    if (VanillaAnimations.enabled(this) && door != null) door.loadItem(stack);
  }

  @Override
  protected java.util.List<ItemStack> getDrops(
      BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
    if (VanillaAnimations.enabled(this)
        && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
        && params.getOptionalParameter(
                net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
            instanceof DoorBlockEntity door) return java.util.List.of(door.item());
    return super.getDrops(state, params);
  }

  @Override
  public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
    if (VanillaAnimations.enabled(this)) {
      var door = DoorInteractions.find(level, pos, state);
      if (door != null) return door.item();
    }
    return super.getCloneItemStack(level, pos, state);
  }
}
