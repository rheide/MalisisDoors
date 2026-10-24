package net.malisis.doors.checks;

import static net.malisis.doors.checks.ServerVerification.check;

import java.util.*;
import net.malisis.doors.*;
import net.malisis.doors.custom.*;
import net.malisis.doors.flat.FlatEntity;
import net.malisis.doors.vanilla.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class VanillaVerification {
  public static final List<BlockPos> ROOTS = new ArrayList<>();

  static void place(ServerLevel level, Block block, BlockPos pos) {
    level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
    var player = FakePlayerFactory.getMinecraft(level);
    player.setYRot(Direction.SOUTH.toYRot());
    player.setPos(pos.getX() + .5, 64, pos.getZ() - .5);
    var stack = new ItemStack(block);
    player.setItemInHand(InteractionHand.MAIN_HAND, stack);
    var result =
        stack.useOn(
            new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                    new Vec3(pos.getX() + .25, pos.getY(), pos.getZ() + .5),
                    Direction.UP,
                    pos.below(),
                    false)));
    check(result.consumesAction(), "vanilla item placement " + block);
  }

  public static void run(ServerLevel level) {
    int index = 0;
    var player = FakePlayerFactory.getMinecraft(level);
    for (var block : VanillaAnimations.doors().toList()) {
      BlockPos pos = new BlockPos(256 + (index % 8) * 4, 64, (index / 8) * 4);
      index++;
      place(level, block, pos);
      ROOTS.add(pos);
      check(
          level.getBlockEntity(pos) instanceof DoorBlockEntity,
          "vanilla door owns animated entity " + block);
      check(
          !level.getBlockState(pos.above()).hasBlockEntity()
              && level.getBlockEntity(pos.above()) == null,
          "vanilla upper half has no duplicate entity");
      var door = (DoorBlockEntity) level.getBlockEntity(pos);
      check(door.configuration.movement().equals("rotating_door"), "vanilla default motion");
      check(
          level.getBlockState(pos).getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED,
          "vanilla animated render path");
      var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
      level.getBlockState(pos).useWithoutItem(level, player, hit);
      check(
          level.getBlockState(pos).getValue(DoorBlock.OPEN)
              == ((DoorBlock) block).type().canOpenByHand(),
          "vanilla hand policy");
      ((DoorBlock) block).setOpen(player, level, level.getBlockState(pos), pos, false);
    }
    for (var block : VanillaAnimations.flats().toList()) {
      BlockPos pos = new BlockPos(256 + (index % 8) * 4, 64, (index / 8) * 4);
      index++;
      place(level, block, pos);
      ROOTS.add(pos);
      check(
          level.getBlockEntity(pos) instanceof FlatEntity,
          "vanilla trapdoor/gate owns entity " + block);
      check(
          level.getBlockState(pos).getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED,
          "vanilla flat render path");
    }
    var edited = new ItemStack(Items.OAK_DOOR);
    var config =
        FactoryVerification.settings(
            "sliding_up_door", 0, false, "", DoorConfiguration.Redstone.HAND_ONLY);
    DoorConfiguration.writeItem(edited, config);
    var placed = FactoryVerification.place(level, new BlockPos(292, 64, 0), edited);
    check(
        placed.configuration.equals(config),
        "factory-edited vanilla item placement retains configuration");
    check(
        ItemStack.isSameItemSameComponents(placed.item(), edited),
        "factory-edited vanilla drop retains data");
    Settings.modifyVanillaDoors = false;
    try {
      BlockPos disabled = new BlockPos(292, 64, 8);
      place(level, Blocks.OAK_DOOR, disabled);
      check(
          !level.getBlockState(disabled).hasBlockEntity() && level.getBlockEntity(disabled) == null,
          "disabled vanilla animation creates no entity");
      check(
          level.getBlockState(disabled).getRenderShape() == RenderShape.MODEL,
          "disabled vanilla animation keeps baked rendering");
      level.removeBlock(disabled, false);
      level.removeBlock(disabled.above(), false);
    } finally {
      Settings.modifyVanillaDoors = true;
    }
  }
}
