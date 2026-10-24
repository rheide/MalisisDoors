package net.malisis.doors.mixin;

import net.malisis.doors.flat.FlatEntity;
import net.malisis.doors.vanilla.VanillaAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(FenceGateBlock.class)
public abstract class VanillaGateMixin extends HorizontalDirectionalBlock implements EntityBlock {
  protected VanillaGateMixin(Properties properties) {
    super(properties);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return VanillaAnimations.enabled(this) ? new FlatEntity(pos, state) : null;
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return VanillaAnimations.enabled(this) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
  }

  @Inject(method = "getCollisionShape", at = @At("HEAD"), cancellable = true)
  private void malisis$collision(
      BlockState state,
      BlockGetter level,
      BlockPos pos,
      CollisionContext context,
      CallbackInfoReturnable<VoxelShape> result) {
    if (VanillaAnimations.enabled(this) && level.getBlockEntity(pos) instanceof FlatEntity)
      result.setReturnValue(Shapes.empty());
  }

  @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
  private void malisis$shape(
      BlockState state,
      BlockGetter level,
      BlockPos pos,
      CollisionContext context,
      CallbackInfoReturnable<VoxelShape> result) {
    if (VanillaAnimations.enabled(this) && level.getBlockEntity(pos) instanceof FlatEntity entity)
      result.setReturnValue(entity.outline());
  }

  @Inject(method = "useWithoutItem", at = @At("RETURN"))
  private void malisis$hand(
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      BlockHitResult hit,
      CallbackInfoReturnable<InteractionResult> result) {
    if (VanillaAnimations.enabled(this)
        && !level.isClientSide
        && level.getBlockEntity(pos) instanceof FlatEntity entity) entity.syncPartner();
  }

  @Inject(method = "neighborChanged", at = @At("TAIL"))
  private void malisis$power(
      BlockState state,
      Level level,
      BlockPos pos,
      Block source,
      BlockPos from,
      boolean moving,
      CallbackInfo callback) {
    if (VanillaAnimations.enabled(this)
        && !level.isClientSide
        && level.getBlockEntity(pos) instanceof FlatEntity entity) entity.syncPartner();
  }
}
