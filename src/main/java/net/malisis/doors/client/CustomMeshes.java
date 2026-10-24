package net.malisis.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.malisis.core.client.BoxMesh;
import net.malisis.core.util.BoundedCache;
import net.malisis.doors.custom.DoorConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class CustomMeshes {
  public record Key(BlockState frame, BlockState top, BlockState bottom) {}

  public record Part(BoxMesh mesh, BlockState material, boolean translucent) {}

  public static final BoundedCache<Key, Part[][]> CACHE = new BoundedCache<>(256);

  private static TextureAtlasSprite sprite(BlockState material) {
    String special =
        material.getBlock() == Blocks.WATER
            ? "water_still"
            : material.getBlock() == Blocks.LAVA
                ? "lava_still"
                : material.getBlock() == Blocks.FIRE
                    ? "fire_0"
                    : material.getBlock() == Blocks.NETHER_PORTAL ? "nether_portal" : null;
    return special == null
        ? Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(material)
        : Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(ResourceLocation.withDefaultNamespace("block/" + special));
  }

  private static boolean translucent(BlockState state) {
    return state.getBlock() instanceof LiquidBlock
        || state.getBlock() instanceof NetherPortalBlock
        || ItemBlockRenderTypes.getRenderLayers(state).contains(RenderType.translucent());
  }

  private static Part[][] bake(Key key) {
    Part[][] parts = new Part[2][2];
    var frame = sprite(key.frame);
    for (int half = 0; half < 2; half++) {
      var material = half == 0 ? key.bottom : key.top;
      var texture = sprite(material);
      float barY = half == 0 ? 0 : .875f, materialY = half == 0 ? .125f : 0;
      BoxMesh rails =
          BoxMesh.merge(
              BoxMesh.blockCuboid(0, 0, 0, .125f, 1, .1875f * .995f, frame),
              BoxMesh.blockCuboid(.875f, 0, 0, 1, 1, .1875f * .995f, frame),
              BoxMesh.blockCuboid(.125f, barY, 0, .875f, barY + .125f, .1875f * .995f, frame)
                  .faces(51));
      BoxMesh panel =
          BoxMesh.blockCuboid(.125f, materialY, .0375f, .875f, materialY + .875f, .15f, texture)
              .faces(19);
      parts[half][0] = new Part(rails, key.frame, translucent(key.frame));
      parts[half][1] = new Part(panel, material, translucent(material));
    }
    return parts;
  }

  public static Part[][] of(DoorConfiguration configuration) {
    return CACHE.get(
        new Key(configuration.frame(), configuration.top(), configuration.bottom()),
        CustomMeshes::bake);
  }

  public static void draw(
      Part[] parts,
      PoseStack stack,
      MultiBufferSource buffers,
      Level level,
      BlockPos pos,
      int light,
      int overlay,
      int alpha) {
    draw(parts, stack.last(), buffers, level, pos, light, overlay, alpha);
  }

  public static void draw(
      Part[] parts,
      PoseStack.Pose pose,
      MultiBufferSource buffers,
      Level level,
      BlockPos pos,
      int light,
      int overlay,
      int alpha) {
    var colors = Minecraft.getInstance().getBlockColors();
    for (var part : parts) {
      int color =
          part.material.getBlock() == Blocks.GRASS_BLOCK
              ? 0xffffff
              : colors.getColor(part.material, level, pos, 0);
      if (part.material.getBlock() == Blocks.WATER && level != null)
        color = BiomeColors.getAverageWaterColor(level, pos);
      if (color == -1) color = 0xffffff;
      int emission =
          level == null
              ? part.material.getLightEmission()
              : part.material.getLightEmission(level, pos);
      int lit = (light & 0xffff0000) | Math.max(light & 0xffff, emission << 4);
      part.mesh.emit(
          pose,
          buffers.getBuffer(
              part.translucent || alpha < 255
                  ? RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)
                  : RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
          lit,
          overlay,
          color,
          alpha);
    }
  }
}
