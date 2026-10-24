package net.malisis.doors;
import java.util.*;
import org.joml.Matrix4f;
import net.malisis.core.animation.DoorTimeline;
import net.malisis.core.collision.*;
import net.malisis.doors.mechanism.MechanismPose;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class DoorBlockEntity extends BlockEntity implements MovingGeometry {
 public final AABB renderBounds;
 public final DoorTimeline animation=new DoorTimeline();
 private long collisionTick=Long.MIN_VALUE;private boolean collisionOpen;
 private List<AABB> cached=List.of();
 public DoorBlockEntity(BlockPos pos,BlockState state){super(MalisisDoors.DOOR_ENTITY.get(),pos,state);renderBounds=new AABB(pos).inflate(1.25).expandTowards(0,3,0);}
 public AnimatedDoorBlock block(){return (AnimatedDoorBlock)getBlockState().getBlock();}
 public float progress(float partial){return animation.update(getBlockState().getValue(DoorBlock.OPEN),getLevel().getGameTime()+partial,block().spec.ticks());}
 @Override public void onLoad(){super.onLoad();animation.reset(getBlockState().getValue(DoorBlock.OPEN),level.getGameTime());GeometryIndex.of(level).add(this);}
 @Override public void setRemoved(){if(level!=null)GeometryIndex.of(level).remove(this);super.setRemoved();}
 @Override public AABB coverage(){return renderBounds;}
 @Override public boolean alive(){return !isRemoved()&&level!=null&&level.hasChunkAt(worldPosition);}
 @Override public List<AABB> collisionBoxes(){
  long tick=level.getGameTime();boolean open=getBlockState().getValue(DoorBlock.OPEN);if(tick==collisionTick&&collisionOpen==open)return cached;collisionTick=tick;collisionOpen=open;
  float p=progress(0),thickness=3f/16;boolean left=getBlockState().getValue(DoorBlock.HINGE)==DoorHingeSide.LEFT;List<AABB> boxes=new ArrayList<>(2);
  Matrix4f global=MechanismPose.facing(new Matrix4f(),getBlockState().getValue(DoorBlock.FACING));
  for(int half=0;half<2;half++){
   float x=0,y=half,width=1;
   switch(block().spec.movement()){
    case SLIDE->x=(left?1:-1)*(1-thickness)*p;
    case UP->y+=(2-thickness)*p;
    case SPLIT->y+=(half==1?1-thickness:-1.1f)*p;
    case CURTAIN->{width=1-(1-thickness)*p;if(left)x=1-width;}
   }
   boxes.add(MechanismPose.bounds(new AABB(x,y,0,x+width,y+1,thickness),global).move(worldPosition));
  }
  return cached=List.copyOf(boxes);
 }
 @Override public BlockPos interactionPosition(Vec3 hit){return hit.y>=worldPosition.getY()+1?worldPosition.above():worldPosition;}
 public VoxelShape outline(BlockPos cell){VoxelShape shape=Shapes.empty();for(AABB box:collisionBoxes())shape=Shapes.or(shape,Shapes.create(box.move(-cell.getX(),-cell.getY(),-cell.getZ())));return shape;}
}
