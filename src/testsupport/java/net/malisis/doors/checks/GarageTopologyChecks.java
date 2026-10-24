package net.malisis.doors.checks;
import net.malisis.doors.tileentity.GarageDoorTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
public class GarageTopologyChecks {
 static class Door extends GarageDoorTileEntity {Door up,down;public GarageDoorTileEntity getDoor(EnumFacing facing){return facing==EnumFacing.UP?up:facing==EnumFacing.DOWN?down:null;}}
 public static void main(String[] args){int n=0;for(int height=1;height<=64;height++){Door[] doors=new Door[height];for(int i=0;i<height;i++){doors[i]=new Door();doors[i].setPos(new BlockPos(10,64+i,20));if(i>0){doors[i].down=doors[i-1];doors[i-1].up=doors[i];}}
  for(int i=0;i<height;i++){Door d=doors[i];if(d.getTopDoor()!=doors[height-1]||d.isTop()!=(i==height-1)||d.getOpeningTime()!=height*10)throw new AssertionError("Garage topology "+height+"/"+i);AxisAlignedBB box=d.getRenderBoundingBox();double count=i+1;if(box.minX!=10-count||box.maxX!=11+count||box.minY!=64+i-count||box.maxY!=65+i+count)throw new AssertionError("Garage bounds");n++;}}
 System.out.println("PASS GarageTopologyChecks "+n+" column positions across heights1..64");}
}
