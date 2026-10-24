package net.malisis.doors;
/** Descriptor values retained from the legacy mod. */
public record DoorSpec(String id,String texture,Movement movement,int ticks,boolean redstoneOnly,boolean pairable) {
 public enum Movement { SLIDE, UP, SPLIT, CURTAIN }
}
