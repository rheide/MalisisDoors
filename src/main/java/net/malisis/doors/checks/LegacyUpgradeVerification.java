package net.malisis.doors.checks;

import com.google.gson.*;
import java.nio.file.*;
import net.malisis.doors.*;
import net.malisis.doors.compat.*;
import net.malisis.doors.custom.*;
import net.malisis.doors.factory.*;
import net.malisis.doors.field.*;
import net.malisis.doors.mechanism.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Explicit opt-in acceptance against a copied save produced by the original mods. */
@EventBusSubscriber(modid = "malisisdoors")
public final class LegacyUpgradeVerification {
  @SubscribeEvent
  public static void login(
      net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
    if (Boolean.getBoolean("malisis.compat.verify.hold")
        && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
      player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
      player.connection.teleport(35, 80, 30, 0, 25);
    }
  }

  static int checks, ticks;
  static Path pending;
  static com.google.gson.JsonArray loadedCases = new JsonArray();

  static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new IllegalStateException(message);
  }

  static void configuration(DoorConfiguration c) {
    check(c.movement().equals("rotate_slide"), "movement " + c);
    check(c.sound().equals("shoji_door"), "sound");
    check(c.openingTicks() == 27 && c.autoCloseTicks() == 90, "timers");
    check(
        !c.paired() && c.proximity() && c.redstone() == DoorConfiguration.Redstone.HAND_ONLY,
        "policy");
    check(c.code().equals("741852"), "code");
    check(c.frame().is(Blocks.DARK_OAK_PLANKS), "frame " + c.frame());
    check(c.top().is(Blocks.RED_STAINED_GLASS), "top " + c.top());
    check(c.bottom().is(Blocks.CRACKED_STONE_BRICKS), "bottom " + c.bottom());
  }

  @SubscribeEvent
  public static void started(ServerStartedEvent event) {
    String output = System.getProperty("malisis.compat.verify.out");
    if (output == null) return;
    Path out = Path.of(output);
    try {
      var server = event.getServer();
      var level = server.overworld();
      var cases =
          JsonParser.parseString(
                  Files.readString(Path.of(System.getProperty("malisis.compat.verify.cases"))))
              .getAsJsonArray();
      for (var entry : cases) {
        var c = entry.getAsJsonObject();
        if (c.has("late") && !Boolean.getBoolean("malisis.compat.verify.late")) continue;
        loadedCases.add(c);
        var pos = new BlockPos(c.get("x").getAsInt(), c.get("y").getAsInt(), c.get("z").getAsInt());
        level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        check(
            BuiltInRegistries.BLOCK
                .getKey(level.getBlockState(pos).getBlock())
                .toString()
                .equals(c.get("block").getAsString()),
            "block " + pos + " " + level.getBlockState(pos));
        check(level.getBlockEntity(pos) != null, "entity " + pos);
        var state = level.getBlockState(pos);
        if (c.has("properties"))
          for (var prop : c.getAsJsonObject("properties").entrySet()) {
            String name =
                switch (prop.getKey()) {
                  case "direction" -> "facing";
                  case "power" -> "powered";
                  case "wall" -> "in_wall";
                  default -> prop.getKey();
                };
            var property = state.getBlock().getStateDefinition().getProperty(name);
            if (property != null)
              check(
                  state
                      .getValue(property)
                      .toString()
                      .equalsIgnoreCase(prop.getValue().getAsString()),
                  "property "
                      + pos
                      + " "
                      + name
                      + " expected "
                      + prop.getValue()
                      + " got "
                      + state);
          }
        if (state.getBlock() instanceof DoorBlock)
          check(level.getBlockState(pos.above()).is(state.getBlock()), "upper half " + pos);
        if (level.getBlockEntity(pos) instanceof MechanismEntity mechanism)
          for (var part : mechanism.block().footprint(pos, state)) {
            level.setChunkForced(part.getX() >> 4, part.getZ() >> 4, true);
            level.getChunk(part.getX() >> 4, part.getZ() >> 4);
          }
        if (level.getBlockEntity(pos) instanceof DoorBlockEntity door) {
          check(!door.collisionBoxes().isEmpty(), "collision " + pos);
          if (c.get("item").getAsString().equals("malisisdoors:customdooritem"))
            configuration(door.configuration);
        }
        if (level.getBlockEntity(pos) instanceof FactoryEntity factory) {
          check(
              factory.getItem(0).is(Items.DARK_OAK_PLANKS) && factory.getItem(0).getCount() == 11,
              "factory frame inventory");
          check(
              factory.getItem(1).is(Items.RED_STAINED_GLASS) && factory.getItem(1).getCount() == 7,
              "factory glass inventory");
        }
      }
      // Load non-origin chunk first; verify all fifteen members share a controller and identity.
      level.getChunk(2, 5);
      level.getChunk(1, 5);
      FieldEntity controller = (FieldEntity) level.getBlockEntity(new BlockPos(30, 64, 80));
      check(controller != null, "field controller");
      for (var pos : BlockPos.betweenClosed(30, 64, 80, 34, 66, 80)) {
        check(level.getBlockEntity(pos) instanceof FieldEntity, "field member " + pos);
        var field = (FieldEntity) level.getBlockEntity(pos);
        check(
            field.root.equals(new BlockPos(30, 64, 80))
                && field.max.equals(new BlockPos(34, 66, 80)),
            "field bounds " + pos);
        check(field.identity.equals(controller.identity), "field identity");
      }
      // Exercise the actual player/container ItemStack parsing path on original legacy NBT.
      var old =
          NbtIo.readCompressed(
              Path.of(System.getProperty("malisis.compat.verify.player")),
              NbtAccounter.unlimitedHeap());
      boolean custom = false;
      for (var value : old.getList("Inventory", Tag.TAG_COMPOUND)) {
        var item = (CompoundTag) value;
        if (item.getString("id").equals("malisisdoors:customdooritem")) {
          var stack =
              ItemStack.parse(
                      level.registryAccess(),
                      (CompoundTag)
                          net.minecraft.util.datafix.DataFixers.getDataFixer()
                              .update(
                                  net.minecraft.util.datafix.fixes.References.ITEM_STACK,
                                  new com.mojang.serialization.Dynamic<>(NbtOps.INSTANCE, item),
                                  1343,
                                  net.minecraft.SharedConstants.getCurrentVersion()
                                      .getDataVersion()
                                      .getVersion())
                              .getValue())
                  .orElseThrow();
          check(stack.getCount() == 3, "custom item count");
          configuration(
              DoorConfiguration.readItem(
                  stack,
                  level.registryAccess(),
                  DoorConfiguration.defaults(FactoryContent.CUSTOM_SPEC)));
          custom = true;
        }
      }
      check(custom, "legacy player custom item exists");
      LegacyDataChecks.run(server);
      Files.createDirectories(out);
      pending = out;
      Files.writeString(
          out.resolve("migration.txt"),
          "PASS "
              + checks
              + " authentic legacy save checks; "
              + cases.size()
              + " placed item families, custom materials/settings, Factory inventory, cross-chunk"
              + " field, player item parsing\n");
    } catch (Throwable error) {
      error.printStackTrace();
      try {
        Files.createDirectories(out);
        Files.writeString(out.resolve("FAILED"), error.toString());
      } catch (Exception ignored) {
      }
    } finally {
      if (pending == null)
        try {
          Files.writeString(out.resolve("STOP-SERVER"), "failed");
        } catch (Exception ignored) {
        }
    }
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (pending == null || ++ticks < 60) return;
    try {
      var level = event.getServer().overworld();
      for (var entry : loadedCases) {
        var c = entry.getAsJsonObject();
        var pos = new BlockPos(c.get("x").getAsInt(), c.get("y").getAsInt(), c.get("z").getAsInt());
        if (level.getBlockEntity(pos) instanceof MechanismEntity mechanism)
          for (var part : mechanism.block().footprint(pos, mechanism.getBlockState())) {
            if (part.equals(pos)) continue;
            check(level.getBlockEntity(part) instanceof PartEntity, "restored footprint " + part);
            check(
                ((PartEntity) level.getBlockEntity(part)).root.equals(pos),
                "footprint root " + part);
          }
      }
      Files.writeString(
          pending.resolve("structure.txt"),
          "PASS "
              + checks
              + " total checks including restored mechanism footprints after 60 ticks\n");
    } catch (Throwable error) {
      error.printStackTrace();
      try {
        Files.writeString(pending.resolve("FAILED"), error.toString());
      } catch (Exception ignored) {
      }
    } finally {
      try {
        if (!Boolean.getBoolean("malisis.compat.verify.hold"))
          Files.writeString(pending.resolve("STOP-SERVER"), "done");
      } catch (Exception ignored) {
      }
      pending = null;
    }
  }
}
