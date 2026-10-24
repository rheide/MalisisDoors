package net.malisis.doors.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

/** Native additive pass restores its GL state through Minecraft's renderer. */
public final class FieldRenderTypes extends RenderType {
  private FieldRenderTypes() {
    super(
        "malisis_field",
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        () -> {},
        () -> {});
  }

  private static final RenderType[] FRAMES = new RenderType[50];

  public static RenderType frame(int index) {
    RenderType type = FRAMES[index];
    if (type == null) {
      var texture =
          ResourceLocation.fromNamespaceAndPath(
              "malisisdoors",
              String.format("textures/blocks/forcefield/forcefield%02d.png", index));
      type =
          create(
              "malisis_forcefield",
              DefaultVertexFormat.NEW_ENTITY,
              VertexFormat.Mode.QUADS,
              256,
              false,
              true,
              CompositeState.builder()
                  .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                  .setTextureState(new TextureStateShard(texture, false, false))
                  .setTransparencyState(ADDITIVE_TRANSPARENCY)
                  .setCullState(NO_CULL)
                  .setLightmapState(LIGHTMAP)
                  .setOverlayState(OVERLAY)
                  .setWriteMaskState(COLOR_WRITE)
                  .createCompositeState(false));
      FRAMES[index] = type;
    }
    return type;
  }
}
