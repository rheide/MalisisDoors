package net.malisis.doors.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class CodeMenu extends AbstractContainerMenu {
  public final BlockPos position;
  public final ContainerData status = new SimpleContainerData(1);

  public CodeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
    this(id, inventory, data.readBlockPos());
  }

  public CodeMenu(int id, Inventory inventory, BlockPos pos) {
    super(FactoryContent.CODE_MENU.get(), id);
    position = pos;
    addDataSlots(status);
  }

  @Override
  public boolean stillValid(Player player) {
    return player.distanceToSqr(position.getX() + .5, position.getY() + .5, position.getZ() + .5)
        <= 64;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    return ItemStack.EMPTY;
  }
}
