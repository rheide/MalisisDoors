package net.malisis.doors.client;

import net.malisis.doors.MalisisDoors;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = "malisisdoors", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(MalisisDoors.DOOR_ENTITY.get(), DoorRenderer::new);
    event.registerBlockEntityRenderer(
        net.malisis.doors.mechanism.Mechanisms.ENTITY.get(), MechanismRenderer::new);
    event.registerBlockEntityRenderer(
        net.malisis.doors.flat.FlatBlocks.ENTITY.get(), FlatRenderer::new);
    event.registerBlockEntityRenderer(
        net.malisis.doors.field.FieldContent.ENTITY.get(), FieldRenderer::new);
  }

  @SubscribeEvent
  public static void itemProperties(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
    event.enqueueWork(
        () ->
            net.minecraft.client.renderer.item.ItemProperties.register(
                net.malisis.doors.field.FieldContent.PROJECTOR.get(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    "malisisdoors", "field_state"),
                (stack, level, entity, seed) -> {
                  var tag = net.malisis.doors.field.ProjectorItem.data(stack);
                  if (!tag.contains("start")) return 0;
                  var hit = net.minecraft.client.Minecraft.getInstance().hitResult;
                  if (!(hit instanceof net.minecraft.world.phys.BlockHitResult block)) return 1;
                  int area =
                      net.malisis.doors.field.ProjectorItem.area(
                          net.minecraft.core.BlockPos.of(tag.getLong("start")),
                          block.getBlockPos().relative(block.getDirection()));
                  return area > 0
                          && net.malisis.doors.field.ProjectorItem.energy(stack) >= area * 20
                      ? 3
                      : 2;
                }));
  }

  @SubscribeEvent
  public static void menus(RegisterMenuScreensEvent event) {
    event.register(net.malisis.doors.factory.FactoryContent.MENU.get(), FactoryScreen::new);
    event.register(net.malisis.doors.factory.FactoryContent.CODE_MENU.get(), CodeScreen::new);
  }

  @SubscribeEvent
  public static void reload(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(
        (ResourceManagerReloadListener)
            manager -> {
              DoorRenderer.MESHES.clear();
              MechanismRenderer.MODELS.clear();
              FlatRenderer.MODELS.clear();
              CustomMeshes.CACHE.clear();
              DoorRenderer.resourceGeneration++;
            });
  }
}
