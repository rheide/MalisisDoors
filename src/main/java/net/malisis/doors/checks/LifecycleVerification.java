package net.malisis.doors.checks;

import static net.malisis.doors.checks.ServerVerification.check;

import net.malisis.doors.*;
import net.malisis.doors.custom.*;
import net.malisis.doors.mechanism.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class LifecycleVerification {
  public static void run(ServerLevel level) {
    var player = FakePlayerFactory.getMinecraft(level);
    var pos = new BlockPos(512, 64, 0);
    var config =
        FactoryVerification.settings(
            "sliding_door", 0, false, "", DoorConfiguration.Redstone.HAND_ONLY);
    var item = FactoryVerification.custom(level, config);
    var door = FactoryVerification.place(level, pos, item);
    level.setBlock(
        pos,
        door.getBlockState()
            .setValue(
                DoorBlock.HINGE,
                net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT),
        3);
    door.animation.reset(false, level.getGameTime());
    door.invalidate();
    player.noPhysics = false;
    player.setPos(512.5, 64, -1);
    player.move(MoverType.SELF, new Vec3(0, 0, 2));
    check(player.getZ() < 0, "real player movement blocked by closed moving geometry");
    DoorInteractions.setOpen(door.block(), level, door.getBlockState(), pos, null, true);
    door.animation.reset(true, level.getGameTime());
    door.invalidate();
    player.setPos(512.5, 64, -1);
    player.move(MoverType.SELF, new Vec3(0, 0, 2));
    check(player.getZ() > .8, "real player passes opened sliding leaf");
    var box = door.selectionBoxes().getFirst();
    var from = new Vec3(box.getCenter().x, box.getCenter().y, box.minZ - 2);
    var to = new Vec3(box.getCenter().x, box.getCenter().y, box.maxZ + 2);
    var hit =
        level.clip(
            new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    check(
        hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos),
        "displaced leaf ray resolves real interaction anchor");
    var wall = BlockPos.containing(from.add(0, 0, 1));
    level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
    hit =
        level.clip(
            new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    check(hit.getBlockPos().equals(wall), "wall occludes displaced door interaction");
    level.removeBlock(wall, false);
    int index = 0;
    for (var entry : Mechanisms.BLOCKS) {
      var block = entry.get();
      var root = new BlockPos(540 + index * 10, 64, 0);
      index++;
      var state = block.defaultBlockState().setValue(MechanismBlock.TOP, true);
      for (var p : block.footprint(root, state)) level.removeBlock(p, false);
      level.setBlockAndUpdate(root, state);
      block.setPlacedBy(level, root, state, player, new ItemStack(block));
      var entity = (MechanismEntity) level.getBlockEntity(root);
      entity.frame = Blocks.DEEPSLATE_TILES.defaultBlockState();
      var clone = new MechanismEntity(root, state);
      clone.loadWithComponents(
          entity.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
      check(clone.frame == entity.frame, "mechanism frame persists " + block.spec.id());
      var parts = block.footprint(root, state);
      var highest =
          parts.stream().max(java.util.Comparator.comparingInt(BlockPos::getY)).orElseThrow();
      if (parts.size() > 1)
        check(
            ItemStack.isSameItemSameComponents(
                level
                    .getBlockState(highest)
                    .getBlock()
                    .getCloneItemStack(level, highest, level.getBlockState(highest)),
                entity.item()),
            "proxy pick preserves frame " + block.spec.id());
      var power = highest.above();
      level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
      boolean responds =
          block.spec.kind() != MechanismSpec.Kind.CARRIAGE
              && block.spec.kind() != MechanismSpec.Kind.MEDIEVAL
              && block.spec.kind() != MechanismSpec.Kind.RUSTY_HATCH;
      check(entity.isOpen() == responds, "mechanism proxy power policy " + block.spec.id());
      level.removeBlock(power, false);
      check(!entity.isOpen(), "mechanism proxy power removal closes " + block.spec.id());
      if (parts.size() > 1) {
        var part = parts.stream().filter(p -> !p.equals(root)).findFirst().orElseThrow();
        level.destroyBlock(part, false, player);
        check(level.getBlockState(root).isAir(), "proxy break destroys root " + block.spec.id());
        for (var p : parts)
          check(
              level.getBlockState(p).isAir(), "proxy teardown clears structure " + block.spec.id());
      } else level.destroyBlock(root, false, player);
    }
    var garage =
        Mechanisms.BLOCKS.stream()
            .map(e -> e.get())
            .filter(b -> b.spec.kind() == MechanismSpec.Kind.GARAGE)
            .findFirst()
            .orElseThrow();
    BlockPos bottom = new BlockPos(720, 64, 24);
    for (int y = 0; y < 66; y++)
      level.setBlockAndUpdate(bottom.above(y), garage.defaultBlockState());
    var first = (MechanismEntity) level.getBlockEntity(bottom);
    check(first.columnHeight() == 66, "garage columns above 64 segments supported");
    check(
        first.columnTop().getBlockPos().equals(bottom.above(65)),
        "garage top after tall placement");
    level.removeBlock(bottom.above(33), false);
    check(first.columnHeight() == 33, "garage topology invalidates after break");
    for (int y = 0; y < 66; y++) level.removeBlock(bottom.above(y), false);
  }
}
