package net.malisis.doors.compat;

/** ChunkMap regenerates on ordinary exceptions. An Error deliberately aborts loading instead. */
public final class LegacyUpgradeFailure extends Error {
  public LegacyUpgradeFailure(String message, Throwable cause) {
    super(message, cause);
  }
}
