package net.malisis.doors.client;

import com.mojang.blaze3d.vertex.*;
import net.malisis.doors.field.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class FieldRenderer implements BlockEntityRenderer<FieldEntity> {
  public static int rendered;

  public FieldRenderer(BlockEntityRendererProvider.Context context) {}

  @Override
  public void render(
      FieldEntity field,
      float partial,
      PoseStack stack,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    long metric =
        net.malisis.core.client.RenderMetrics.ENABLED
            ? net.malisis.core.client.RenderMetrics.begin()
            : 0;
    try {
      if (field.getBlockState().getValue(FieldBlock.OPEN)) return;
      var origin = field.origin(false);
      if (origin != null && !field.controller()) return;
      boolean full = field.controller();
      var box =
          full
              ? field
                  .bounds()
                  .move(
                      -field.getBlockPos().getX(),
                      -field.getBlockPos().getY(),
                      -field.getBlockPos().getZ())
              : new AABB(0, 0, 0, 1, 1, 1);
      var axis = field.getBlockState().getValue(FieldBlock.AXIS);
      var buffer =
          buffers.getBuffer(FieldRenderTypes.frame((int) (field.getLevel().getGameTime() % 50)));
      var pose = stack.last();
      float x0 = (float) box.minX,
          x1 = (float) box.maxX,
          y0 = (float) box.minY,
          y1 = (float) box.maxY,
          z0 = (float) box.minZ,
          z1 = (float) box.maxZ,
          width = axis == Direction.Axis.X ? z1 - z0 : x1 - x0,
          height = axis == Direction.Axis.Y ? z1 - z0 : y1 - y0;
      float u = width / 3, v = height / 3;
      if (axis == Direction.Axis.Y) {
        float y = (y0 + y1) * .5f;
        vertex(buffer, pose, x0, y, z0, 0, 0, 0, 1, 0);
        vertex(buffer, pose, x1, y, z0, u, 0, 0, 1, 0);
        vertex(buffer, pose, x1, y, z1, u, v, 0, 1, 0);
        vertex(buffer, pose, x0, y, z1, 0, v, 0, 1, 0);
      } else if (axis == Direction.Axis.X) {
        float x = (x0 + x1) * .5f;
        vertex(buffer, pose, x, y0, z0, 0, v, 1, 0, 0);
        vertex(buffer, pose, x, y0, z1, u, v, 1, 0, 0);
        vertex(buffer, pose, x, y1, z1, u, 0, 1, 0, 0);
        vertex(buffer, pose, x, y1, z0, 0, 0, 1, 0, 0);
      } else {
        float z = (z0 + z1) * .5f;
        vertex(buffer, pose, x0, y0, z, 0, v, 0, 0, 1);
        vertex(buffer, pose, x1, y0, z, u, v, 0, 0, 1);
        vertex(buffer, pose, x1, y1, z, u, 0, 0, 0, 1);
        vertex(buffer, pose, x0, y1, z, 0, 0, 0, 0, 1);
      }
      if (System.getProperty("malisis.verify.out") != null) rendered++;

    } finally {
      if (net.malisis.core.client.RenderMetrics.ENABLED)
        net.malisis.core.client.RenderMetrics.end(metric);
    }
  }

  private static void vertex(
      VertexConsumer buffer,
      PoseStack.Pose pose,
      float x,
      float y,
      float z,
      float u,
      float v,
      float nx,
      float ny,
      float nz) {
    buffer
        .addVertex(pose, x, y, z)
        .setColor(255, 255, 255, 50)
        .setUv(u, v)
        .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
        .setLight(LightTexture.FULL_BRIGHT)
        .setNormal(pose, nx, ny, nz);
  }

  @Override
  public AABB getRenderBoundingBox(FieldEntity field) {
    return field.bounds();
  }

  @Override
  public int getViewDistance() {
    return 128;
  }
}
