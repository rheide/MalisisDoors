package net.malisis.doors.checks;

import static net.malisis.doors.checks.ServerVerification.check;

import java.util.*;
import net.malisis.doors.field.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class FieldVerification {
  public static final List<BlockPos> ROOTS = new ArrayList<>();

  static void select(ServerLevel level, ItemStack stack, BlockPos target) {
    var player = FakePlayerFactory.getMinecraft(level);
    var support = target.below();
    level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
    player.setPos(target.getX() + .5, target.getY(), target.getZ() - .5);
    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    var result =
        stack.useOn(
            new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                    Vec3.atCenterOf(support).add(0, .5, 0), Direction.UP, support, false)));
    check(result.consumesAction(), "projector corner selection");
  }

  public static void run(ServerLevel level) {
    var player = FakePlayerFactory.getMinecraft(level);
    int index = 0;
    check(
        ProjectorItem.area(new BlockPos(0, 0, 0), new BlockPos(2, 2, 2)) < 0,
        "projector rejects volumes");
    check(
        ProjectorItem.area(new BlockPos(0, 0, 0), new BlockPos(0, 2, 0)) < 0,
        "projector rejects vertical line");
    for (Direction.Axis axis : Direction.Axis.values()) {
      BlockPos min = new BlockPos(336 + index * 8, 64, 0),
          max =
              min.offset(
                  axis == Direction.Axis.X ? 0 : 2,
                  axis == Direction.Axis.Y ? 0 : 2,
                  axis == Direction.Axis.Z ? 0 : 2);
      index++;
      // End support blocks lie below each corner. For a vertical field the upper
      // corner is selected against a removable temporary support outside the field.
      for (var pos : BlockPos.betweenClosed(min, max)) level.removeBlock(pos, false);
      var stack = new ItemStack(FieldContent.PROJECTOR.get());
      ProjectorItem.energy(stack, 2000);
      select(level, stack, min);
      if (axis != Direction.Axis.Y) {
        var support = max.relative(Direction.SOUTH);
        level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result =
            stack.useOn(
                new UseOnContext(
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(
                        Vec3.atCenterOf(support).add(0, 0, -.5), Direction.NORTH, support, false)));
        check(result.consumesAction(), "projector completes vertical field");
        level.removeBlock(support, false);
      } else select(level, stack, max);
      check(level.getBlockEntity(min) instanceof FieldEntity, "projector creates controller");
      var controller = (FieldEntity) level.getBlockEntity(min);
      ROOTS.add(min);
      check(
          controller.area() == 9 && ProjectorItem.energy(stack) == 1820,
          "projector creation energy");
      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
        check(
            level.getBlockEntity(pos) instanceof FieldEntity field
                && field.root.equals(min)
                && field.identity.equals(controller.identity),
            "field ownership");
        check(
            !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(),
            "closed field collision");
      }
      var click =
          new UseOnContext(
              player,
              InteractionHand.MAIN_HAND,
              new BlockHitResult(Vec3.atCenterOf(min), Direction.NORTH, min, false));
      check(stack.useOn(click).consumesAction(), "projector toggles field");
      check(ProjectorItem.energy(stack) == 1775, "projector toggle energy");
      for (BlockPos pos : BlockPos.betweenClosed(min, max))
        check(
            level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(),
            "open field has no collision");
      var tag = controller.saveWithoutMetadata(level.registryAccess());
      var copy = new FieldEntity(min, level.getBlockState(min));
      copy.loadWithComponents(tag, level.registryAccess());
      check(
          copy.root.equals(min)
              && copy.max.equals(max)
              && copy.identity.equals(controller.identity),
          "field persistence");
      ProjectorItem.energy(stack, 2000);
      player.setShiftKeyDown(true);
      check(stack.useOn(click).consumesAction(), "projector removes field");
      player.setShiftKeyDown(false);
      for (BlockPos pos : BlockPos.betweenClosed(min, max))
        check(level.getBlockState(pos).isAir(), "field removal clears every member");
      check(ProjectorItem.energy(stack) == 1820, "projector removal energy");
      // Leave a fresh closed field in each orientation for the native client fixture.
      ProjectorItem.energy(stack, 2000);
      select(level, stack, min);
      if (axis == Direction.Axis.Y) select(level, stack, max);
      else {
        var support = max.relative(Direction.SOUTH);
        level.setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
        stack.useOn(
            new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                    Vec3.atCenterOf(support).add(0, 0, -.5), Direction.NORTH, support, false)));
        level.removeBlock(support, false);
      }
    }
    var battery = new ItemStack(FieldContent.PROJECTOR.get());
    player.getAbilities().instabuild = false;
    ((ProjectorItem) battery.getItem()).inventoryTick(battery, level, player, 0, false);
    check(ProjectorItem.energy(battery) == 5, "projector survival recharge");
    player.getAbilities().instabuild = true;
    ((ProjectorItem) battery.getItem()).inventoryTick(battery, level, player, 0, false);
    check(ProjectorItem.energy(battery) == 25, "projector creative recharge");
    player.getAbilities().instabuild = false;
  }
}
