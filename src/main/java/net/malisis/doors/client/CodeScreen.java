package net.malisis.doors.client;

import net.malisis.doors.factory.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CodeScreen extends AbstractContainerScreen<CodeMenu> {
  private EditBox code;

  public CodeScreen(CodeMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 154;
    imageHeight = 146;
    inventoryLabelY = -100;
  }

  @Override
  protected void init() {
    super.init();
    code = new EditBox(font, leftPos + 22, topPos + 23, 110, 20, Component.literal("Code"));
    code.setMaxLength(6);
    code.setFilter(value -> value.matches("[0-9]*"));
    addRenderableWidget(code);
    for (int i = 0; i < 10; i++) {
      final int digit = i;
      int x = i == 0 ? 1 : (i - 1) % 3, y = i == 0 ? 3 : (i - 1) / 3;
      addRenderableWidget(
          Button.builder(
                  Component.literal("" + i),
                  button -> {
                    if (code.getValue().length() < 6) code.setValue(code.getValue() + digit);
                  })
              .bounds(leftPos + 23 + x * 36, topPos + 50 + y * 22, 34, 20)
              .build());
    }
    addRenderableWidget(
        Button.builder(Component.literal("C"), button -> code.setValue(""))
            .bounds(leftPos + 23, topPos + 116, 34, 20)
            .build());
    addRenderableWidget(
        Button.builder(Component.literal("Open"), button -> submit())
            .bounds(leftPos + 95, topPos + 116, 34, 20)
            .build());
    setInitialFocus(code);
  }

  public void submit() {
    PacketDistributor.sendToServer(
        new FactoryNetwork.SubmitCode(menu.containerId, menu.position, code.getValue()));
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (key == 257 || key == 335) {
      submit();
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  @Override
  protected void renderBg(GuiGraphics gui, float partial, int x, int y) {
    gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
  }

  @Override
  protected void renderLabels(GuiGraphics gui, int x, int y) {
    gui.drawString(font, title, 12, 8, 0x333333, false);
    if (menu.status.get(0) != 0) gui.drawString(font, "Incorrect code", 30, 137, 0xaa0000, false);
  }
}
