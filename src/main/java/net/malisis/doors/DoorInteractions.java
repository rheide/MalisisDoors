package net.malisis.doors;

import net.malisis.doors.custom.DoorConfiguration;
import net.malisis.doors.factory.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;

public final class DoorInteractions {
  public static DoorBlockEntity find(BlockGetter level, BlockPos pos, BlockState state) {
    BlockPos root = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    return level.getBlockEntity(root) instanceof DoorBlockEntity door ? door : null;
  }

  public static InteractionResult hand(
      DoorBlock block, BlockState state, Level level, BlockPos pos, Player player) {
    var door = find(level, pos, state);
    if (door == null) return InteractionResult.PASS;
    var config = door.configuration;
    if (!config.code().isEmpty() && !state.getValue(DoorBlock.OPEN)) {
      if (player instanceof ServerPlayer server)
        server.openMenu(
            new SimpleMenuProvider(
                (id, inv, p) -> new CodeMenu(id, inv, door.getBlockPos()),
                Component.literal("Door access code")),
            buffer -> buffer.writeBlockPos(door.getBlockPos()));
      return InteractionResult.sidedSuccess(level.isClientSide);
    }
    if (config.redstone() == DoorConfiguration.Redstone.REDSTONE_ONLY)
      return InteractionResult.PASS;
    if (config.redstone() == DoorConfiguration.Redstone.REDSTONE_LOCK
        && (door.powered() || door.partner() != null && door.partner().powered()))
      return InteractionResult.sidedSuccess(level.isClientSide);
    if (!level.isClientSide)
      setOpen(block, level, state, pos, player, !state.getValue(DoorBlock.OPEN));
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  public static void setOpen(
      DoorBlock block, Level level, BlockState state, BlockPos pos, Entity source, boolean open) {
    if (level.isClientSide || !state.is(block)) return;
    var door = find(level, pos, state);
    if (door == null) return;
    var partner = door.partner();
    setOne(block, level, door, source, open);
    if (partner != null) setOne(block, level, partner, source, open);
  }

  private static void setOne(
      DoorBlock block, Level level, DoorBlockEntity door, Entity source, boolean open) {
    var state = door.getBlockState();
    if (state.getValue(DoorBlock.OPEN) == open) return;
    door.started(open);
    level.setBlock(door.getBlockPos(), state.setValue(DoorBlock.OPEN, open), 10);
    net.malisis.doors.sound.DoorSounds.play(
        level, door.getBlockPos(), door.configuration.sound(), open, false);
    level.gameEvent(
        source, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, door.getBlockPos());
  }

  public static void neighbor(DoorBlock block, BlockState state, Level level, BlockPos pos) {
    if (level.isClientSide) return;
    var door = find(level, pos, state);
    if (door == null) return;
    door.invalidate();
    boolean power = door.powered();
    var current = door.getBlockState();
    if (current.getValue(DoorBlock.POWERED) == power) return;
    level.setBlock(door.getBlockPos(), current.setValue(DoorBlock.POWERED, power), 2);
    var config = door.configuration;
    if (config.redstone() == DoorConfiguration.Redstone.HAND_ONLY
        || config.redstone() == DoorConfiguration.Redstone.REDSTONE_LOCK
        || !config.code().isEmpty()) return;
    var other = door.partner();
    setOpen(
        block,
        level,
        door.getBlockState(),
        door.getBlockPos(),
        null,
        power || other != null && other.powered());
  }
}
