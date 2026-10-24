package net.malisis.doors.client;

import java.util.*;
import net.malisis.doors.custom.*;
import net.malisis.doors.factory.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FactoryScreen extends AbstractContainerScreen<FactoryMenu> {
  private DoorConfiguration draft;
  private boolean draftMode;
  private int tab;
  private final List<AbstractWidget> properties = new ArrayList<>(),
      materials = new ArrayList<>(),
      access = new ArrayList<>();
  private CycleButton<String> movement, sound;
  private CycleButton<DoorConfiguration.Redstone> redstone;
  private CycleButton<Boolean> paired, proximity, mode;
  private EditBox opening, closing, code;

  public FactoryScreen(FactoryMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 352;
    imageHeight = 282;
    inventoryLabelX = 88;
    inventoryLabelY = 188;
  }

  private <T extends AbstractWidget> T add(T widget, List<AbstractWidget> group) {
    addRenderableWidget(widget);
    group.add(widget);
    return widget;
  }

  private EditBox number(int x, int y, int w, String initial, int max, List<AbstractWidget> group) {
    EditBox box = new EditBox(font, leftPos + x, topPos + y, w, 18, Component.empty());
    box.setMaxLength(max);
    box.setFilter(value -> value.matches("[0-9]*"));
    box.setValue(initial);
    return add(box, group);
  }

  @Override
  protected void init() {
    if (movement != null) {
      draft = settings();
      draftMode = mode.getValue();
    }
    super.init();
    properties.clear();
    materials.clear();
    access.clear();
    var c = draft == null ? menu.configuration : draft;
    String[] tabs = {"Properties", "Materials", "Access code"};
    for (int i = 0; i < tabs.length; i++) {
      final int index = i;
      addRenderableWidget(
          Button.builder(
                  Component.literal(tabs[i]),
                  button -> {
                    tab = index;
                    visibility();
                  })
              .bounds(leftPos + 8 + i * 112, topPos + 15, 108, 18)
              .build());
    }
    movement =
        add(
            CycleButton.<String>builder(value -> Component.translatable("door_movement." + value))
                .withValues(DoorMotion.FACTORY)
                .withInitialValue(
                    DoorMotion.FACTORY.contains(c.movement()) ? c.movement() : "rotating_door")
                .create(leftPos + 10, topPos + 46, 210, 20, Component.literal("Movement")),
            properties);
    opening = number(266, 46, 72, Integer.toString(c.openingTicks()), 5, properties);
    closing = number(266, 78, 72, Integer.toString(c.autoCloseTicks()), 5, properties);
    paired =
        add(
            CycleButton.onOffBuilder(c.paired())
                .create(leftPos + 10, topPos + 78, 116, 20, Component.literal("Paired")),
            properties);
    proximity =
        add(
            CycleButton.onOffBuilder(c.proximity())
                .create(leftPos + 132, topPos + 78, 128, 20, Component.literal("Proximity")),
            properties);
    redstone =
        add(
            CycleButton.<DoorConfiguration.Redstone>builder(
                    value ->
                        Component.literal(
                            switch (value) {
                              case STANDARD -> "Standard";
                              case REDSTONE_ONLY -> "Redstone only";
                              case REDSTONE_LOCK -> "Powered lock";
                              case HAND_ONLY -> "Hand only";
                            }))
                .withValues(DoorConfiguration.Redstone.values())
                .withInitialValue(c.redstone())
                .create(leftPos + 10, topPos + 110, 160, 20, Component.literal("Redstone")),
            properties);
    sound =
        add(
            CycleButton.<String>builder(
                    value -> Component.translatable("gui.door_factory.door_sound." + value))
                .withValues(DoorConfiguration.SOUNDS)
                .withInitialValue(c.sound())
                .create(leftPos + 178, topPos + 110, 160, 20, Component.literal("Sound")),
            properties);
    mode =
        add(
            CycleButton.booleanBuilder(
                    Component.literal("Create new"), Component.literal("Edit existing"))
                .withInitialValue(draft == null ? menu.createMode : draftMode)
                .create(leftPos + 10, topPos + 46, 210, 20, Component.literal("Mode")),
            materials);
    code = number(122, 48, 108, c.code(), 6, access);
    for (int i = 0; i < 10; i++) {
      final int digit = i;
      int column = i == 0 ? 1 : (i - 1) % 3, row = i == 0 ? 3 : (i - 1) / 3;
      add(
          Button.builder(
                  Component.literal(Integer.toString(i)),
                  button -> {
                    if (code.getValue().length() < 6) code.setValue(code.getValue() + digit);
                  })
              .bounds(leftPos + 134 + column * 24, topPos + 72 + row * 17, 22, 16)
              .build(),
          access);
    }
    add(
        Button.builder(Component.literal("C"), button -> code.setValue(""))
            .bounds(leftPos + 134, topPos + 123, 22, 16)
            .build(),
        access);
    addRenderableWidget(
        Button.builder(Component.literal("Create / Edit"), button -> send(true))
            .bounds(leftPos + 252, topPos + 158, 88, 20)
            .build());
    visibility();
  }

  private void visibility() {
    for (var item : properties) item.visible = tab == 0;
    for (var item : materials) item.visible = tab == 1;
    for (var item : access) item.visible = tab == 2;
  }

  private int value(EditBox box, int fallback) {
    try {
      return Integer.parseInt(box.getValue());
    } catch (NumberFormatException error) {
      return fallback;
    }
  }

  private DoorConfiguration settings() {
    var original = menu.configuration;
    return new DoorConfiguration(
            movement.getValue(),
            sound.getValue(),
            value(opening, 6),
            value(closing, 0),
            paired.getValue(),
            proximity.getValue(),
            redstone.getValue(),
            code.getValue(),
            original.frame(),
            original.top(),
            original.bottom())
        .validated();
  }

  public void send(boolean produce) {
    PacketDistributor.sendToServer(
        new FactoryNetwork.Configure(
            menu.containerId, menu.position, settings().save(), mode.getValue(), produce));
  }

  @Override
  public void onClose() {
    send(false);
    super.onClose();
  }

  @Override
  protected void renderBg(GuiGraphics gui, float partial, int mouseX, int mouseY) {
    gui.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xffc6c6c6);
    gui.fill(
        leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xffdddddd);
    for (var slot : menu.slots) {
      int x = leftPos + slot.x, y = topPos + slot.y;
      gui.fill(x - 1, y - 1, x + 17, y + 17, 0xff777777);
      gui.fill(x, y, x + 16, y + 16, 0xffaaaaaa);
    }
  }

  @Override
  protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
    gui.drawString(font, title, 8, 5, 0x333333, false);
    gui.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x333333, false);
    if (tab == 0) {
      gui.drawString(font, "Opening ticks", 240, 35, 0x333333, false);
      gui.drawString(font, "Auto-close (0 = off)", 224, 67, 0x333333, false);
    } else if (tab == 1) {
      gui.drawString(font, "New: frame + top panel + bottom panel", 10, 78, 0x333333, false);
      gui.drawString(font, "Edit: place a door in the Edit slot", 10, 94, 0x333333, false);
      gui.drawString(
          font, "Panels also accept water, lava, fire and portal items", 10, 110, 0x333333, false);
    } else
      gui.drawString(font, "Up to six digits; leave empty for no code", 58, 36, 0x333333, false);
    String[] labels = {"Frame", "Top", "Bottom", "Edit", "Output"};
    int[] x = {14, 52, 81, 153, 213};
    for (int i = 0; i < labels.length; i++)
      gui.drawString(font, labels[i], x[i], 147, 0x333333, false);
    if (menu.status.get(0) != 0)
      gui.drawString(font, "Check inputs and output space", 12, 180, 0xaa0000, false);
  }

  @Override
  public void render(GuiGraphics gui, int mouseX, int mouseY, float partial) {
    super.render(gui, mouseX, mouseY, partial);
    renderTooltip(gui, mouseX, mouseY);
  }
}
