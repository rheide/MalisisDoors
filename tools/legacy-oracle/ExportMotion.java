package net.malisis.doors.checks;

import java.io.*;
import java.util.*;
import net.malisis.core.renderer.*;
import net.malisis.core.renderer.animation.*;
import net.malisis.core.renderer.element.*;
import net.malisis.core.renderer.model.*;
import net.malisis.doors.*;
import net.malisis.doors.movement.*;
import net.malisis.doors.tileentity.*;

public class ExportMotion {
  public static void main(String[] args) throws Exception {
    net.minecraft.init.Bootstrap.register();
    String[] classes = {
      "RotatingDoor",
      "SlidingDoor",
      "SlidingUpDoor",
      "SlidingSplitDoor",
      "VaultDoor",
      "RotatingSplit",
      "Sliding4Ways",
      "Rotating4Ways",
      "RotateAround",
      "RotateAndSlide",
      "RotateAndPlace",
      "VanishingDoor",
      "SpinningDoor",
      "SpinningAroundDoor",
      "DoubleSlide",
      "DoubleSlide",
      "DoubleRotate",
      "DoubleRotate",
      "Curtain"
    };
    String[] ids = {
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
      "double_rotate_right",
      "curtain"
    };
    try (PrintWriter out = new PrintWriter(args[0])) {
      out.println("id,left,paired,half,progress,x,y,z,expected_x,expected_y,expected_z");
      for (int m = 0; m < ids.length; m++) {
        Class<?> cls = Class.forName("net.malisis.doors.movement." + classes[m] + "Movement");
        IDoorMovement move =
            (IDoorMovement)
                (classes[m].startsWith("Double")
                    ? cls.getConstructor(boolean.class).newInstance(ids[m].endsWith("right"))
                    : cls.getConstructor().newInstance());
        for (int flags = 0; flags < 4; flags++)
          for (int step = 0; step <= 8; step++) {
            AnimationPlanChecks.Fixture tile = new AnimationPlanChecks.Fixture();
            tile.data.hinge = (flags & 1) != 0;
            tile.data.partner = (flags & 2) != 0 ? new AnimationPlanChecks.Fixture() : null;
            tile.setState(DoorState.OPENING);
            tile.getDescriptor().setMovement(move);
            tile.getDescriptor().setOpeningTime(16);
            MalisisModel before = AnimationPlanChecks.model(), after = AnimationPlanChecks.model();
            AnimationPlanChecks.animate(
                move.getAnimations(tile, after, new RenderParameters()), step * 100);
            for (int half = 0; half < 2; half++) {
              String name = half == 0 ? "bottom" : "top";
              Shape b = before.getShape(name), a = after.getShape(name);
              b.applyMatrix();
              a.applyMatrix();
              Set<String> seen = new HashSet<>();
              for (int f = 0; f < b.getFaces().length; f++)
                for (int v = 0; v < b.getFaces()[f].getVertexes().length; v++) {
                  Vertex bv = b.getFaces()[f].getVertexes()[v],
                      av = a.getFaces()[f].getVertexes()[v];
                  String key = bv.getX() + "," + bv.getY() + "," + bv.getZ();
                  if (seen.add(key))
                    out.printf(
                        Locale.ROOT,
                        "%s,%s,%s,%d,%.3f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f%n",
                        ids[m],
                        tile.data.hinge,
                        tile.data.partner != null,
                        half,
                        step / 8f,
                        bv.getX(),
                        bv.getY() - half,
                        bv.getZ(),
                        av.getX(),
                        av.getY(),
                        av.getZ());
                }
            }
          }
      }
    }
  }
}
