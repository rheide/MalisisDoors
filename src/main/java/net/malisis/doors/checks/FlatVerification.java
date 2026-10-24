package net.malisis.doors.checks;

import static net.malisis.doors.checks.ServerVerification.check;

import java.util.*;
import net.malisis.core.collision.GeometryIndex;
import net.malisis.doors.flat.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class FlatVerification {
  public static final List<BlockPos> ROOTS = new ArrayList<>();

  public static void run(ServerLevel level) {
    int index = 0;
    for (var entry : FlatBlocks.BLOCKS)
      for (Direction facing : Direction.Plane.HORIZONTAL)
        for (int top = 0; top < (entry.get() instanceof AnimatedTrapdoor ? 2 : 1); top++) {
          var block = entry.get();
          BlockPos root = new BlockPos(128 + (index % 8) * 4, 64, (index / 8) * 4);
          index++;
          level.removeBlock(root, false);
          level.setBlock(root.below(), Blocks.STONE.defaultBlockState(), 3);
          BlockPos support = root.below();
          Direction clicked = Direction.UP;
          if (block instanceof LadderBlock) {
            support = root.relative(facing.getOpposite());
            clicked = facing;
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
          } else if (top == 1) {
            support = root.above();
            clicked = Direction.DOWN;
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
          }
          var player = FakePlayerFactory.getMinecraft(level);
          player.setYRot(facing.toYRot());
          var stack = new ItemStack(block);
          player.setItemInHand(InteractionHand.MAIN_HAND, stack);
          var point =
              Vec3.atCenterOf(support)
                  .add(clicked.getStepX() * .5, clicked.getStepY() * .5, clicked.getStepZ() * .5);
          var result =
              stack.useOn(
                  new UseOnContext(
                      player,
                      InteractionHand.MAIN_HAND,
                      new BlockHitResult(point, clicked, support, false)));
          check(
              result.consumesAction(),
              "flat item placement " + entry.getId() + " " + facing + " top=" + top);
          check(
              level.getBlockEntity(root) instanceof FlatEntity,
              "flat entity owns renderer " + entry.getId());
          ROOTS.add(root);
          var entity = (FlatEntity) level.getBlockEntity(root);
          if (block instanceof LadderBlock) {
            check(
                level.getBlockState(root).is(net.minecraft.tags.BlockTags.CLIMBABLE),
                "rusty ladder is climbable");
            continue;
          }
          GeometryIndex.of(level).add(entity);
          if (block instanceof TrapDoorBlock)
            check(
                (level.getBlockState(root).getValue(TrapDoorBlock.HALF) == Half.TOP) == (top == 1),
                "trapdoor mount half");
          check(!entity.collisionBoxes().isEmpty(), "flat closed physical geometry");
          level
              .getBlockState(root)
              .useWithoutItem(
                  level,
                  player,
                  new BlockHitResult(Vec3.atCenterOf(root), Direction.NORTH, root, false));
          check(
              level.getBlockState(root).getValue(BlockStateProperties.OPEN), "flat opens by hand");
          level
              .getBlockState(root)
              .useWithoutItem(
                  level,
                  player,
                  new BlockHitResult(Vec3.atCenterOf(root), Direction.NORTH, root, false));
          check(
              !level.getBlockState(root).getValue(BlockStateProperties.OPEN),
              "flat closes by hand");
          BlockPos power = root.relative(Direction.EAST);
          level.setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState());
          check(
              level.getBlockState(root).getValue(BlockStateProperties.OPEN),
              "flat opens by redstone");
          level.removeBlock(power, false);
          check(
              !level.getBlockState(root).getValue(BlockStateProperties.OPEN),
              "flat closes by redstone");
        }
  }
}
