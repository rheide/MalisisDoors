package net.malisis.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.io.*;
import java.util.*;
import net.malisis.core.client.BoxMesh;
import net.malisis.core.model.ObjMesh;
import net.malisis.core.util.BoundedCache;
import net.malisis.doors.flat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class FlatRenderer implements BlockEntityRenderer<FlatEntity> {
  public static final java.util.Set<net.minecraft.resources.ResourceLocation> RENDERED =
      new java.util.HashSet<>();
  public static final BoundedCache<BlockState, BoxMesh[][]> MODELS = new BoundedCache<>(256);
  private final net.malisis.core.client.MeshPose meshPose = new net.malisis.core.client.MeshPose();
  private final Matrix4f matrix = new Matrix4f();

  public FlatRenderer(BlockEntityRendererProvider.Context context) {}

  public static BoxMesh[][] bake(BlockState state) {
    if (state.getBlock() instanceof AnimatedTrapdoor block)
      return new BoxMesh[][] {
        {
          BoxMesh.blockCuboid(
              0,
              0,
              0,
              1,
              block.sliding ? .09375f : .1875f,
              1,
              MechanismRenderer.sprite(block.texture))
        }
      };
    if (state.getBlock() instanceof TrapDoorBlock)
      return new BoxMesh[][] {
        {
          net.malisis.core.client.BakedMeshes.model(
              state
                  .setValue(TrapDoorBlock.OPEN, false)
                  .setValue(
                      TrapDoorBlock.HALF,
                      net.minecraft.world.level.block.state.properties.Half.BOTTOM)
                  .setValue(TrapDoorBlock.FACING, net.minecraft.core.Direction.NORTH))
        }
      };
    if (state.getBlock() instanceof LadderBlock) {
      try (var input =
          Minecraft.getInstance()
              .getResourceManager()
              .openAsReader(
                  ResourceLocation.fromNamespaceAndPath(
                      "malisisdoors", "models/rusty_hatch.obj"))) {
        return new BoxMesh[][] {
          {ObjMesh.read(input, MechanismRenderer.sprite("rusty_hatch_handle")).get("ladder")}
        };
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    TextureAtlasSprite texture =
        state.getBlock() instanceof CamouflageGate
            ? MechanismRenderer.sprite("camo_fencegate")
            : Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModelShaper()
                .getParticleIcon(state);
    BoxMesh[][] parts = new BoxMesh[2][4];
    for (int half = 0; half < 2; half++) {
      float min = half * .5f, max = (half + 1) * .5f;
      parts[half][0] =
          BoxMesh.blockCuboid(
              half == 0 ? 0 : .875f, .3125f, .4375f, half == 0 ? .125f : 1, 1, .5625f, texture);
      parts[half][1] =
          BoxMesh.blockCuboid(
              half == 0 ? .375f : .5f,
              .375f,
              .4375f,
              half == 0 ? .5f : .625f,
              .9375f,
              .5625f,
              texture);
      parts[half][2] =
          BoxMesh.blockCuboid(min + .125f, .375f, .4375f, max - .125f, .5625f, .5625f, texture);
      parts[half][3] =
          BoxMesh.blockCuboid(min + .125f, .75f, .4375f, max - .125f, .9375f, .5625f, texture);
    }
    return parts;
  }

  @Override
  public void render(
      FlatEntity entity,
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
      if (net.minecraft.core.registries.BuiltInRegistries.BLOCK
              .getKey(entity.getBlockState().getBlock())
              .getNamespace()
              .equals("minecraft")
          && !net.malisis.doors.vanilla.Settings.modifyVanillaDoors) return;
      if (System.getProperty("malisis.verify.out") != null)
        RENDERED.add(
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(
                entity.getBlockState().getBlock()));
      var key = entity.trap() || entity.ladder() ? entity.getBlockState() : entity.camouflage();
      var parts = MODELS.get(key, FlatRenderer::bake);
      float p = entity.progress(partial);
      int tint =
          Minecraft.getInstance()
              .getBlockColors()
              .getColor(key, entity.getLevel(), entity.getBlockPos(), 0);
      if (tint < 0) tint = 0xffffff;
      var buffer =
          buffers.getBuffer(
              ItemBlockRenderTypes.getRenderLayers(key).contains(RenderType.translucent())
                  ? RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)
                  : RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
      if (!entity.trap() && !entity.ladder() && entity.partner() != null) {
        entity.pose(matrix, 0, p);
        var transformed = meshPose.set(stack.last(), matrix);
        for (var half : parts)
          for (var mesh : half) mesh.emit(transformed, buffer, light, overlay, tint, 255);
      } else
        for (int half = 0; half < parts.length; half++) {
          entity.pose(matrix, half, p);
          var transformed = meshPose.set(stack.last(), matrix);
          for (var mesh : parts[half]) mesh.emit(transformed, buffer, light, overlay, tint, 255);
        }

    } finally {
      if (net.malisis.core.client.RenderMetrics.ENABLED)
        net.malisis.core.client.RenderMetrics.end(metric);
    }
  }

  @Override
  public AABB getRenderBoundingBox(FlatEntity entity) {
    return entity.coverage();
  }
}
