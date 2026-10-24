package net.malisis.doors.field;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class FieldEntity extends BlockEntity {
  public BlockPos root, max;
  public UUID identity = new UUID(0, 0);
  public boolean destroying;

  public FieldEntity(BlockPos pos, BlockState state) {
    super(FieldContent.ENTITY.get(), pos, state);
    root = max = pos;
  }

  public void configure(BlockPos root, BlockPos max, UUID identity) {
    this.root = root.immutable();
    this.max = max.immutable();
    this.identity = identity;
    sync();
  }

  public boolean controller() {
    return worldPosition.equals(root);
  }

  public AABB bounds() {
    return new AABB(
        root.getX(), root.getY(), root.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
  }

  public int area() {
    return (max.getX() - root.getX() + 1)
        * (max.getY() - root.getY() + 1)
        * (max.getZ() - root.getZ() + 1);
  }

  public FieldEntity origin(boolean load) {
    if (controller()) return this;
    if (level == null || !load && !level.hasChunkAt(root)) return null;
    if (load) level.getChunk(root.getX() >> 4, root.getZ() >> 4);
    return level.getBlockEntity(root) instanceof FieldEntity field
            && field.identity.equals(identity)
        ? field
        : null;
  }

  public void toggle() {
    if (level == null || level.isClientSide) return;
    boolean open = !getBlockState().getValue(FieldBlock.OPEN);
    for (BlockPos pos : BlockPos.betweenClosed(root, max)) {
      if (level.getBlockEntity(pos) instanceof FieldEntity part && part.identity.equals(identity))
        level.setBlock(pos, part.getBlockState().setValue(FieldBlock.OPEN, open), 3);
    }
  }

  public void destroy() {
    if (destroying || level == null || level.isClientSide) return;
    destroying = true;
    for (BlockPos pos : BlockPos.betweenClosed(root, max))
      if (level.getBlockEntity(pos) instanceof FieldEntity part && part.identity.equals(identity))
        level.removeBlock(pos, false);
  }

  public void tick() {
    if (level.getGameTime() % 20 == 0
        && !controller()
        && level.hasChunkAt(root)
        && !(level.getBlockEntity(root) instanceof FieldEntity))
      level.removeBlock(worldPosition, false);
  }

  public void sync() {
    setChanged();
    if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.saveAdditional(tag, lookup);
    tag.putLong("root", root.asLong());
    tag.putLong("max", max.asLong());
    tag.putUUID("field", identity);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
    super.loadAdditional(tag, lookup);
    root = tag.contains("root") ? BlockPos.of(tag.getLong("root")) : worldPosition;
    max = tag.contains("max") ? BlockPos.of(tag.getLong("max")) : root;
    identity = tag.hasUUID("field") ? tag.getUUID("field") : new UUID(0, 0);
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
