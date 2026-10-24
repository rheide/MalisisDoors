package net.malisis.doors.mechanism;
public record MechanismSpec(String id,Kind kind,int width,int height,int depth,int minX,String texture,String model,int ticks) {
 public enum Kind { GARAGE,LARGE,CARRIAGE,MEDIEVAL,HITECH,RUSTY_HATCH }
}
