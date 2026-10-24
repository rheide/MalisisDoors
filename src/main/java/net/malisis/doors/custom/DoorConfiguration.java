package net.malisis.doors.custom;

import java.util.*;
import net.malisis.doors.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Immutable, validated item/block configuration; arbitrary registry block materials retain their
 * states.
 */
public record DoorConfiguration(
    String movement,
    String sound,
    int openingTicks,
    int autoCloseTicks,
    boolean paired,
    boolean proximity,
    Redstone redstone,
    String code,
    BlockState frame,
    BlockState top,
    BlockState bottom) {
  public enum Redstone {
    STANDARD,
    REDSTONE_ONLY,
    REDSTONE_LOCK,
    HAND_ONLY
  }

  public static final List<String> SOUNDS =
      List.of(
          "silent_door",
          "vanilla_door",
          "iron_door",
          "wooden_trapdoor",
          "iron_trapdoor",
          "glass_door",
          "jail_door",
          "pneumatic_door",
          "shoji_door",
          "rusty_hatch",
          "carriage_door",
          "fence_gate",
          "rustyDoor");

  public static DoorConfiguration defaults(DoorSpec spec) {
    String movement =
        switch (spec.movement()) {
          case SLIDE -> "sliding_door";
          case UP -> "sliding_up_door";
          case SPLIT -> "sliding_split_door";
          case CURTAIN -> "curtain";
        };
    if (spec.id().equals("customdoor") || spec.id().startsWith("minecraft:"))
      movement = "rotating_door";
    String sound =
        spec.movement() == DoorSpec.Movement.CURTAIN
            ? "silent_door"
            : spec.id().equals("jail_door")
                ? "jail_door"
                : spec.id().equals("shoji_door")
                    ? "shoji_door"
                    : spec.movement() == DoorSpec.Movement.SLIDE ? "glass_door" : "pneumatic_door";
    if (spec.id().equals("customdoor") || spec.id().startsWith("minecraft:"))
      sound = spec.redstoneOnly() ? "iron_door" : "vanilla_door";
    return new DoorConfiguration(
        movement,
        sound,
        spec.ticks(),
        0,
        spec.pairable(),
        false,
        spec.redstoneOnly() ? Redstone.REDSTONE_ONLY : Redstone.STANDARD,
        "",
        Blocks.OAK_PLANKS.defaultBlockState(),
        Blocks.GLASS.defaultBlockState(),
        Blocks.GLASS.defaultBlockState());
  }

  public DoorConfiguration validated() {
    String digits = code == null ? "" : code.replaceAll("[^0-9]", "");
    if (digits.length() > 6) digits = digits.substring(0, 6);
    return new DoorConfiguration(
        DoorMotion.known(movement) ? movement : "rotating_door",
        SOUNDS.contains(sound) ? sound : "vanilla_door",
        Math.max(1, Math.min(72000, openingTicks)),
        Math.max(0, Math.min(72000, autoCloseTicks)),
        paired,
        proximity,
        redstone == null ? Redstone.STANDARD : redstone,
        digits,
        frame == null ? Blocks.OAK_PLANKS.defaultBlockState() : frame,
        top == null ? Blocks.GLASS.defaultBlockState() : top,
        bottom == null ? Blocks.GLASS.defaultBlockState() : bottom);
  }

  public CompoundTag save() {
    var tag = new CompoundTag();
    tag.putString("movement", movement);
    tag.putString("sound", sound);
    tag.putInt("openingTicks", openingTicks);
    tag.putInt("autoCloseTicks", autoCloseTicks);
    tag.putBoolean("paired", paired);
    tag.putBoolean("proximity", proximity);
    tag.putString("redstone", redstone.name());
    tag.putString("code", code);
    tag.put("frame", NbtUtils.writeBlockState(frame));
    tag.put("top", NbtUtils.writeBlockState(top));
    tag.put("bottom", NbtUtils.writeBlockState(bottom));
    return tag;
  }

  public static DoorConfiguration read(
      CompoundTag tag, HolderLookup.Provider lookup, DoorConfiguration fallback) {
    Redstone policy = fallback.redstone;
    try {
      if (tag.contains("redstone")) policy = Redstone.valueOf(tag.getString("redstone"));
    } catch (IllegalArgumentException ignored) {
    }
    var blocks = lookup.lookupOrThrow(Registries.BLOCK);
    return new DoorConfiguration(
            tag.contains("movement") ? tag.getString("movement") : fallback.movement,
            tag.contains("sound") ? tag.getString("sound") : fallback.sound,
            tag.contains("openingTicks") ? tag.getInt("openingTicks") : fallback.openingTicks,
            tag.contains("autoCloseTicks") ? tag.getInt("autoCloseTicks") : fallback.autoCloseTicks,
            tag.contains("paired") ? tag.getBoolean("paired") : fallback.paired,
            tag.contains("proximity") ? tag.getBoolean("proximity") : fallback.proximity,
            policy,
            tag.contains("code") ? tag.getString("code") : fallback.code,
            tag.contains("frame")
                ? NbtUtils.readBlockState(blocks, tag.getCompound("frame"))
                : fallback.frame,
            tag.contains("top")
                ? NbtUtils.readBlockState(blocks, tag.getCompound("top"))
                : fallback.top,
            tag.contains("bottom")
                ? NbtUtils.readBlockState(blocks, tag.getCompound("bottom"))
                : fallback.bottom)
        .validated();
  }

  public static void writeItem(ItemStack stack, DoorConfiguration configuration) {
    var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    data.put("malisisDoor", configuration.save());
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
  }

  public static DoorConfiguration readItem(
      ItemStack stack, HolderLookup.Provider lookup, DoorConfiguration fallback) {
    var data = stack.get(DataComponents.CUSTOM_DATA);
    return data != null && data.copyTag().contains("malisisDoor")
        ? read(data.copyTag().getCompound("malisisDoor"), lookup, fallback)
        : fallback;
  }

  public static boolean material(ItemStack stack, boolean frame) {
    if (stack.isEmpty()) return false;
    if (!frame
        && (stack.is(Items.FLINT_AND_STEEL)
            || stack.is(Items.ENDER_PEARL)
            || stack.is(Items.WATER_BUCKET)
            || stack.is(Items.LAVA_BUCKET))) return true;
    return stack.getItem() instanceof BlockItem item
        && item.getBlock().defaultBlockState().getRenderShape() == RenderShape.MODEL;
  }

  public static BlockState materialState(ItemStack stack) {
    if (stack.is(Items.FLINT_AND_STEEL)) return Blocks.FIRE.defaultBlockState();
    if (stack.is(Items.ENDER_PEARL)) return Blocks.NETHER_PORTAL.defaultBlockState();
    if (stack.is(Items.WATER_BUCKET)) return Blocks.WATER.defaultBlockState();
    if (stack.is(Items.LAVA_BUCKET)) return Blocks.LAVA.defaultBlockState();
    return stack.getItem() instanceof BlockItem item
        ? item.getBlock().defaultBlockState()
        : Blocks.GLASS.defaultBlockState();
  }
}
