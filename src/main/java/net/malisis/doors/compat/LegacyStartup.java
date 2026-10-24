package net.malisis.doors.compat;

import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@EventBusSubscriber(modid = "malisisdoors")
public final class LegacyStartup {
  @SubscribeEvent
  public static void starting(ServerAboutToStartEvent event) {
    LegacyItems.active =
        LegacyRegistry.forWorld(event.getServer().getWorldPath(LevelResource.ROOT)).orElse(null);
  }

  @SubscribeEvent
  public static void stopped(ServerStoppedEvent event) {
    LegacyRegistry.release(event.getServer().getWorldPath(LevelResource.ROOT));
    LegacyItems.active = null;
  }
}
