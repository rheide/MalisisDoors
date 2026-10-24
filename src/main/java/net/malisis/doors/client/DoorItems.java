package net.malisis.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.malisis.doors.*;
import net.malisis.doors.custom.DoorConfiguration;
import net.malisis.doors.factory.FactoryContent;
import net.malisis.doors.flat.*;
import net.malisis.doors.mechanism.*;
import net.malisis.doors.vanilla.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import org.joml.Matrix4f;

/** Item views reuse the same immutable closed meshes as the world renderers. */
public final class DoorItems {
  public static int rendered;
  private static final Matrix4f MATRIX = new Matrix4f();

  public static boolean accepts(ItemStack stack) {
    if (stack.is(MalisisDoors.RUSTY_HANDLE.get()) || stack.is(FactoryContent.CUSTOM_ITEM.get()))
      return true;
    if (!(stack.getItem() instanceof BlockItem item)) return false;
    var block = item.getBlock();
    if (block instanceof MechanismBlock mechanism)
      return switch (mechanism.spec.kind()) {
        case GARAGE, HITECH, VERTICAL -> true;
        default -> Settings.use3DItems;
      };
    if (block instanceof AnimatedDoorBlock) return Settings.use3DItems;
    if (block instanceof AnimatedTrapdoor
        || block instanceof CamouflageGate
        || block instanceof RustyLadder) return true;
    return VanillaAnimations.enabled(block)
        && (block instanceof TrapDoorBlock
            || block instanceof FenceGateBlock
            || block instanceof DoorBlock && Settings.use3DItems);
  }

  public static void render(
      ItemStack item,
      ItemDisplayContext context,
      boolean left,
      PoseStack stack,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    var mc = Minecraft.getInstance();
    Block block = item.getItem() instanceof BlockItem bi ? bi.getBlock() : null;
    float minX = 0, minY = 0, minZ = 0, maxX = 1, maxY = 1, maxZ = 1;
    if (block instanceof DoorBlock) {
      maxY = 2;
      maxZ = .1875f;
    }
    if (block instanceof TrapDoorBlock) maxY = .1875f;
    if (block instanceof MechanismBlock mechanism) {
      var spec = mechanism.spec;
      minX = spec.minX();
      maxX = minX + spec.width();
      maxY = spec.height();
      maxZ = spec.depth();
      if (spec.kind() == MechanismSpec.Kind.RUSTY_HATCH) {
        minX = 0;
        maxX = 2;
        maxY = 1;
        maxZ = 2;
      }
    }
    float scale = .85f / Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
    stack.pushPose();
    switch (context) {
      case GUI -> {
        stack.mulPose(Axis.XP.rotationDegrees(20));
        stack.mulPose(Axis.YP.rotationDegrees(-30));
      }
      case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
        stack.translate(left ? -.15 : .15, .15, -.35);
        stack.mulPose(Axis.YP.rotationDegrees(left ? 35 : -35));
        scale *= .9f;
      }
      case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
        stack.translate(0, .15, 0);
        scale *= .65f;
      }
      case GROUND -> scale *= .5f;
      case FIXED -> stack.mulPose(Axis.YP.rotationDegrees(180));
      default -> {}
    }
    stack.scale(scale, scale, scale);
    stack.translate(-(minX + maxX) * .5, -(minY + maxY) * .5, -(minZ + maxZ) * .5);
    if (block instanceof DoorBlock door) {
      var spec = VanillaAnimations.spec(door);
      var config = DoorConfiguration.defaults(spec);
      if (mc.level != null)
        config = DoorConfiguration.readItem(item, mc.level.registryAccess(), config);
      for (int half = 0; half < 2; half++) {
        stack.pushPose();
        stack.translate(0, half, 0);
        if (block == FactoryContent.CUSTOM_DOOR.get())
          CustomMeshes.draw(
              CustomMeshes.of(config)[half],
              stack,
              buffers,
              mc.level,
              BlockPos.ZERO,
              light,
              overlay,
              255);
        else
          DoorRenderer.MESHES.get(spec, DoorRenderer::bake)[half * 2].emit(
              stack.last(),
              buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
              light,
              overlay);
        stack.popPose();
      }
    } else if (block instanceof MechanismBlock mechanism) {
      BlockState frame = Blocks.QUARTZ_BLOCK.defaultBlockState();
      var data = item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
      if (data != null && data.copyTag().contains("frame") && mc.level != null)
        frame =
            NbtUtils.readBlockState(
                mc.level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),
                data.copyTag().getCompound("frame"));
      for (var part : MechanismRenderer.model(mechanism.spec, frame)) {
        if (part.name().equals("ladder")) continue;
        stack.pushPose();
        MechanismPose.part(MATRIX, mechanism.spec.kind(), part.name(), 0, false);
        stack.mulPose(MATRIX);
        part.mesh()
            .emit(
                stack.last(),
                buffers.getBuffer(
                    part.name().contains("glass")
                        ? RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)
                        : RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
                light,
                overlay);
        stack.popPose();
      }
    } else if (block instanceof TrapDoorBlock
        || block instanceof FenceGateBlock
        || block instanceof LadderBlock) {
      var state = block.defaultBlockState();
      if (block instanceof TrapDoorBlock)
        state =
            state
                .setValue(TrapDoorBlock.FACING, Direction.NORTH)
                .setValue(TrapDoorBlock.HALF, Half.BOTTOM)
                .setValue(TrapDoorBlock.OPEN, false);
      if (block instanceof LadderBlock) stack.translate(-1, 0, 0);
      for (var half : FlatRenderer.MODELS.get(state, FlatRenderer::bake))
        for (var mesh : half)
          mesh.emit(
              stack.last(),
              buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
              light,
              overlay);
    } else if (item.is(MalisisDoors.RUSTY_HANDLE.get())) {
      var spec =
          Mechanisms.BLOCKS.stream()
              .map(b -> b.get().spec)
              .filter(s -> s.kind() == MechanismSpec.Kind.RUSTY_HATCH)
              .findFirst()
              .orElseThrow();
      for (var part : MechanismRenderer.model(spec, Blocks.QUARTZ_BLOCK.defaultBlockState()))
        if (part.name().equals("handle"))
          part.mesh()
              .emit(
                  stack.last(),
                  buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),
                  light,
                  overlay);
    }
    stack.popPose();
    if (System.getProperty("malisis.verify.out") != null) rendered++;
  }
}
