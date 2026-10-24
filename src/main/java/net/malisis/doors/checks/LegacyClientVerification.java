package net.malisis.doors.checks;

import java.nio.file.*;
import net.malisis.doors.*;
import net.malisis.doors.client.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "malisisdoors", value = Dist.CLIENT)
public final class LegacyClientVerification {
  static int ticks;
  static boolean done;
  static java.util.concurrent.CompletableFuture<Void> reload;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) throws Exception {
    String output = System.getProperty("malisis.compat.client.out");
    if (output == null || done) return;
    var mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) return;
    mc.options.pauseOnLostFocus = false;
    mc.options.hideGui = true;
    ticks++;
    mc.player.setYRot((ticks / 60 % 4) * 90);
    mc.player.setXRot(25);
    if (ticks < 260) return;
    if (reload == null) {
      if (DoorRenderer.MESHES.size() == 0 || MechanismRenderer.MODELS.size() == 0)
        throw new IllegalStateException("Converted doors were not rendered");
      Screenshot.grab(
          Path.of(output).toFile(), "converted-world.png", mc.getMainRenderTarget(), message -> {});
      reload = mc.reloadResourcePacks();
      return;
    }
    if (!reload.isDone() || ticks < 340) return;
    reload.join();
    boolean custom = false;
    var cases =
        com.google.gson.JsonParser.parseString(
                Files.readString(Path.of(System.getProperty("malisis.compat.verify.cases"))))
            .getAsJsonArray();
    int entities = 0;
    for (var entry : cases) {
      var c = entry.getAsJsonObject();
      if (c.has("late")) continue;
      var pos = new BlockPos(c.get("x").getAsInt(), c.get("y").getAsInt(), c.get("z").getAsInt());
      if (mc.level.getBlockEntity(pos) != null) entities++;
      if (c.get("item").getAsString().equals("malisisdoors:customdooritem")) {
        if (!(mc.level.getBlockEntity(pos) instanceof DoorBlockEntity door)
            || !door.configuration.code().equals("741852")
            || !door.configuration
                .top()
                .is(net.minecraft.world.level.block.Blocks.RED_STAINED_GLASS))
          throw new IllegalStateException("Converted client custom-door configuration");
        custom = true;
      }
    }
    if (!custom || entities < 47)
      throw new IllegalStateException("Converted entities missing on client: " + entities);
    Files.writeString(
        Path.of(output, "migration-client.txt"),
        "PASS actual client login to converted save, "
            + entities
            + " original-family entities, custom settings/materials, door/mechanism rendering and"
            + " resource reload; software renderer\n");
    done = true;
    mc.stop();
  }
}
