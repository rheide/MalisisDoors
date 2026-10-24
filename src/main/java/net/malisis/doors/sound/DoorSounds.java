package net.malisis.doors.sound;

import java.util.*;
import net.malisis.doors.MalisisDoors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.*;

/** Original sound families, with closing impacts played when motion finishes. */
public final class DoorSounds {
  public static final DeferredRegister<SoundEvent> EVENTS =
      DeferredRegister.create(Registries.SOUND_EVENT, MalisisDoors.ID);
  private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> CUSTOM = new HashMap<>();

  static {
    for (String id :
        List.of(
            "slidingdooro",
            "jaildoor",
            "space_door_open",
            "space_door_close",
            "shoji_door",
            "rustyhatch_open",
            "rustyhatch_close",
            "carriage_opening",
            "carriage_closing",
            "carriage_closed",
            "rustydoor_opening",
            "rustydoor_closing",
            "rustydoor_closed"))
      CUSTOM.put(
          id,
          EVENTS.register(
              id,
              () ->
                  SoundEvent.createVariableRangeEvent(
                      ResourceLocation.fromNamespaceAndPath(MalisisDoors.ID, id))));
  }

  public static SoundEvent sound(String family, boolean opening, boolean finished) {
    return switch (family) {
      case "silent_door" -> null;
      case "vanilla_door" ->
          finished
              ? (opening ? null : SoundEvents.WOODEN_DOOR_CLOSE)
              : (opening ? SoundEvents.WOODEN_DOOR_OPEN : null);
      case "iron_door" ->
          finished
              ? (opening ? null : SoundEvents.IRON_DOOR_CLOSE)
              : (opening ? SoundEvents.IRON_DOOR_OPEN : null);
      case "wooden_trapdoor" ->
          finished
              ? (opening ? null : SoundEvents.WOODEN_TRAPDOOR_CLOSE)
              : (opening ? SoundEvents.WOODEN_TRAPDOOR_OPEN : null);
      case "iron_trapdoor" ->
          finished
              ? (opening ? null : SoundEvents.IRON_TRAPDOOR_CLOSE)
              : (opening ? SoundEvents.IRON_TRAPDOOR_OPEN : null);
      case "fence_gate" ->
          finished
              ? (opening ? null : SoundEvents.FENCE_GATE_CLOSE)
              : (opening ? SoundEvents.FENCE_GATE_OPEN : null);
      default -> {
        String id =
            switch (family) {
              case "glass_door" -> "slidingdooro";
              case "jail_door" -> "jaildoor";
              case "shoji_door" -> "shoji_door";
              case "pneumatic_door" -> opening ? "space_door_open" : "space_door_close";
              case "rusty_hatch" -> opening ? "rustyhatch_open" : "rustyhatch_close";
              case "carriage_door" ->
                  finished ? "carriage_closed" : opening ? "carriage_opening" : "carriage_closing";
              case "rustyDoor" ->
                  finished
                      ? "rustydoor_closed"
                      : opening ? "rustydoor_opening" : "rustydoor_closing";
              default -> null;
            };
        yield id == null
                || finished
                    && (opening || !family.equals("carriage_door") && !family.equals("rustyDoor"))
            ? null
            : CUSTOM.get(id).get();
      }
    };
  }

  public static void play(
      Level level, BlockPos pos, String family, boolean opening, boolean finished) {
    var event = sound(family, opening, finished);
    if (event != null && !level.isClientSide)
      level.playSound(null, pos, event, SoundSource.BLOCKS, 1, 1);
  }
}
