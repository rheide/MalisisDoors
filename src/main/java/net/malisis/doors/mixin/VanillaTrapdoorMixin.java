package net.malisis.doors.mixin;

import net.malisis.doors.flat.FlatEntity;
import net.malisis.doors.vanilla.VanillaAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrapDoorBlock.class)
public abstract class VanillaTrapdoorMixin extends Block implements EntityBlock {
  protected VanillaTrapdoorMixin(Properties properties) {
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

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return VanillaAnimations.enabled(this) && level.getBlockEntity(pos) instanceof FlatEntity
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
    if (VanillaAnimations.enabled(this) && level.getBlockEntity(pos) instanceof FlatEntity entity)
      result.setReturnValue(entity.outline());
  }
}
