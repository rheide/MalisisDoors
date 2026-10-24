package net.malisis.doors.mechanism;

public record MechanismSpec(
    String id,
    Kind kind,
    int width,
    int height,
    int depth,
    int minX,
    String texture,
    String model,
    int ticks) {
  /** Legacy DirectionalComponent.PLACER, unlike native DoorItem placement. */
  public boolean usesPlacerFacing() {
    return switch (kind) {
      case GARAGE, LARGE, CARRIAGE, MEDIEVAL, HITECH -> true;
      default -> false;
    };
  }

  public enum Kind {
    GARAGE,
    LARGE,
    CARRIAGE,
    MEDIEVAL,
    HITECH,
    RUSTY_HATCH,
    SALOON,
    VERTICAL
  }
}
