package net.malisis.doors.client;

import com.mojang.blaze3d.vertex.*;
import net.malisis.core.client.BoxMesh;
import net.malisis.core.util.BoundedCache;
import net.malisis.doors.*;
import net.malisis.doors.custom.DoorMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class DoorRenderer implements BlockEntityRenderer<DoorBlockEntity> {
  public static int resourceGeneration;
  public static final java.util.Set<String> CUSTOM_MOVEMENTS = new java.util.HashSet<>();
  public static final BoundedCache<DoorSpec, BoxMesh[]> MESHES = new BoundedCache<>(512);
  private final Matrix4f global = new Matrix4f(), part = new Matrix4f(), total = new Matrix4f();
  private final net.malisis.core.client.MeshPose meshPose = new net.malisis.core.client.MeshPose();
  private final Vector3f lightPoint = new Vector3f();
  private final BlockPos.MutableBlockPos lightPosition = new BlockPos.MutableBlockPos();

  public DoorRenderer(BlockEntityRendererProvider.Context context) {}

  @Override
  public void render(
      DoorBlockEntity door,
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
      if (door.spec().id().startsWith("minecraft:")
          && !net.malisis.doors.vanilla.Settings.modifyVanillaDoors) return;
      var state = door.getBlockState();
      boolean left = state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT;
      float p = door.progress(partial);
      String movement = door.configuration.movement();
      if (!DoorMotion.visible(movement, p, left)) return;
      int alpha = (int) (255 * DoorMotion.alpha(movement, p));
      if (alpha <= 0) return;
      var spec = door.spec();
      boolean custom = spec.id().equals("customdoor");
      if (custom && System.getProperty("malisis.verify.out") != null)
        CUSTOM_MOVEMENTS.add(movement);
      door.globalPose(global);
      for (int half = 0; half < 2; half++) {
        door.partPose(part, half, p);
        total.set(global).mul(part);
        var transformed = meshPose.set(stack.last(), total);
        total.transformPosition(.5f, .5f, .09375f, lightPoint);
        lightPosition.set(
            door.getBlockPos().getX() + (int) Math.floor(lightPoint.x),
            door.getBlockPos().getY() + (int) Math.floor(lightPoint.y),
            door.getBlockPos().getZ() + (int) Math.floor(lightPoint.z));
        int liveLight = LevelRenderer.getLightColor(door.getLevel(), lightPosition);
        if (custom)
          CustomMeshes.draw(
              CustomMeshes.of(door.configuration)[half],
              transformed,
              buffers,
              door.getLevel(),
              lightPosition,
              liveLight,
              overlay,
              alpha);
        else {
          BoxMesh mesh = MESHES.get(spec, DoorRenderer::bake)[half * 2 + (left ? 1 : 0)];
          var buffer =
              buffers.getBuffer(
                  alpha < 255
                      ? RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)
                      : RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
          if (movement.equals("curtain"))
            mesh.emitTaperedX(
                transformed,
                buffer,
                liveLight,
                overlay,
                0xffffff,
                alpha,
                1 - (1 - 3f / 16) * p,
                half == 1 ? 1 : 1 - (1 - 3f / 16) * p,
                left ? 1 : 0);
          else mesh.emit(transformed, buffer, liveLight, overlay, 0xffffff, alpha);
        }
      }

    } finally {
      if (net.malisis.core.client.RenderMetrics.ENABLED)
        net.malisis.core.client.RenderMetrics.end(metric);
    }
  }

  public static BoxMesh[] bake(DoorSpec spec) {
    BoxMesh[] result = new BoxMesh[4];
    if (spec.id().startsWith("minecraft:")) {
      var block =
          net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
              ResourceLocation.parse(spec.id()));
      for (int half = 0; half < 2; half++)
        for (int mirror = 0; mirror < 2; mirror++)
          result[half * 2 + mirror] =
              net.malisis.core.client.BakedMeshes.model(
                  block
                      .defaultBlockState()
                      .setValue(DoorBlock.FACING, net.minecraft.core.Direction.SOUTH)
                      .setValue(DoorBlock.OPEN, false)
                      .setValue(
                          DoorBlock.HALF,
                          half == 0
                              ? net.minecraft.world.level.block.state.properties.DoubleBlockHalf
                                  .LOWER
                              : net.minecraft.world.level.block.state.properties.DoubleBlockHalf
                                  .UPPER)
                      .setValue(
                          DoorBlock.HINGE, mirror == 1 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT));
      return result;
    }
    var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    var side =
        atlas.apply(
            ResourceLocation.fromNamespaceAndPath(
                "malisisdoors", "blocks/" + spec.texture() + "_side"));
    for (int half = 0; half < 2; half++)
      for (int mirror = 0; mirror < 2; mirror++) {
        var front =
            atlas.apply(
                ResourceLocation.fromNamespaceAndPath(
                    "malisisdoors",
                    "blocks/" + spec.texture() + (half == 1 ? "_upper" : "_lower")));
        result[half * 2 + mirror] = BoxMesh.panel(front, side, half == 1, mirror == 1);
      }
    return result;
  }

  @Override
  public AABB getRenderBoundingBox(DoorBlockEntity door) {
    return door.renderBounds;
  }

  @Override
  public int getViewDistance() {
    return 128;
  }
}
