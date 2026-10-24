package net.malisis.doors.checks;

import java.nio.file.*;
import java.util.*;
import net.malisis.doors.custom.DoorMotion;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Expected vertices exported by the actual upstream 1.12.2 animation engine. */
public final class MotionParity {
  public static void main(String[] args) throws Exception {
    int checks = 0;
    Map<String, Float> errors = new TreeMap<>();
    var matrix = new Matrix4f();
    var point = new Vector3f();
    for (String line :
        Files.readAllLines(Path.of(args[0]))
            .subList(1, Files.readAllLines(Path.of(args[0])).size())) {
      String[] v = line.split(",");
      DoorMotion.pose(
          matrix,
          v[0],
          Float.parseFloat(v[4]),
          Boolean.parseBoolean(v[1]),
          Integer.parseInt(v[3]),
          Boolean.parseBoolean(v[2]));
      float x = Float.parseFloat(v[5]), y = Float.parseFloat(v[6]);
      if (v[0].equals("curtain")) {
        float travel = (1 - 3f / 16) * Float.parseFloat(v[4]) * (v[3].equals("1") ? 1 - y : 1);
        x += (Boolean.parseBoolean(v[1]) ? 1 - x : -x) * travel;
      }
      matrix.transformPosition(x, y, Float.parseFloat(v[7]), point);
      float error =
          point.distance(Float.parseFloat(v[8]), Float.parseFloat(v[9]), Float.parseFloat(v[10]));
      errors.merge(v[0], error, Math::max);
      checks++;
    }
    System.out.println(errors);
    if (errors.values().stream().anyMatch(error -> error > .0001f))
      throw new AssertionError("Legacy motion parity failed");
    System.out.println(
        "PASS "
            + checks
            + " original-engine vertex samples across 19 movements, both hinges, paired and single"
            + " doors");
  }
}
