package net.malisis.doors.checks;

import java.nio.file.*;
import net.malisis.doors.*;
import net.malisis.doors.client.DoorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "malisisdoors", value = Dist.CLIENT)
public final class ClientVerification {
  static int ticks, midpoints, generation, mechanismWait, verifiedEntities;
  static boolean done;
  static float min = 1, max = 0;
  static java.util.concurrent.CompletableFuture<Void> reload;
  static Object oldMesh;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) throws Exception {
    String output = System.getProperty("malisis.verify.out");
    if (output == null || done || System.getProperty("malisis.bench.out") != null) return;
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) return;
    mc.options.pauseOnLostFocus = false;
    mc.options.hideGui = true;
    mc.options.renderDistance().set(6);
    mc.options.enableVsync().set(false);
    ticks++;
    var sample = mc.level.getBlockEntity(new net.minecraft.core.BlockPos(0, 64, 0));
    if (sample instanceof DoorBlockEntity door) {
      float p = door.progress(0);
      min = Math.min(min, p);
      max = Math.max(max, p);
      if (p > 0 && p < 1) midpoints++;
    }
    if (ticks < 240) return;
    if (reload == null) {
      if (min > .001 || max < .999 || midpoints == 0)
        throw new IllegalStateException(
            "No observed continuous door motion: " + min + ".." + max + " mid=" + midpoints);
      generation = DoorRenderer.resourceGeneration;
      oldMesh =
          DoorRenderer.MESHES.get(
              MalisisDoors.SPECS.getFirst(),
              k -> {
                throw new IllegalStateException("First descriptor not visible");
              });
      Screenshot.grab(
          Path.of(output).toFile(),
          "modern-before-reload.png",
          mc.getMainRenderTarget(),
          message -> {});
      reload = mc.reloadResourcePacks();
      return;
    }
    if (!reload.isDone() || ticks < 300) return;
    reload.join();
    if (DoorRenderer.resourceGeneration <= generation)
      throw new IllegalStateException("Resource reload not applied");
    Object newMesh =
        DoorRenderer.MESHES.get(
            MalisisDoors.SPECS.getFirst(),
            k -> {
              throw new IllegalStateException("Mesh not rebuilt after reload");
            });
    if (newMesh == oldMesh) throw new IllegalStateException("Stale resource mesh after reload");
    var atlas = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    int textures = 0, entities = 0;
    for (var spec : MalisisDoors.SPECS)
      for (String suffix : new String[] {"_lower", "_upper", "_side"}) {
        var id =
            ResourceLocation.fromNamespaceAndPath(
                "malisisdoors", "blocks/" + spec.texture() + suffix);
        if (!atlas.apply(id).contents().name().equals(id))
          throw new IllegalStateException("Missing atlas texture " + id);
        textures++;
      }
    for (int i = 0; i < 88; i++)
      if (mc.level.getBlockEntity(new net.minecraft.core.BlockPos((i % 11) * 4, 64, (i / 11) * 4))
          instanceof DoorBlockEntity) entities++;
    if (entities != 88 && mechanismWait < 100)
      throw new IllegalStateException("Expected 88 fixture entities, got " + entities);
    if (mechanismWait < 100) verifiedEntities = entities;
    if (DoorRenderer.MESHES.size() == 0)
      throw new IllegalStateException("Renderer never populated mesh cache");
    if (mechanismWait++ == 0) {
      mc.player.connection.sendCommand("malisisverifyview mechanisms");
      return;
    }
    if (mechanismWait < 100) return;
    for (var entry : net.malisis.doors.mechanism.Mechanisms.BLOCKS)
      if (!net.malisis.doors.client.MechanismRenderer.RENDERED.contains(entry.get().spec.id()))
        throw new IllegalStateException("Mechanism was never rendered: " + entry.get().spec.id());
    if (mechanismWait == 100) {
      Screenshot.grab(
          Path.of(output).toFile(), "mechanisms.png", mc.getMainRenderTarget(), message -> {});
      mc.player.connection.sendCommand("malisisverifyview flat");
      return;
    }
    if (mechanismWait < 180) return;
    for (var entry : net.malisis.doors.flat.FlatBlocks.BLOCKS)
      if (!net.malisis.doors.client.FlatRenderer.RENDERED.contains(entry.getId()))
        throw new IllegalStateException("Flat family was never rendered: " + entry.getId());
    if (!FactoryClientVerification.step(mc, output)) return;
    Path folder = Path.of(output);
    Files.createDirectories(folder);
    Screenshot.grab(
        folder.toFile(), "modern-showroom.png", mc.getMainRenderTarget(), message -> {});
    Files.writeString(
        folder.resolve("client-verification.txt"),
        "PASS connected to dedicated server; "
            + verifiedEntities
            + " doors, "
            + textures
            + " resolved sprites, "
            + DoorRenderer.MESHES.size()
            + " cached descriptor meshes; "
            + net.malisis.doors.client.MechanismRenderer.RENDERED.size()
            + " mechanism families rendered; "
            + net.malisis.doors.client.FlatRenderer.RENDERED.size()
            + " flat families rendered; continuous motion observed ("
            + midpoints
            + " intermediate ticks), resource reload rebuilt meshes; screenshot saved.\n");
    done = true;
    if (Boolean.getBoolean("malisis.verify.exit")) mc.stop();
  }
}
