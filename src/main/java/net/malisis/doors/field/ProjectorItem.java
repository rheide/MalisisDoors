package net.malisis.doors.field;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class ProjectorItem extends Item {
  public ProjectorItem(Properties properties) {
    super(properties);
  }

  public static CompoundTag data(ItemStack stack) {
    return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
  }

  public static int energy(ItemStack stack) {
    return Math.max(0, Math.min(2000, data(stack).getInt("energy")));
  }

  public static void energy(ItemStack stack, int value) {
    var tag = data(stack);
    tag.putInt("energy", Math.max(0, Math.min(2000, value)));
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
  }

  public static void clear(ItemStack stack) {
    var tag = data(stack);
    tag.remove("start");
    tag.remove("time");
    tag.remove("dimension");
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
  }

  public static int area(BlockPos first, BlockPos second) {
    long x = Math.abs((long) second.getX() - first.getX()) + 1,
        y = Math.abs((long) second.getY() - first.getY()) + 1,
        z = Math.abs((long) second.getZ() - first.getZ()) + 1;
    if (x > 100 || y > 100 || z > 100 || !(x == 1 && z != 1 || y == 1 || z == 1 && x != 1))
      return -1;
    long area = x * y * z;
    return area > 100 ? -1 : (int) area;
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    Level level = context.getLevel();
    Player player = context.getPlayer();
    ItemStack stack = context.getItemInHand();
    if (player == null) return InteractionResult.FAIL;
    if (level.isClientSide) return InteractionResult.SUCCESS;
    if (level.getBlockEntity(context.getClickedPos()) instanceof FieldEntity part) {
      var origin = part.origin(true);
      if (origin == null) return InteractionResult.FAIL;
      int cost = origin.area() * (player.isShiftKeyDown() ? 20 : 5);
      if (energy(stack) < cost) return InteractionResult.FAIL;
      if (!level.mayInteract(player, origin.root)) return InteractionResult.FAIL;
      energy(stack, energy(stack) - cost);
      if (player.isShiftKeyDown()) origin.destroy();
      else origin.toggle();
      clear(stack);
      return InteractionResult.SUCCESS;
    }
    BlockPos point = context.getClickedPos().relative(context.getClickedFace());
    var tag = data(stack);
    if (!tag.contains("start")
        || !tag.getString("dimension").equals(level.dimension().location().toString())) {
      tag.putLong("start", point.asLong());
      tag.putLong("time", level.getGameTime());
      tag.putString("dimension", level.dimension().location().toString());
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      return InteractionResult.SUCCESS;
    }
    BlockPos first = BlockPos.of(tag.getLong("start"));
    int area = area(first, point);
    if (area <= 0 || energy(stack) < area * 20) {
      clear(stack);
      return InteractionResult.FAIL;
    }
    BlockPos
        min =
            new BlockPos(
                Math.min(first.getX(), point.getX()),
                Math.min(first.getY(), point.getY()),
                Math.min(first.getZ(), point.getZ())),
        max =
            new BlockPos(
                Math.max(first.getX(), point.getX()),
                Math.max(first.getY(), point.getY()),
                Math.max(first.getZ(), point.getZ()));
    for (BlockPos pos : BlockPos.betweenClosed(min, max))
      if (level.isOutsideBuildHeight(pos)
          || !level.hasChunkAt(pos)
          || !level.getBlockState(pos).canBeReplaced()
          || !level.mayInteract(player, pos)
          || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
        clear(stack);
        return InteractionResult.FAIL;
      }
    Direction.Axis axis =
        min.getY() == max.getY()
            ? Direction.Axis.Y
            : min.getX() == max.getX() ? Direction.Axis.X : Direction.Axis.Z;
    UUID identity = UUID.randomUUID();
    var state = FieldContent.FIELD.get().defaultBlockState().setValue(FieldBlock.AXIS, axis);
    for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
      level.setBlock(pos, state, 3);
      if (level.getBlockEntity(pos) instanceof FieldEntity field)
        field.configure(min, max, identity);
    }
    energy(stack, energy(stack) - area * 20);
    clear(stack);
    return InteractionResult.SUCCESS;
  }

  @Override
  public void inventoryTick(
      ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
    if (level.isClientSide) return;
    var tag = data(stack);
    if (tag.contains("start")
        && (level.getGameTime() - tag.getLong("time") > 100
            || !tag.getString("dimension").equals(level.dimension().location().toString())))
      clear(stack);
    int energy = energy(stack);
    if (energy < 2000)
      energy(
          stack,
          energy
              + 5
              + (entity instanceof Player player && player.getAbilities().instabuild ? 15 : 0));
  }

  @Override
  public boolean isBarVisible(ItemStack stack) {
    return true;
  }

  @Override
  public int getBarWidth(ItemStack stack) {
    return Math.round(13f * energy(stack) / 2000);
  }

  @Override
  public int getBarColor(ItemStack stack) {
    return 0x00aaff;
  }

  @Override
  public boolean shouldCauseReequipAnimation(
      ItemStack before, ItemStack after, boolean slotChanged) {
    return before.getItem() != after.getItem();
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    tooltip.add(Component.literal("Energy: " + energy(stack) + " / 2000"));
    tooltip.add(Component.literal("Select two corners; use on a field to toggle"));
    tooltip.add(Component.literal("Sneak-use a field to remove it"));
  }
}
