package net.malisis.doors.checks;

import static net.malisis.doors.checks.ServerVerification.check;

import java.nio.file.*;
import net.malisis.doors.mechanism.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.joml.Matrix4f;

public final class SurfaceVerification {
  public static void run(ServerLevel level) throws Exception {
    var player = FakePlayerFactory.getMinecraft(level);
    int index = 0;
    for (String line :
        Files.readAllLines(Path.of(System.getProperty("malisis.verify.placement"))).subList(1, 9)) {
      var row = line.split(",");
      float yaw = Float.parseFloat(row[0]);
      boolean sneak = Boolean.parseBoolean(row[1]);
      var facing = Direction.byName(row[2]);
      for (var entry : Mechanisms.BLOCKS) {
        var block = entry.get();
        if (!block.spec.usesPlacerFacing()) continue;
        var pos = new BlockPos(1000 + (index % 12) * 10, 64, 40 + (index / 12) * 10);
        index++;
        player.setYRot(yaw);
        player.setShiftKeyDown(sneak);
        player.setPos(pos.getX() + .5, 64, pos.getZ() - .5);
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        var stack = new ItemStack(block);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit =
            new BlockHitResult(
                Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
        check(
            stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
            "surface fixture placement " + block.spec.id());
        var entity = (MechanismEntity) level.getBlockEntity(pos);
        check(
            entity.facing() == facing,
            "legacy player-relative placement "
                + block.spec.id()
                + " yaw="
                + yaw
                + " sneak="
                + sneak);
        var local = new Matrix4f();
        var global = entity.facingPose(new Matrix4f());
        if (block.spec.kind() == MechanismSpec.Kind.GARAGE) {
          var closed = MechanismPose.bounds(new AABB(0, 0, .40625, 1, 1, .59375), global);
          var opened =
              MechanismPose.bounds(
                  new AABB(0, 0, .40625, 1, 1, .59375),
                  new Matrix4f(global).mul(MechanismPose.garage(local, 3, 0, 1)));
          var delta = opened.getCenter().subtract(closed.getCenter());
          var playerFacing = Direction.fromYRot(yaw);
          check(
              (delta.x * playerFacing.getStepX() + delta.z * playerFacing.getStepZ())
                      * (sneak ? -1 : 1)
                  > 1,
              "garage retracts to legacy side");
        }
        if (block.spec.kind() == MechanismSpec.Kind.LARGE
            || block.spec.kind() == MechanismSpec.Kind.CARRIAGE
            || block.spec.kind() == MechanismSpec.Kind.MEDIEVAL) {
          float x = block.spec.kind() == MechanismSpec.Kind.LARGE ? .4f : 1.5f;
          var closed = MechanismPose.bounds(new AABB(x, 1, .85, x + .05, 2, .95), global);
          var opened =
              MechanismPose.bounds(
                  new AABB(x, 1, .85, x + .05, 2, .95),
                  new Matrix4f(global)
                      .mul(MechanismPose.part(local, block.spec.kind(), "left", 1, false)));
          var delta = opened.getCenter().subtract(closed.getCenter());
          var playerFacing = Direction.fromYRot(yaw);
          check(
              (delta.x * playerFacing.getStepX() + delta.z * playerFacing.getStepZ())
                      * (sneak ? -1 : 1)
                  > 0,
              "double door opens to legacy side " + block.spec.id());
        }
      }
    }
    player.setShiftKeyDown(false);
    var garage =
        Mechanisms.BLOCKS.stream()
            .map(e -> e.get())
            .filter(b -> b.spec.kind() == MechanismSpec.Kind.GARAGE)
            .findFirst()
            .orElseThrow();
    for (int i = 0; i < 2; i++) {
      var pos = new BlockPos(900 + i * 2, 64, 0);
      player.setYRot(0);
      for (int y = 0; y < 3; y++) {
        var cell = pos.above(y);
        level.setBlockAndUpdate(
            cell.below(),
            y == 0
                ? Blocks.STONE.defaultBlockState()
                : garage.defaultBlockState().setValue(MechanismBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(
            cell, garage.defaultBlockState().setValue(MechanismBlock.FACING, Direction.NORTH));
      }
      for (int x = -1; x <= 1; x++)
        for (int y = 65; y <= 68; y++)
          level.setBlockAndUpdate(
              new BlockPos(pos.getX() + x, y, 3),
              (i == 0 ? Blocks.RED_CONCRETE : Blocks.BLUE_CONCRETE).defaultBlockState());
    }
    Files.writeString(
        Path.of(System.getProperty("malisis.verify.out"), "surface-server.txt"),
        "PASS "
            + index
            + " actual mechanism item placements matched 1.12.2 yaw/sneak results; garage travel"
            + " matched player-relative legacy side\n");
  }
}
