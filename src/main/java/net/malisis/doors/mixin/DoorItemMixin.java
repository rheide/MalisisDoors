package net.malisis.doors.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.malisis.doors.client.DoorItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class DoorItemMixin {
  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void malisis$item(
      ItemStack item,
      ItemDisplayContext context,
      boolean left,
      PoseStack stack,
      MultiBufferSource buffers,
      int light,
      int overlay,
      BakedModel model,
      CallbackInfo callback) {
    if (!item.isEmpty() && DoorItems.accepts(item)) {
      DoorItems.render(item, context, left, stack, buffers, light, overlay);
      callback.cancel();
    }
  }
}
