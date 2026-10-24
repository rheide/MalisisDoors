package net.malisis.doors.checks;

import java.nio.file.*;
import net.malisis.doors.client.*;
import net.malisis.doors.custom.DoorMotion;
import net.malisis.doors.factory.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.world.level.block.DoorBlock;

/** Real native menus and payloads; only invoked by the explicitly enabled fixture runner. */
public final class FactoryClientVerification {
  private static int stage, wait;

  private static void shot(Minecraft mc, String output, String name) {
    Screenshot.grab(Path.of(output).toFile(), name, mc.getMainRenderTarget(), message -> {});
  }

  private static void click(Minecraft mc, String label) {
    if (mc.screen == null) throw new IllegalStateException("No screen for " + label);
    for (var child : mc.screen.children())
      if (child instanceof Button button && button.getMessage().getString().equals(label)) {
        button.onPress();
        return;
      }
    throw new IllegalStateException("Missing button " + label);
  }

  private static void type(Minecraft mc, String value) {
    for (var child : mc.screen.children())
      if (child instanceof EditBox field) {
        field.setValue(value);
        return;
      }
    throw new IllegalStateException("Missing code field");
  }

  public static boolean step(Minecraft mc, String output) throws Exception {
    if (stage == 0) {
      mc.player.connection.sendCommand("malisisverifyview custom");
      stage = 1;
      wait = 0;
      return false;
    }
    if (++wait < 60) return false;
    if (stage == 1) {
      for (String movement : DoorMotion.FACTORY)
        if (!DoorRenderer.CUSTOM_MOVEMENTS.contains(movement))
          throw new IllegalStateException("Custom movement not rendered: " + movement);
      shot(mc, output, "custom-movements.png");
      mc.player.connection.sendCommand("malisisverifyview factory");
      stage = 2;
      wait = 0;
      return false;
    }
    if (stage == 2) {
      if (!(mc.screen instanceof FactoryScreen))
        throw new IllegalStateException("Factory screen not opened");
      shot(mc, output, "factory-properties.png");
      click(mc, "Materials");
      stage = 3;
      wait = 0;
      return false;
    }
    if (stage == 3) {
      shot(mc, output, "factory-materials.png");
      click(mc, "Create / Edit");
      stage = 4;
      wait = 0;
      return false;
    }
    if (stage == 4) {
      if (!(mc.player.containerMenu instanceof FactoryMenu menu)
          || menu.getSlot(4).getItem().isEmpty()
          || menu.getSlot(0).getItem().getCount() != 2)
        throw new IllegalStateException("Network create did not consume inputs and emit output");
      shot(mc, output, "factory-created.png");
      mc.screen.onClose();
      mc.player.connection.sendCommand("malisisverifyview code");
      stage = 5;
      wait = 0;
      return false;
    }
    if (stage == 5) {
      if (!(mc.screen instanceof CodeScreen))
        throw new IllegalStateException("Code keypad screen not opened");
      shot(mc, output, "code-keypad.png");
      type(mc, "000000");
      click(mc, "Open");
      stage = 6;
      wait = 0;
      return false;
    }
    if (stage == 6) {
      if (!(mc.player.containerMenu instanceof CodeMenu menu)
          || menu.status.get(0) != 1
          || mc.level.getBlockState(FactoryVerification.CODED).getValue(DoorBlock.OPEN))
        throw new IllegalStateException("Wrong code was not rejected");
      shot(mc, output, "code-rejected.png");
      type(mc, "123456");
      click(mc, "Open");
      stage = 7;
      wait = 0;
      return false;
    }
    if (stage == 7) {
      if (mc.screen instanceof CodeScreen
          || !mc.level.getBlockState(FactoryVerification.CODED).getValue(DoorBlock.OPEN))
        throw new IllegalStateException("Correct code did not open the door");
      Files.writeString(
          Path.of(output, "factory-client.txt"),
          "PASS all 18 custom movements rendered, real Factory menu click and server payload"
              + " consumed inputs and created output, real keypad rejected an incorrect code and"
              + " opened for the correct code\n");
      mc.player.connection.sendCommand("malisisverifyview vanilla");
      stage = 8;
      wait = 0;
      return false;
    }
    if (stage == 8) {
      int doors = 0, flats = 0;
      for (var pos : net.minecraft.core.BlockPos.betweenClosed(256, 64, 0, 284, 64, 24)) {
        var tile = mc.level.getBlockEntity(pos);
        if (tile instanceof net.malisis.doors.DoorBlockEntity) doors++;
        if (tile instanceof net.malisis.doors.flat.FlatEntity) flats++;
      }
      if (doors == 0 || flats == 0)
        throw new IllegalStateException("Vanilla animated entities not synchronized");
      shot(mc, output, "vanilla-animations.png");
      mc.player.connection.sendCommand("malisisverifyview field");
      stage = 9;
      wait = 0;
      return false;
    }
    if (stage == 9) {
      if (FieldRenderer.rendered == 0) throw new IllegalStateException("No forcefields rendered");
      shot(mc, output, "forcefields.png");
      Files.writeString(
          Path.of(output, "vanilla-field-client.txt"),
          "PASS native vanilla animated entities and forcefield render passes\n");
      net.malisis.doors.vanilla.Settings.use3DItems = true;
      mc.setScreen(new DoorGalleryScreen());
      stage = 10;
      wait = 0;
      return false;
    }
    if (stage == 10) {
      if (!(mc.screen instanceof DoorGalleryScreen gallery)
          || gallery.items.size() != 49
          || DoorItems.rendered < 45)
        throw new IllegalStateException(
            "Incomplete item gallery: "
                + (mc.screen instanceof DoorGalleryScreen g ? g.items.size() : -1)
                + " renders="
                + DoorItems.rendered);
      shot(mc, output, "creative-item-gallery.png");
      net.malisis.doors.vanilla.Settings.use3DItems = false;
      mc.setScreen(null);
      Files.writeString(
          Path.of(output, "item-client.txt"),
          "PASS all 49 original creative items present; native three-dimensional item rendering"
              + " exercised\n");
      stage = 11;
      return true;
    }
    return stage == 11;
  }
}
