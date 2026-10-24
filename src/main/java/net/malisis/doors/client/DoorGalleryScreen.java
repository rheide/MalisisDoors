package net.malisis.doors.client;

import java.util.*;
import net.malisis.doors.MalisisDoors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Opt-in visual fixture for the same item renderer used by inventory and creative mode. */
public final class DoorGalleryScreen extends Screen {
  public final List<ItemStack> items = new ArrayList<>();

  public DoorGalleryScreen() {
    super(Component.literal("Malisis Doors inventory rendering"));
    BuiltInRegistries.ITEM.entrySet().stream()
        .filter(e -> e.getKey().location().getNamespace().equals(MalisisDoors.ID))
        .sorted(java.util.Comparator.comparing(e -> e.getKey().location().getPath()))
        .forEach(e -> items.add(new ItemStack(e.getValue())));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
    super.render(gui, mouseX, mouseY, partial);
    gui.fill(0, 0, width, height, 0xff303740);
    gui.drawCenteredString(font, title, width / 2, 8, 0xffffff);
    int columns = 10, cell = 56, left = (width - columns * cell) / 2;
    for (int i = 0; i < items.size(); i++) {
      int x = left + (i % columns) * cell, y = 26 + (i / columns) * 62;
      gui.fill(x, y, x + 52, y + 54, 0xff505a66);
      gui.pose().pushPose();
      gui.pose().translate(x + 10, y + 4, 0);
      gui.pose().scale(2, 2, 1);
      gui.renderItem(items.get(i), 0, 0);
      gui.pose().popPose();
      String label = BuiltInRegistries.ITEM.getKey(items.get(i).getItem()).getPath();
      while (font.width(label) > 50) label = label.substring(0, label.length() - 1);
      gui.drawString(font, label, x + 1, y + 42, 0xffffff, false);
      if (mouseX >= x && mouseX < x + 52 && mouseY >= y && mouseY < y + 54)
        gui.renderTooltip(font, items.get(i), mouseX, mouseY);
    }
  }
}
