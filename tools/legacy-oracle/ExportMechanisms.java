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

public class ExportMechanisms {
  public static void main(String[] args) throws Exception {
    net.minecraft.init.Bootstrap.register();
    try (PrintWriter out = new PrintWriter(args[0])) {
      out.println("kind,part,left,top,backward,p,x,y,z,ex,ey,ez");
      for (String kind : new String[] {"RUSTY_HATCH", "VERTICAL", "SALOON", "LARGE", "CARRIAGE"})
        for (int flags = 0; flags < 8; flags++)
          for (int step = 0; step <= 16; step++) {
            IDoorMovement movement =
                kind.equals("RUSTY_HATCH")
                    ? new RustyHatchMovement()
                    : kind.equals("VERTICAL")
                        ? new VerticalHatchMovement()
                        : kind.equals("SALOON")
                            ? new SaloonDoorMovement()
                            : kind.equals("LARGE")
                                ? new Door3x3Movement()
                                : new CarriageDoorMovement();
            DoorTileEntity tile =
                kind.equals("RUSTY_HATCH")
                    ? new AnimationPlanChecks.FixtureRusty()
                    : new AnimationPlanChecks.Fixture();
            AnimationPlanChecks.Data data = ((AnimationPlanChecks.Controlled) tile).config();
            data.hinge = (flags & 1) != 0;
            data.top = (flags & 2) != 0;
            if (tile instanceof SaloonDoorTileEntity)
              ((SaloonDoorTileEntity) tile).setBackward((flags & 4) != 0);
            tile.getDescriptor().setMovement(movement);
            tile.getDescriptor().setOpeningTime(16);
            tile.setState(DoorState.OPENING);
            MalisisModel before = AnimationPlanChecks.model(), after = AnimationPlanChecks.model();
            if (kind.equals("RUSTY_HATCH") && data.top) {
              after.getShape("door").translate(0, .875f, 0);
              after.getShape("handle").translate(0, .875f, 0);
            }
            if (kind.equals("SALOON") && !data.hinge) after.rotate(180, 0, 1, 0, 0, 0, 0);
            AnimationPlanChecks.animate(
                movement.getAnimations(tile, after, new RenderParameters()), step * 50);
            String[] parts =
                kind.equals("RUSTY_HATCH")
                    ? new String[] {"door", "handle"}
                    : kind.equals("VERTICAL")
                        ? new String[] {"Door", "Handle4"}
                        : kind.equals("SALOON")
                            ? new String[] {"bottom"}
                            : new String[] {"left", "right"};
            for (String part : parts) {
              Shape b = before.getShape(part), a = after.getShape(part);
              b.applyMatrix();
              a.applyMatrix();
              Set<String> seen = new HashSet<>();
              for (int f = 0; f < b.getFaces().length; f++)
                for (int v = 0; v < 4; v++) {
                  Vertex bv = b.getFaces()[f].getVertexes()[v],
                      av = a.getFaces()[f].getVertexes()[v];
                  String key = bv.getX() + "," + bv.getY() + "," + bv.getZ();
                  if (seen.add(key))
                    out.printf(
                        Locale.ROOT,
                        "%s,%s,%s,%s,%s,%.5f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f%n",
                        kind,
                        part.toLowerCase(),
                        data.hinge,
                        data.top,
                        (flags & 4) != 0,
                        step / 16f,
                        bv.getX(),
                        bv.getY(),
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
