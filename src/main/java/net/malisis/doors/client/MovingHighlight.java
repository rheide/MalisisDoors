package net.malisis.doors.client;

import net.malisis.core.collision.MovingGeometry;
import net.malisis.doors.*;
import net.malisis.doors.mechanism.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

@EventBusSubscriber(modid = "malisisdoors", value = Dist.CLIENT)
public final class MovingHighlight {
  @SubscribeEvent
  public static void highlight(RenderHighlightEvent.Block event) {
    var mc = Minecraft.getInstance();
    var pos = event.getTarget().getBlockPos();
    var tile = mc.level.getBlockEntity(pos);
    MovingGeometry geometry =
        tile instanceof MovingGeometry moving
            ? moving
            : tile instanceof PartEntity part
                ? part.origin()
                : mc.level.getBlockState(pos).getBlock()
                        instanceof net.minecraft.world.level.block.DoorBlock
                    ? DoorInteractions.find(mc.level, pos, mc.level.getBlockState(pos))
                    : null;
    if (geometry == null || !geometry.alive()) return;
    var camera = event.getCamera().getPosition();
    var buffer = event.getMultiBufferSource().getBuffer(RenderType.lines());
    for (var box : geometry.selectionBoxes())
      LevelRenderer.renderLineBox(
          event.getPoseStack(), buffer, box.move(-camera.x, -camera.y, -camera.z), 0, 0, 0, .4f);
    event.setCanceled(true);
  }
}
