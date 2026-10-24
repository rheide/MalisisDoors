package net.malisis.doors.vanilla;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = "malisisdoors", bus = EventBusSubscriber.Bus.MOD)
public final class Settings {
  public static volatile boolean modifyVanillaDoors = true, use3DItems = false;
  public static final ModConfigSpec SERVER, CLIENT;
  private static final ModConfigSpec.BooleanValue MODIFY, ITEMS;

  static {
    var server = new ModConfigSpec.Builder();
    MODIFY =
        server
            .comment("Animate vanilla doors, trapdoors and fence gates. Requires a world restart.")
            .worldRestart()
            .define("modifyVanillaDoors", true);
    SERVER = server.build();
    var client = new ModConfigSpec.Builder();
    ITEMS =
        client
            .comment("Render ordinary door items as three-dimensional models.")
            .define("use3DItems", false);
    CLIENT = client.build();
  }

  @SubscribeEvent
  public static void loaded(ModConfigEvent.Loading event) {
    if (event.getConfig().getSpec() == SERVER) modifyVanillaDoors = MODIFY.get();
    if (event.getConfig().getSpec() == CLIENT) use3DItems = ITEMS.get();
  }

  @SubscribeEvent
  public static void reloaded(ModConfigEvent.Reloading event) {
    if (event.getConfig().getSpec() == CLIENT) use3DItems = ITEMS.get();
  }
}
