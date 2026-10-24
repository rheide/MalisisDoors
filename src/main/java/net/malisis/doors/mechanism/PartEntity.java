package net.malisis.doors.mechanism;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class PartEntity extends BlockEntity {
 public BlockPos root;
 public PartEntity(BlockPos pos,BlockState state){super(Mechanisms.PART_ENTITY.get(),pos,state);root=pos;}
 public void setRoot(BlockPos pos){root=pos.immutable();setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
 public MechanismEntity origin(){return level!=null&&level.hasChunkAt(root)&&level.getBlockEntity(root) instanceof MechanismEntity entity?entity:null;}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);tag.putLong("root",root.asLong());}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);root=BlockPos.of(tag.getLong("root"));}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
 @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
