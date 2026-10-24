package net.malisis.doors.custom;

import java.util.*;
import org.joml.Matrix4f;

/** The eighteen factory movements and standard curtain deformation, in canonical door space. */
public final class DoorMotion {
  public static final List<String> FACTORY =
      List.of(
          "rotating_door",
          "sliding_door",
          "sliding_up_door",
          "sliding_split_door",
          "vault_door",
          "rotating_split_door",
          "sliding_4ways",
          "rotating_4ways",
          "rotate_around",
          "rotate_slide",
          "rotate_place",
          "vanishing_door",
          "spinning_door",
          "spinning_around_door",
          "double_slide_left",
          "double_slide_right",
          "double_rotate_left",
          "double_rotate_right");

  public static boolean known(String id) {
    return FACTORY.contains(id) || id.equals("curtain");
  }

  private static void rotate(Matrix4f matrix, float degrees, int axis, float x, float y, float z) {
    matrix.translate(x, y, z);
    float radians = (float) Math.toRadians(degrees);
    if (axis == 0) matrix.rotateX(radians);
    else if (axis == 1) matrix.rotateY(radians);
    else matrix.rotateZ(radians);
    matrix.translate(-x, -y, -z);
  }

  public static Matrix4f pose(
      Matrix4f matrix, String id, float progress, boolean left, int half, boolean paired) {
    matrix.identity();
    float p = Math.max(0, Math.min(1, progress)),
        d = 3f / 16,
        hinge = left ? 1 - d / 2 : d / 2,
        sign = left ? 1 : -1;
    boolean top = half == 1;
    switch (id) {
      case "sliding_door" -> matrix.translate(sign * (1 - d) * p, 0, 0);
      case "sliding_up_door" -> matrix.translate(0, (2 - d) * p, 0);
      case "sliding_split_door" -> matrix.translate(0, (top ? 1 - d : -1.1f) * p, 0);
      case "curtain" -> {} // Per-vertex taper leaves the upper edge attached to the rail.
      case "rotating_door" -> rotate(matrix, sign * 90 * p, 1, hinge, .5f, d / 2);
      case "vault_door" -> rotate(matrix, -90 * p, 2, hinge, top ? 2 - d / 2 : d / 2, .5f);
      case "rotating_split_door" ->
          rotate(matrix, (top ? -90 : 90) * p, 0, .5f, top ? 2 - d / 2 : d / 2, d / 2);
      case "sliding_4ways" -> {
        float travel = sign * (1 - d) * p;
        if (top == left) matrix.translate(0, travel, 0);
        else matrix.translate(travel, 0, 0);
      }
      case "rotating_4ways" ->
          rotate(
              matrix,
              (top ? -90 : 90) * p,
              top == left ? 0 : 1,
              hinge,
              top ? 2 - d / 2 : d / 2,
              d / 2);
      case "rotate_around" -> rotate(matrix, -sign * 90 * p, 1, .5f, .5f, .5f);
      case "rotate_slide" -> {
        float first = Math.min(1, p * 2), second = Math.max(0, p * 2 - 1);
        rotate(matrix, -sign * 90 * first, 1, hinge, .5f, d / 2);
        matrix.translate(sign * (1 - d) * second, 0, 0);
      }
      case "rotate_place" -> {
        float first = Math.min(1, p * 2), second = Math.max(0, p * 2 - 1);
        rotate(matrix, sign * 90 * first, 1, .5f, .5f, 0);
        matrix.translate(0, 0, (.5f - d) * second);
      }
      case "spinning_door" -> {
        float size = Math.max(.00001f, 1 - p);
        matrix
            .translate(.5f, half + .5f, .5f)
            .rotateZ((float) Math.toRadians(sign * 720 * (1 - p)))
            .scale(size)
            .translate(-.5f, -half - .5f, -.5f);
      }
      case "spinning_around_door" -> {
        float size = Math.max(.00001f, 1 - p), pivot = .5f + (paired ? (left ? -.5f : .5f) : 0);
        rotate(matrix, 720 * p, 2, pivot, 1, .5f);
        rotate(matrix, -720 * p, 2, .5f, half + .5f, .5f);
        matrix.translate(.5f, half + .5f, .5f).scale(size).translate(-.5f, -half - .5f, -.5f);
      }
      case "double_slide_left", "double_slide_right" -> {
        boolean right = id.endsWith("right");
        float x = (1 - d) + (left == right ? 1 : 0);
        matrix.translate((right ? -1 : 1) * x * p, 0, 0);
      }
      case "double_rotate_left", "double_rotate_right" -> {
        boolean right = id.endsWith("right");
        if (left == right) matrix.translate((right ? -1 : 1) * (2 - d) * p, 0, 0);
        rotate(matrix, sign * 90 * p, 1, hinge, .5f, d / 2);
      }
      case "vanishing_door" -> {}
      default -> throw new IllegalArgumentException("Unknown door movement " + id);
    }
    return matrix.translate(0, half, 0);
  }

  public static float alpha(String id, float p) {
    return id.equals("vanishing_door") ? Math.max(0, 1 - p) : 1;
  }

  public static boolean visible(String id, float p, boolean left) {
    if (p < .99999f) return true;
    if (id.startsWith("spinning_")) return false;
    return !(id.startsWith("double_rotate_") && left == id.endsWith("right"));
  }

  public static boolean solid(String id, float p, boolean left) {
    return visible(id, p, left) && !(id.equals("vanishing_door") && p > 0);
  }
}
