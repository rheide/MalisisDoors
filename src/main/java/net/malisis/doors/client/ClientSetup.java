package net.malisis.doors.client;
import net.malisis.doors.MalisisDoors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
@EventBusSubscriber(modid="malisisdoors",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
 @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(MalisisDoors.DOOR_ENTITY.get(),DoorRenderer::new);}
 @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){event.registerReloadListener((ResourceManagerReloadListener)manager->{DoorRenderer.MESHES.clear();DoorRenderer.resourceGeneration++;});}
}
