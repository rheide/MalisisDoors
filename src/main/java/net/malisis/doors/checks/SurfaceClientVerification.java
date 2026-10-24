package net.malisis.doors.checks;

import com.mojang.blaze3d.vertex.*;
import java.nio.file.*;
import java.util.*;
import net.malisis.doors.client.*;
import net.malisis.doors.mechanism.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;

/** Compare actual emitted front/back UVs to vertices exported by the 1.12.2 renderer. */
public final class SurfaceClientVerification {
  public static void verify(Minecraft mc, String output) throws Exception {
    var rows = Files.readAllLines(Path.of(System.getProperty("malisis.verify.surfaces")));
    int checked = 0;
    for (var entry : Mechanisms.BLOCKS) {
      var spec = entry.get().spec;
      if (spec.kind() != MechanismSpec.Kind.LARGE) continue;
      var texture = MechanismRenderer.sprite(spec.texture());
      for (var part : MechanismRenderer.model(spec, Blocks.QUARTZ_BLOCK.defaultBlockState())) {
        var recording = new Recording();
        part.mesh().emit(new PoseStack().last(), recording, LightTexture.FULL_BRIGHT, 0);
        for (String row : rows.subList(1, rows.size())) {
          var v = row.split(",");
          if (!v[0].equals(part.name()) || !v[1].equals("North") && !v[1].equals("South")) continue;
          float x = Float.parseFloat(v[2]),
              y = Float.parseFloat(v[3]),
              nz = v[1].equals("North") ? -1 : 1;
          var vertex =
              recording.vertices.stream()
                  .filter(
                      p -> Math.abs(p[0] - x) < .0001 && Math.abs(p[1] - y) < .0001 && p[6] == nz)
                  .findFirst()
                  .orElseThrow();
          if (Math.abs(vertex[3] - texture.getU(Float.parseFloat(v[5]))) > .00001
              || Math.abs(vertex[4] - texture.getV(Float.parseFloat(v[6]))) > .00001)
            throw new IllegalStateException(
                "Legacy handle UV mismatch " + spec.id() + " " + part.name() + " " + v[1]);
          checked++;
        }
      }
    }
    var spec =
        Mechanisms.BLOCKS.stream()
            .map(e -> e.get().spec)
            .filter(s -> s.kind() == MechanismSpec.Kind.GARAGE)
            .findFirst()
            .orElseThrow();
    if (MechanismRenderer.renderType(spec, "top")
        != RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS))
      throw new IllegalStateException("Garage window has no blended render pass");
    Files.writeString(
        Path.of(output, "surface-client.txt"),
        "PASS "
            + checked
            + " emitted front/back UV coordinates matched the legacy large-door meshes across all"
            + " eight variants; garage uses blended rendering\n");
  }

  static final class Recording implements VertexConsumer {
    final List<float[]> vertices = new ArrayList<>();
    float x, y, z, u, v;

    public VertexConsumer addVertex(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return this;
    }

    public VertexConsumer setColor(int r, int g, int b, int a) {
      return this;
    }

    public VertexConsumer setUv(float u, float v) {
      this.u = u;
      this.v = v;
      return this;
    }

    public VertexConsumer setUv1(int u, int v) {
      return this;
    }

    public VertexConsumer setUv2(int u, int v) {
      return this;
    }

    public VertexConsumer setNormal(float x, float y, float z) {
      vertices.add(new float[] {this.x, this.y, this.z, u, v, x, z});
      return this;
    }
  }
}
