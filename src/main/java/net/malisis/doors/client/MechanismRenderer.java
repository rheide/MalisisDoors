package net.malisis.doors.client;

import com.mojang.blaze3d.vertex.*;
import java.io.*;
import java.util.*;
import net.malisis.core.client.BoxMesh;
import net.malisis.core.model.ObjMesh;
import net.malisis.core.util.BoundedCache;
import net.malisis.doors.mechanism.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class MechanismRenderer implements BlockEntityRenderer<MechanismEntity> {
  public static final java.util.Set<String> RENDERED = new java.util.HashSet<>();

  public record Key(MechanismSpec spec, BlockState frame) {}

  public record Part(String name, BoxMesh mesh) {}

  public static final BoundedCache<Key, Part[]> MODELS = new BoundedCache<>(256);
  private final Matrix4f pose = new Matrix4f(), facing = new Matrix4f(), total = new Matrix4f();
  private final net.malisis.core.client.MeshPose meshPose = new net.malisis.core.client.MeshPose();

  public MechanismRenderer(BlockEntityRendererProvider.Context context) {}

  public static TextureAtlasSprite sprite(String name) {
    return Minecraft.getInstance()
        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
        .apply(ResourceLocation.fromNamespaceAndPath("malisisdoors", "blocks/" + name));
  }

  public static RenderType renderType(MechanismSpec spec, String part) {
    return spec.kind() == MechanismSpec.Kind.GARAGE || part.contains("glass")
        ? RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)
        : RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS);
  }

  private static Part[] bake(Key key) {
    var spec = key.spec();
    var texture = sprite(spec.texture());
    List<Part> parts = new ArrayList<>();
    if (spec.kind() == MechanismSpec.Kind.GARAGE) {
      parts.add(
          new Part("top", BoxMesh.cuboid(0, 0, .40625f, 1, 1, .59375f, sprite("garage_door_top"))));
      parts.add(new Part("bottom", BoxMesh.cuboid(0, 0, .40625f, 1, 1, .59375f, texture)));
    } else if (spec.kind() == MechanismSpec.Kind.LARGE) {
      parts.add(new Part("left", BoxMesh.cuboidUV(-1, 0, .8125f, .5f, 3, 1, texture, 0, .5f)));
      parts.add(new Part("right", BoxMesh.cuboidUV(.5f, 0, .8125f, 2, 3, 1, texture, .5f, 1)));
    } else {
      ResourceLocation path =
          ResourceLocation.fromNamespaceAndPath("malisisdoors", "models/" + spec.model() + ".obj");
      try (var input = Minecraft.getInstance().getResourceManager().openAsReader(path)) {
        Map<String, BoxMesh> original = ObjMesh.read(input, texture);
        if (spec.kind() == MechanismSpec.Kind.CARRIAGE
            || spec.kind() == MechanismSpec.Kind.MEDIEVAL) {
          var frameSprite =
              Minecraft.getInstance()
                  .getBlockRenderer()
                  .getBlockModelShaper()
                  .getParticleIcon(key.frame());
          try (var frameInput = Minecraft.getInstance().getResourceManager().openAsReader(path)) {
            var frames = ObjMesh.read(frameInput, frameSprite);
            if (frames.containsKey("frame")) parts.add(new Part("frame", frames.get("frame")));
          }
        }
        for (var entry : original.entrySet())
          if (!(entry.getKey().equals("frame")
              && (spec.kind() == MechanismSpec.Kind.CARRIAGE
                  || spec.kind() == MechanismSpec.Kind.MEDIEVAL))) {
            if (spec.kind() == MechanismSpec.Kind.RUSTY_HATCH
                && (entry.getKey().equals("handle") || entry.getKey().equals("ladder"))) {
              try (var handleInput =
                  Minecraft.getInstance().getResourceManager().openAsReader(path)) {
                parts.add(
                    new Part(
                        entry.getKey(),
                        ObjMesh.read(handleInput, sprite("rusty_hatch_handle"))
                            .get(entry.getKey())));
              }
            } else parts.add(new Part(entry.getKey(), entry.getValue()));
          }
      } catch (IOException e) {
        throw new IllegalStateException("Unable to load original model " + path, e);
      }
    }
    return parts.toArray(Part[]::new);
  }

  public static Part[] model(MechanismSpec spec, BlockState frame) {
    return MODELS.get(new Key(spec, frame), MechanismRenderer::bake);
  }

  @Override
  public void render(
      MechanismEntity entity,
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
      if (entity.spec().kind() == MechanismSpec.Kind.GARAGE && entity.columnTop() != entity) return;
      if (System.getProperty("malisis.verify.out") != null) RENDERED.add(entity.spec().id());
      var parts = model(entity.spec(), entity.frame);
      float progress = entity.progress(partial);
      entity.facingPose(facing);
      if (entity.spec().kind() == MechanismSpec.Kind.GARAGE) {
        int count = entity.columnHeight();
        var buffer = buffers.getBuffer(renderType(entity.spec(), "top"));
        for (int down = 0; down < count; down++) {
          MechanismPose.garage(pose, count, down, progress);
          total.set(facing).mul(pose);
          int live =
              LevelRenderer.getLightColor(entity.getLevel(), entity.getBlockPos().below(down));
          parts[down == 0 ? 0 : 1].mesh.emit(
              meshPose.set(stack.last(), total), buffer, live, overlay);
        }
      } else
        for (var part : parts) {
          if (part.name.equals("ladder"))
            continue; // Hatch ladders are emitted separately in each reserved shaft cell.
          entity.partPose(pose, part.name, progress);
          total.set(facing).mul(pose);
          part.mesh.emit(
              meshPose.set(stack.last(), total),
              buffers.getBuffer(renderType(entity.spec(), part.name)),
              light,
              overlay);
        }
      if (entity.spec().kind() == MechanismSpec.Kind.RUSTY_HATCH)
        for (var part : parts)
          if (part.name.equals("ladder"))
            for (var cell : entity.block().footprint(entity.getBlockPos(), entity.getBlockState()))
              if (entity.shouldLadder(cell)) {
                entity.facingPose(facing);
                total
                    .identity()
                    .translate(
                        cell.getX() - entity.getBlockPos().getX(),
                        cell.getY() - entity.getBlockPos().getY(),
                        cell.getZ() - entity.getBlockPos().getZ())
                    .mul(facing)
                    .translate(-1, 0, 0);
                part.mesh.emit(
                    meshPose.set(stack.last(), total),
                    buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
                    LevelRenderer.getLightColor(entity.getLevel(), cell),
                    overlay);
              }

    } finally {
      if (net.malisis.core.client.RenderMetrics.ENABLED)
        net.malisis.core.client.RenderMetrics.end(metric);
    }
  }

  @Override
  public int getViewDistance() {
    return 128;
  }

  @Override
  public AABB getRenderBoundingBox(MechanismEntity entity) {
    return entity.coverage();
  }
}
