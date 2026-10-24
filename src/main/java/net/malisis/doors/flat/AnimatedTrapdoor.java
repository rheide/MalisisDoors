package net.malisis.doors.flat;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public final class AnimatedTrapdoor extends TrapDoorBlock implements EntityBlock {
  public final String texture;
  public final boolean sliding;

  public AnimatedTrapdoor(String texture, boolean sliding, Properties properties) {
    super(BlockSetType.OAK, properties);
    this.texture = texture;
    this.sliding = sliding;
  }

  @Override
  protected void playSound(
      @javax.annotation.Nullable net.minecraft.world.entity.player.Player player,
      Level level,
      BlockPos pos,
      boolean open) {
    if (sliding) {
      net.malisis.doors.sound.DoorSounds.play(level, pos, "pneumatic_door", open, false);
      level.gameEvent(
          player,
          open
              ? net.minecraft.world.level.gameevent.GameEvent.BLOCK_OPEN
              : net.minecraft.world.level.gameevent.GameEvent.BLOCK_CLOSE,
          pos);
    } else super.playSound(player, level, pos, open);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new FlatEntity(pos, state);
  }

  @Override
  protected RenderShape getRenderShape(BlockState state) {
    return RenderShape.ENTITYBLOCK_ANIMATED;
  }

  @Override
  protected VoxelShape getCollisionShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return Shapes.empty();
  }

  @Override
  protected VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return level.getBlockEntity(pos) instanceof FlatEntity flat
        ? flat.outline()
        : super.getShape(state, level, pos, context);
  }
}
