package net.malisis.doors.checks;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.malisis.core.client.RenderMetrics;
import net.malisis.doors.*;
import net.malisis.doors.mechanism.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = "malisisdoors", value = Dist.CLIENT)
public final class ModernBenchmark {
  private static final String OUT = System.getProperty("malisis.bench.out");
  private static final com.sun.management.ThreadMXBean CPU =
      (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
  private static int index = -1, warm, frames;
  private static long settle, wall, cpu, allocated;
  private static PrintWriter output;
  private static String stage;
  private static boolean measuring, done;

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) throws Exception {
    if (OUT == null || done) return;
    var mc = Minecraft.getInstance();
    if (mc.level == null || mc.player == null) return;
    mc.options.pauseOnLostFocus = false;
    mc.options.hideGui = true;
    mc.options.enableVsync().set(false);
    mc.options.renderDistance().set(6);
    mc.options.bobView().set(false);
    if (index < 0) {
      Files.createDirectories(Path.of(OUT));
      CPU.setThreadAllocatedMemoryEnabled(true);
      CPU.setThreadCpuTimeEnabled(true);
      output = new PrintWriter(Path.of(OUT, "frames.csv").toFile());
      output.println(
          "stage,frame,complete_ms,client_cpu_ms,allocated_bytes,door_callback_ms,door_allocated_bytes,door_calls,door_vertices");
      Files.writeString(
          Path.of(OUT, "environment.txt"),
          "renderer="
              + GL11.glGetString(GL11.GL_RENDERER)
              + "\nOpenGL="
              + GL11.glGetString(GL11.GL_VERSION)
              + "\n"
              + "fixtures=64 roots; garage height3; 8x8 spacing4; camera15,79,-13 yaw0 pitch23;"
              + " 1280x720; fancy, AO, view6, no shaders; 180 frames after 12s+90frames"
              + " settling\n");
      advance(mc);
    }
  }

  private static void advance(Minecraft mc) throws Exception {
    index++;
    if (index >= 18) {
      output.close();
      Files.writeString(Path.of(OUT, "COMPLETE"), "PASS 18 matched fixture phases\n");
      done = true;
      mc.stop();
      return;
    }
    stage =
        BenchmarkFixtures.FAMILIES[index / 3]
            + "_"
            + new String[] {"closed", "opened", "midpoint"}[index % 3];
    if (index % 3 == 0)
      mc.player.connection.sendCommand("malisisbench " + BenchmarkFixtures.FAMILIES[index / 3]);
    settle = System.nanoTime();
    warm = frames = 0;
    measuring = false;
    System.out.println("[modern-bench] settle " + stage);
  }

  @SubscribeEvent
  public static void pre(RenderFrameEvent.Pre event) {
    if (OUT == null || done || index < 0) return;
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    int target = index % 3;
    double now = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
    for (int row = 0; row < 8; row++)
      for (int col = 0; col < 8; col++)
        for (int h = 0; h < (index / 3 == 3 ? 3 : 1); h++) {
          var pos = new BlockPos(col * 4, 64 + h, row * 4);
          var tile = mc.level.getBlockEntity(pos);
          if (tile instanceof DoorBlockEntity door) {
            var state = door.getBlockState();
            if (state.getValue(DoorBlock.OPEN) != (target != 0))
              mc.level.setBlock(pos, state.setValue(DoorBlock.OPEN, target != 0), 2);
            freeze(door.animation, target, now, door.configuration.openingTicks());
          } else if (tile instanceof MechanismEntity mechanism) {
            var state = mechanism.getBlockState();
            if (state.getValue(MechanismBlock.OPEN) != (target != 0))
              mc.level.setBlock(pos, state.setValue(MechanismBlock.OPEN, target != 0), 2);
            freeze(mechanism.animation, target, now, mechanism.duration());
          }
        }
    RenderMetrics.reset();
    GL11.glFinish();
    cpu = CPU.getCurrentThreadCpuTime();
    allocated = CPU.getThreadAllocatedBytes(Thread.currentThread().threadId());
    wall = System.nanoTime();
  }

  private static void freeze(
      net.malisis.core.animation.DoorTimeline timeline, int target, double now, int duration) {
    timeline.reset(target == 1, now);
    if (target == 2) {
      timeline.reset(false, now - duration * .5);
      timeline.update(true, now - duration * .5, duration);
    }
  }

  @SubscribeEvent
  public static void post(RenderFrameEvent.Post event) throws Exception {
    if (OUT == null || done || index < 0 || wall == 0) return;
    GL11.glFinish();
    RenderMetrics.active = false;
    long end = System.nanoTime();
    var mc = Minecraft.getInstance();
    if (!measuring) {
      if (++warm >= 90 && end - settle > 12_000_000_000L) {
        measuring = true;
        System.out.println("[modern-bench] measure " + stage);
      }
      return;
    }
    output.printf(
        Locale.ROOT,
        "%s,%d,%.6f,%.6f,%d,%.6f,%d,%d,%d%n",
        stage,
        ++frames,
        (end - wall) / 1e6,
        (CPU.getCurrentThreadCpuTime() - cpu) / 1e6,
        CPU.getThreadAllocatedBytes(Thread.currentThread().threadId()) - allocated,
        RenderMetrics.nanos / 1e6,
        RenderMetrics.bytes,
        RenderMetrics.calls,
        RenderMetrics.vertices);
    if (frames >= 180) {
      output.flush();
      Screenshot.grab(
          Path.of(OUT).toFile(),
          "fixture-" + stage + ".png",
          mc.getMainRenderTarget(),
          message -> {});
      advance(mc);
    }
  }
}
