package net.malisis.doors.checks;

import java.nio.file.*;
import java.util.*;
import net.malisis.doors.mechanism.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class MechanismParity {
  public static void main(String[] args) throws Exception {
    int checks = 0;
    Map<String, Float> errors = new TreeMap<>();
    var matrix = new Matrix4f();
    var point = new Vector3f();
    var rows = Files.readAllLines(Path.of(args[0]));
    for (String line : rows.subList(1, rows.size())) {
      var v = line.split(",");
      var kind = MechanismSpec.Kind.valueOf(v[0]);
      float p = Float.parseFloat(v[5]);
      boolean left = Boolean.parseBoolean(v[2]);
      if (kind == MechanismSpec.Kind.VERTICAL) MechanismPose.vertical(matrix, v[1], p, left);
      else if (kind == MechanismSpec.Kind.SALOON) {
        MechanismPose.saloon(matrix, p, left, Boolean.parseBoolean(v[4]));
        if (!left)
          matrix =
              new Matrix4f()
                  .translate(.5f, 0, .5f)
                  .rotateY((float) Math.PI)
                  .translate(-.5f, 0, -.5f)
                  .mul(matrix);
      } else MechanismPose.part(matrix, kind, v[1], p, Boolean.parseBoolean(v[3]));
      matrix.transformPosition(
          Float.parseFloat(v[6]), Float.parseFloat(v[7]), Float.parseFloat(v[8]), point);
      float error =
          point.distance(Float.parseFloat(v[9]), Float.parseFloat(v[10]), Float.parseFloat(v[11]));
      errors.merge(v[0] + "/" + v[1], error, Math::max);
      checks++;
    }
    System.out.println(errors);
    if (errors.values().stream().anyMatch(error -> error > .0001f))
      throw new AssertionError("Legacy mechanism parity failed");
    System.out.println("PASS " + checks + " mechanism vertex comparisons");
  }
}
