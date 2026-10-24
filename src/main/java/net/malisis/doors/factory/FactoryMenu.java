package net.malisis.doors.factory;

import net.malisis.doors.custom.DoorConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.DoorBlock;

public final class FactoryMenu extends AbstractContainerMenu {
  public final Container inventory;
  public final BlockPos position;
  public final DoorConfiguration configuration;
  public final boolean createMode;
  public final ContainerData status = new SimpleContainerData(1);

  public FactoryMenu(int id, Inventory player, RegistryFriendlyByteBuf data) {
    this(
        id,
        player,
        new SimpleContainer(5),
        data.readBlockPos(),
        DoorConfiguration.read(
            data.readNbt(),
            player.player.registryAccess(),
            DoorConfiguration.defaults(FactoryContent.CUSTOM_SPEC)),
        data.readBoolean());
  }

  public FactoryMenu(
      int id,
      Inventory player,
      Container inventory,
      BlockPos position,
      DoorConfiguration configuration,
      boolean createMode) {
    super(FactoryContent.MENU.get(), id);
    this.inventory = inventory;
    this.position = position;
    this.configuration = configuration;
    this.createMode = createMode;
    int[] x = {18, 54, 90, 154, 224};
    for (int i = 0; i < 5; i++) {
      final int slot = i;
      addSlot(
          new Slot(inventory, i, x[i], 160) {
            @Override
            public boolean mayPlace(ItemStack stack) {
              return slot == 4
                  ? false
                  : slot == 3
                      ? stack.getItem() instanceof BlockItem b && b.getBlock() instanceof DoorBlock
                      : DoorConfiguration.material(stack, slot == 0);
            }
          });
    }
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(player, col + row * 9 + 9, 88 + col * 18, 200 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 88 + col * 18, 258));
    addDataSlots(status);
  }

  @Override
  public boolean stillValid(Player player) {
    return inventory instanceof FactoryEntity factory ? factory.stillValid(player) : true;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack empty = ItemStack.EMPTY;
    var slot = slots.get(index);
    if (!slot.hasItem()) return empty;
    var stack = slot.getItem();
    var original = stack.copy();
    if (index < 5) {
      if (!moveItemStackTo(stack, 5, slots.size(), true)) return empty;
    } else if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof DoorBlock) {
      if (!moveItemStackTo(stack, 3, 4, false)) return empty;
    } else {
      if (!moveItemStackTo(stack, 0, 3, false)) return empty;
    }
    if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
    else slot.setChanged();
    if (stack.getCount() == original.getCount()) return empty;
    slot.onTake(player, stack);
    return original;
  }
}
