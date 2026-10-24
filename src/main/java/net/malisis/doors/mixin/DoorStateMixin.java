package net.malisis.doors.mixin;

import net.malisis.doors.vanilla.VanillaAnimations;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** One native entity per leaf; disabled vanilla animation leaves vanilla states entity-free. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class DoorStateMixin {
  @Inject(method = "hasBlockEntity", at = @At("HEAD"), cancellable = true)
  private void malisis$entityOwnership(CallbackInfoReturnable<Boolean> result) {
    var state = (BlockState) (Object) this;
    var block = state.getBlock();
    var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
    boolean vanilla = key != null && key.getNamespace().equals("minecraft");
    if (block instanceof DoorBlock
        && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER
        && (block instanceof net.malisis.doors.AnimatedDoorBlock
            || vanilla && VanillaAnimations.enabled(block))) result.setReturnValue(false);
    else if ((block instanceof DoorBlock
            || block instanceof TrapDoorBlock
            || block instanceof FenceGateBlock)
        && !VanillaAnimations.enabled(block)
        && !VanillaAnimations.ownsEntity(block.getClass())) result.setReturnValue(false);
  }
}
