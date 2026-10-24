package net.malisis.doors.mechanism;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
/** Common transforms: rendering, selection and physical collision use the same pose. */
public final class MechanismPose {
 public static Matrix4f facing(Matrix4f matrix,Direction facing){return matrix.identity().translate(.5f,0,.5f).rotateY((float)Math.toRadians(-facing.toYRot())).translate(-.5f,0,-.5f);}
 public static BlockPos offset(Direction facing,int x,int y,int z){return switch(facing){case NORTH->new BlockPos(-x,y,-z);case WEST->new BlockPos(-z,y,x);case EAST->new BlockPos(z,y,-x);default->new BlockPos(x,y,z);};}
 public static void aroundY(Matrix4f matrix,float angle,float x,float z){matrix.translate(x,0,z).rotateY((float)Math.toRadians(angle)).translate(-x,0,-z);}
 public static Matrix4f part(Matrix4f matrix,MechanismSpec.Kind kind,String name,float p,boolean top){
  matrix.identity();boolean right=name.contains("right");
  switch(kind){
   case LARGE->{if(!name.contains("frame"))aroundY(matrix,(right?-90:90)*p,right?1.90625f:-.90625f,.90625f);}
   case CARRIAGE,MEDIEVAL->{if(name.equals("left")||name.equals("right"))aroundY(matrix,(right?-105:105)*p,right?3.5f:.5f,.90625f);}
   case HITECH->{
    float slide=Math.max(0,Math.min(1,(p*21-6)/15));float latch=Math.min(1,p*21/6);
    if(name.contains("right"))matrix.translate(1.3125f*slide,0,0);
    else if(name.contains("left"))matrix.translate(-1.3125f*slide,0,0);
    else if(name.contains("latch")){matrix.translate(name.contains("top")?-1.3125f*slide:1.3125f*slide,0,0);matrix.translate(.5f,1,.5f).rotateZ((float)Math.toRadians(90*latch)).translate(-.5f,-1,-.5f);}
   }
   case RUSTY_HATCH->{
    if(top)matrix.translate(0,.875f,0);
    if(name.startsWith("door")||name.startsWith("handle")){float swing=Math.max(0,(p-.35f)/.65f);matrix.translate(.125f,0,0).rotateZ((float)Math.toRadians(-105*swing)).translate(-.125f,0,0);}
    if(name.startsWith("handle")){float spin=Math.min(1,p/.35f);matrix.translate(1,0,1).rotateY((float)Math.toRadians(360*spin)).translate(-1,0,-1);}
   }
   default->{}
  }
  return matrix;
 }
 public static AABB bounds(AABB box,Matrix4f matrix){double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX,maxZ=maxX;Vector3f point=new Vector3f();for(int i=0;i<8;i++){
  matrix.transformPosition((float)((i&1)==0?box.minX:box.maxX),(float)((i&2)==0?box.minY:box.maxY),(float)((i&4)==0?box.minZ:box.maxZ),point);
  minX=Math.min(minX,point.x);minY=Math.min(minY,point.y);minZ=Math.min(minZ,point.z);maxX=Math.max(maxX,point.x);maxY=Math.max(maxY,point.y);maxZ=Math.max(maxZ,point.z);
 }return new AABB(minX,minY,minZ,maxX,maxY,maxZ);}
 public static Matrix4f garage(Matrix4f matrix,int count,int down,float progress){
  // A segment rises to the lintel, turns through 90 degrees, then travels under the ceiling.
  float elapsed=progress*count,up=Math.min(down,elapsed),turn=Math.max(0,Math.min(1,elapsed-down)),horizontal=Math.max(0,elapsed-down-1);
  matrix.identity().translate(0,-down+up,0);
  matrix.translate(0,turn,0).translate(0,0,.5f).rotateX((float)Math.toRadians(-90*turn)).translate(0,0,-.5f);
  matrix.translate(0,horizontal,0);return matrix;
 }
}
