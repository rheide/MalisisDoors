package net.malisis.doors.checks;
import net.malisis.doors.renderer.DoorAnimationPlans;
import net.malisis.doors.movement.*;
import net.malisis.doors.tileentity.*;
import net.malisis.doors.*;
import net.malisis.core.renderer.*;
import net.malisis.core.renderer.animation.*;
import net.malisis.core.renderer.element.*;
import net.malisis.core.renderer.element.shape.Cube;
import net.malisis.core.renderer.model.MalisisModel;
import net.malisis.core.util.Timer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;

public final class AnimationPlanChecks {
 static int comparisons;
 static class Data {boolean hinge,opened,top;int metadata;EnumFacing facing=EnumFacing.SOUTH;DoorTileEntity partner;}
 interface Controlled {Data config();}
 static class Fixture extends SaloonDoorTileEntity implements Controlled {
  final Data data=new Data();
  {descriptor=new DoorDescriptor();setPos(new BlockPos(0,64,0));}
  public Data config(){return data;}
  public DoorDescriptor getDescriptor(){return descriptor;}
  public boolean isHingeLeft(){return data.hinge;}
  public boolean isOpened(){return data.opened;}
  public EnumFacing getDirection(){return data.facing;}
  public int getBlockMetadata(){return data.metadata;}
  public DoorTileEntity getDoubleDoor(){return data.partner;}
}
 static class FixtureRusty extends RustyHatchTileEntity implements Controlled {
  final Data data=new Data();
  {descriptor=new DoorDescriptor();setPos(new BlockPos(0,64,0));}
  public Data config(){return data;}
  public DoorDescriptor getDescriptor(){return descriptor;}
  public boolean isHingeLeft(){return data.hinge;}
  public boolean isOpened(){return data.opened;}
  public EnumFacing getDirection(){return data.facing;}
  public int getBlockMetadata(){return data.metadata;}
  public DoorTileEntity getDoubleDoor(){return data.partner;}
public boolean isTop(){return data.top;}}
 static class FixtureTrap extends TrapDoorTileEntity implements Controlled {
  final Data data=new Data();
  {descriptor=new DoorDescriptor();setPos(new BlockPos(0,64,0));}
  public Data config(){return data;}
  public DoorDescriptor getDescriptor(){return descriptor;}
  public boolean isHingeLeft(){return data.hinge;}
  public boolean isOpened(){return data.opened;}
  public EnumFacing getDirection(){return data.facing;}
  public int getBlockMetadata(){return data.metadata;}
  public DoorTileEntity getDoubleDoor(){return data.partner;}
public boolean isTop(){return data.top;}}
 static class Fixed extends Timer {long elapsed;Fixed(long time){super(0);elapsed=time;}public long elapsedTime(){return elapsed;}}
 static MalisisModel model(){MalisisModel m=new MalisisModel();for(String name:new String[]{"bottom","top","left","right","Door","Handle4","door","handle"}){Shape s=new Cube();if(name.equals("top"))s.translate(0,1,0);m.addShape(name,s);}m.storeState();return m;}
 static long fingerprint(MalisisModel m,RenderParameters p){long h=p.alpha.get();List<String> names=new ArrayList<>(m.getShapeNames());Collections.sort(names);for(String name:names){Shape s=m.getShape(name);s.applyMatrix();for(Face f:s.getFaces())for(Vertex v:f.getVertexes()){h=31*h+Double.doubleToLongBits(v.getX());h=31*h+Double.doubleToLongBits(v.getY());h=31*h+Double.doubleToLongBits(v.getZ());}}return h;}
 static int animate(Animation<?>[] a,long time){int n=0;if(a!=null)for(Animation<?> x:a)if(x.animate(new Fixed(time))!=null)n++;return n;}
 static void require(boolean x,String message){if(!x)throw new AssertionError(message);comparisons++;}
 public static void main(String[] args)throws Exception{
  net.minecraft.init.Bootstrap.register();
  List<String> names=new ArrayList<>();try(java.util.stream.Stream<Path> files=Files.list(Paths.get(args[0]))){files.filter(p->p.toString().endsWith("Movement.java")&&!p.getFileName().toString().equals("IDoorMovement.java")).forEach(p->names.add(p.getFileName().toString().replace(".java","")));}Collections.sort(names);
  for(String name:names){Class<?> cls=Class.forName("net.malisis.doors.movement."+name);
   for(Constructor<?> ctor:cls.getConstructors()){
    if(ctor.getParameterCount()>1)throw new AssertionError("Unaudited movement constructor "+ctor);
    int variants=ctor.getParameterCount()==0?1:2;
    for(int constructorVariant=0;constructorVariant<variants;constructorVariant++){
     IDoorMovement movement=(IDoorMovement)(variants==1?ctor.newInstance():ctor.newInstance(constructorVariant==1));DoorTileEntity tile=movement instanceof RustyHatchMovement?new FixtureRusty():movement instanceof TrapDoorMovement||movement instanceof SlidingTrapDoorMovement?new FixtureTrap():new Fixture();Data data=((Controlled)tile).config();tile.getDescriptor().setMovement(movement);DoorAnimationPlans cache=new DoorAnimationPlans();
     MalisisModel expected=model(),actual=model();RenderParameters ep=new RenderParameters(),ap=new RenderParameters();
     for(int repetition=0;repetition<2;repetition++)for(int flags=0;flags<32;flags++)for(DoorState state:DoorState.values()){
      data.hinge=(flags&1)!=0;data.opened=(flags&2)!=0;if(tile instanceof SaloonDoorTileEntity)((SaloonDoorTileEntity)tile).setBackward((flags&4)!=0);data.top=(flags&4)!=0;data.partner=(flags&16)!=0?new Fixture():null;data.metadata=(flags&8)!=0?2:0;data.facing=(flags&8)!=0?EnumFacing.EAST:EnumFacing.SOUTH;
      tile.setState(state);tile.getDescriptor().setOpeningTime(16);
      for(long elapsed:new long[]{0,100,400,800,1200}){
       expected.resetState();actual.resetState();ep.alpha.reset();ap.alpha.reset();
       Animation<?>[] fresh=movement.getAnimations(tile,expected,ep);Animation<?>[] reused=cache.get(tile,actual,ap);
       require(animate(fresh,elapsed)==animate(reused,elapsed),name+" visibility "+flags+" "+state+" "+elapsed);
       require(fingerprint(expected,ep)==fingerprint(actual,ap),name+" geometry/alpha "+flags+" "+state+" "+elapsed);
      }
     }
     for(int duration=1;duration<1100;duration++){tile.getDescriptor().setOpeningTime(duration);cache.get(tile,actual,ap);}require(cache.size()<=512,name+" bounded cache");DoorAnimationPlans.clearAll();require(cache.size()==0,name+" invalidation");
    }
   }
  }
  System.out.println("PASS AnimationPlanChecks "+names.size()+" movement classes, "+comparisons+" geometry/visibility/bounds comparisons");
 }
}
