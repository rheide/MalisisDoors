package net.malisis.doors;
import net.malisis.core.animation.DoorTimeline;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DoorBlock;
public final class DoorBlockEntity extends BlockEntity {
 public final net.minecraft.world.phys.AABB renderBounds;
 public final DoorTimeline animation=new DoorTimeline();
 public DoorBlockEntity(BlockPos pos,BlockState state){super(MalisisDoors.DOOR_ENTITY.get(),pos,state);renderBounds=new net.minecraft.world.phys.AABB(pos).inflate(1.25).expandTowards(0,3,0);}
 public float progress(float partial){return animation.update(getBlockState().getValue(DoorBlock.OPEN),getLevel().getGameTime()+partial,((AnimatedDoorBlock)getBlockState().getBlock()).spec.ticks());}
}
