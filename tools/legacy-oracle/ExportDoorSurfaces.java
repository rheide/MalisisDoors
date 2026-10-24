package net.malisis.doors.checks;

import java.io.*;
import java.util.*;
import net.malisis.core.block.component.DirectionalComponent;
import net.malisis.core.renderer.element.*;
import net.malisis.core.renderer.element.shape.Cube;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

public class ExportDoorSurfaces {
  public static void main(String[] args) throws Exception {
    net.minecraft.init.Bootstrap.register();
    try (PrintWriter out = new PrintWriter(args[0])) {
      out.println("yaw,sneak,facing");
      varPlacements(out);
    }
    try (PrintWriter out = new PrintWriter(args[1])) {
      out.println("part,face,x,y,z,u,v");
      for (boolean right : new boolean[] {false, true}) {
        Cube door = new Cube();
        if (right) {
          door.setSize(1, 3, .1875f / 3.01f);
          door.limit(.5f, 0, 0, 1, 3, .1875f);
          door.interpolateUV();
          door.setSize(1.5f, 3, .1875f);
          door.scale(1, 1, .995f);
          door.translate(0, 0, .8125f);
        } else {
          door.setSize(.5f, 3, .1875f / 3.01f);
          door.interpolateUV();
          door.setSize(1.5f, 3, .1875f);
          door.scale(1, 1, .995f);
          door.translate(-1, 0, .8125f);
        }
        door.applyMatrix();
        for (Face face : door.getFaces())
          for (Vertex v : face.getVertexes())
            out.printf(
                Locale.ROOT,
                "%s,%s,%.8f,%.8f,%.8f,%.8f,%.8f%n",
                right ? "right" : "left",
                face.name(),
                v.getX(),
                v.getY(),
                v.getZ(),
                v.getU(),
                v.getV());
      }
    }
  }

  static void varPlacements(PrintWriter out) {
    EntityArmorStand player = new EntityArmorStand(null);
    for (int yaw = 0; yaw < 360; yaw += 90)
      for (boolean sneak : new boolean[] {false, true}) {
        player.rotationYaw = yaw;
        player.setSneaking(sneak);
        out.printf(
            Locale.ROOT,
            "%d,%s,%s%n",
            yaw,
            sneak,
            DirectionalComponent.IPlacement.PLACER
                .getPlacement(Blocks.STONE.getDefaultState(), EnumFacing.UP, player)
                .getName());
      }
  }
}
