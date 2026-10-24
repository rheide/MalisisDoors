package net.malisis.doors.mixin;

import net.malisis.doors.compat.LegacyItems;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ItemStack.class)
public abstract class LegacyItemMixin {
  @ModifyVariable(
      method =
          "parse(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Ljava/util/Optional;",
      at = @At("HEAD"),
      argsOnly = true)
  private static Tag malisis$legacyItem(Tag data) {
    return data instanceof CompoundTag tag ? LegacyItems.current(LegacyItems.active, tag) : data;
  }
}
