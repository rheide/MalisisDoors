package net.malisis.doors.factory;

import net.malisis.doors.*;
import net.malisis.doors.custom.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FactoryEntity extends BlockEntity implements Container, MenuProvider {
  private final NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
  public DoorConfiguration configuration = DoorConfiguration.defaults(FactoryContent.CUSTOM_SPEC);
  public boolean createMode = true;

  public FactoryEntity(BlockPos pos, BlockState state) {
    super(FactoryContent.ENTITY.get(), pos, state);
  }

  public boolean createDoor() {
    if (level == null || level.isClientSide) return false;
    ItemStack expected;
    if (createMode) {
      if (!DoorConfiguration.material(items.get(0), true)
          || !DoorConfiguration.material(items.get(1), false)
          || !DoorConfiguration.material(items.get(2), false)) return false;
      var c = configuration.validated();
      var actual =
          new DoorConfiguration(
              c.movement(),
              c.sound(),
              c.openingTicks(),
              c.autoCloseTicks(),
              c.paired(),
              c.proximity(),
              c.redstone(),
              c.code(),
              DoorConfiguration.materialState(items.get(0)),
              DoorConfiguration.materialState(items.get(1)),
              DoorConfiguration.materialState(items.get(2)));
      expected = new ItemStack(FactoryContent.CUSTOM_ITEM.get());
      DoorConfiguration.writeItem(expected, actual);
    } else {
      ItemStack input = items.get(3);
      if (!(input.getItem() instanceof BlockItem blockItem)
          || !(blockItem.getBlock() instanceof DoorBlock)) return false;
      var base =
          blockItem.getBlock() instanceof AnimatedDoorBlock animated
              ? DoorConfiguration.defaults(animated.spec)
              : DoorConfiguration.defaults(
                  new DoorSpec(
                      "minecraft:"
                          + net.minecraft.core.registries.BuiltInRegistries.BLOCK
                              .getKey(blockItem.getBlock())
                              .getPath(),
                      "",
                      DoorSpec.Movement.SLIDE,
                      6,
                      blockItem.getBlock() == net.minecraft.world.level.block.Blocks.IRON_DOOR,
                      true));
      var old = DoorConfiguration.readItem(input, level.registryAccess(), base);
      var c = configuration.validated();
      var edited =
          new DoorConfiguration(
              c.movement(),
              c.sound(),
              c.openingTicks(),
              c.autoCloseTicks(),
              c.paired(),
              c.proximity(),
              c.redstone(),
              c.code(),
              old.frame(),
              old.top(),
              old.bottom());
      expected = input.copy();
      DoorConfiguration.writeItem(expected, edited);
    }
    ItemStack output = items.get(4);
    if (!output.isEmpty()
        && (!ItemStack.isSameItemSameComponents(output, expected)
            || output.getCount() + expected.getCount() > output.getMaxStackSize())) return false;
    if (createMode) {
      for (int i = 0; i < 3; i++) items.get(i).shrink(1);
    } else items.set(3, ItemStack.EMPTY);
    if (output.isEmpty()) items.set(4, expected);
    else output.grow(expected.getCount());
    sync();
    return true;
  }

  public void sync() {
    setChanged();
    if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
  }

  @Override
  public int getContainerSize() {
    return 5;
  }

  @Override
  public boolean isEmpty() {
    for (var stack : items) if (!stack.isEmpty()) return false;
    return true;
  }

  @Override
  public ItemStack getItem(int slot) {
    return items.get(slot);
  }

  @Override
  public ItemStack removeItem(int slot, int count) {
    ItemStack result = ContainerHelper.removeItem(items, slot, count);
    if (!result.isEmpty()) sync();
    return result;
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    return ContainerHelper.takeItem(items, slot);
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    items.set(slot, stack);
    stack.limitSize(getMaxStackSize(stack));
    sync();
  }

  @Override
  public boolean stillValid(Player player) {
    return level != null
        && level.getBlockEntity(worldPosition) == this
        && player.distanceToSqr(
                worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5)
            <= 64;
  }

  @Override
  public void clearContent() {
    items.clear();
    sync();
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.malisisdoors.door_factory");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new FactoryMenu(id, inventory, this, worldPosition, configuration, createMode);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    ContainerHelper.saveAllItems(tag, items, lookup);
    tag.put("configuration", configuration.save());
    tag.putBoolean("createMode", createMode);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.loadAdditional(tag, lookup);
    ContainerHelper.loadAllItems(tag, items, lookup);
    configuration =
        DoorConfiguration.read(
            tag.getCompound("configuration"),
            lookup,
            DoorConfiguration.defaults(FactoryContent.CUSTOM_SPEC));
    createMode = !tag.contains("createMode") || tag.getBoolean("createMode");
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
    return saveWithoutMetadata(lookup);
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }
}
