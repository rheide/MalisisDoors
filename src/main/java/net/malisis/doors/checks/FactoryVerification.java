package net.malisis.doors.checks;

import static net.malisis.doors.checks.ServerVerification.check;

import java.util.*;
import net.malisis.doors.*;
import net.malisis.doors.custom.*;
import net.malisis.doors.factory.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "malisisdoors")
public final class FactoryVerification {
  public static final BlockPos FACTORY = new BlockPos(192, 64, 0), CODED = new BlockPos(192, 64, 8);
  public static final List<BlockPos> CUSTOM = new ArrayList<>();
  private static DoorBlockEntity auto, proximity;
  private static ServerPlayer actor;
  private static long started;
  private static int phase;
  private static boolean ready;

  static DoorConfiguration settings(
      String motion, int close, boolean near, String code, DoorConfiguration.Redstone policy) {
    return new DoorConfiguration(
        motion,
        "vanilla_door",
        20,
        close,
        false,
        near,
        policy,
        code,
        Blocks.OAK_PLANKS.defaultBlockState(),
        Blocks.GLASS.defaultBlockState(),
        Blocks.STONE.defaultBlockState());
  }

  static DoorBlockEntity place(ServerLevel level, BlockPos pos, ItemStack item) {
    var player = FakePlayerFactory.getMinecraft(level);
    level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
    level.removeBlock(pos, false);
    level.removeBlock(pos.above(), false);
    player.setPos(pos.getX() + .5, 64, pos.getZ() - .5);
    player.setYRot(Direction.SOUTH.toYRot());
    player.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
    var result =
        player
            .getMainHandItem()
            .useOn(
                new UseOnContext(
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(
                        new Vec3(pos.getX() + .25, pos.getY(), pos.getZ() + .5),
                        Direction.UP,
                        pos.below(),
                        false)));
    check(result.consumesAction(), "factory result can be placed");
    check(level.getBlockEntity(pos) instanceof DoorBlockEntity, "factory result owns door entity");
    return (DoorBlockEntity) level.getBlockEntity(pos);
  }

  static ItemStack custom(ServerLevel level, DoorConfiguration config) {
    var item = new ItemStack(FactoryContent.CUSTOM_ITEM.get());
    DoorConfiguration.writeItem(item, config);
    return item;
  }

  public static void run(ServerLevel level) {
    for (int x = 188; x < 218; x++)
      for (int z = -4; z < 42; z++)
        level.setBlock(new BlockPos(x, 63, z), Blocks.STONE.defaultBlockState(), 2);
    level.setBlockAndUpdate(FACTORY.below(), Blocks.STONE.defaultBlockState());
    level.setBlockAndUpdate(FACTORY, FactoryContent.FACTORY.get().defaultBlockState());
    var factory = (FactoryEntity) level.getBlockEntity(FACTORY);
    var player = FakePlayerFactory.getMinecraft(level);
    player.setPos(192.5, 64, -.5);
    player.containerMenu = factory.createMenu(70, player.getInventory(), player);
    check(
        player.containerMenu instanceof FactoryMenu, "factory menu provider supplies native menu");
    var menu = (FactoryMenu) player.containerMenu;
    check(
        !menu.getSlot(0).mayPlace(new ItemStack(Items.WATER_BUCKET))
            && menu.getSlot(1).mayPlace(new ItemStack(Items.WATER_BUCKET)),
        "factory material slot validation");
    var c = settings("rotating_door", 0, false, "", DoorConfiguration.Redstone.HAND_ONLY);
    factory.setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
    factory.setItem(1, new ItemStack(Items.GLASS, 2));
    factory.setItem(2, new ItemStack(Items.STONE, 2));
    check(
        FactoryNetwork.configure(
            player, new FactoryNetwork.Configure(menu.containerId, FACTORY, c.save(), true, true)),
        "factory create via authoritative handler");
    check(
        factory.getItem(4).is(FactoryContent.CUSTOM_ITEM.get())
            && factory.getItem(4).getCount() == 1,
        "factory emits custom item");
    for (int i = 0; i < 3; i++)
      check(factory.getItem(i).getCount() == 1, "factory consumes one input each");
    int output = factory.getItem(4).getCount();
    check(
        !FactoryNetwork.configure(
            player,
            new FactoryNetwork.Configure(menu.containerId + 1, FACTORY, c.save(), true, true)),
        "factory rejects stale menu id");
    check(factory.getItem(4).getCount() == output, "rejected action leaves output alone");
    check(factory.createDoor() && factory.getItem(4).getCount() == 2, "equivalent output stacks");
    check(
        !factory.createDoor() && factory.getItem(4).getCount() == 2,
        "empty inputs cannot duplicate output");
    factory.clearContent();
    // Editing consumes the input stack and preserves its item identity while changing settings.
    factory.setItem(3, new ItemStack(MalisisDoors.DOORS.getFirst().get(), 3));
    var edit =
        settings("sliding_up_door", 40, true, "123456", DoorConfiguration.Redstone.REDSTONE_LOCK);
    check(
        FactoryNetwork.configure(
            player,
            new FactoryNetwork.Configure(menu.containerId, FACTORY, edit.save(), false, true)),
        "factory edit path");
    check(
        factory.getItem(3).isEmpty() && factory.getItem(4).getCount() == 3,
        "edit transfers the entire stack once");
    check(
        DoorConfiguration.readItem(factory.getItem(4), level.registryAccess(), c)
            .movement()
            .equals("sliding_up_door"),
        "edited item carries settings");
    factory.clearContent();
    // Real inventory shift-click uses the menu's validated slot transfer path.
    player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 3));
    check(
        !menu.quickMoveStack(player, 32).isEmpty() && factory.getItem(0).getCount() == 3,
        "shift-click transfers frame input");
    factory.clearContent();
    int index = 0;
    for (String movement : DoorMotion.FACTORY) {
      factory.setItem(0, new ItemStack(Items.OAK_PLANKS));
      factory.setItem(1, new ItemStack(Items.GLASS));
      factory.setItem(2, new ItemStack(Items.STONE));
      factory.configuration =
          settings(movement, 0, false, "", DoorConfiguration.Redstone.HAND_ONLY);
      factory.createMode = true;
      check(factory.createDoor(), "create movement " + movement);
      ItemStack result = factory.removeItem(4, 1);
      var saved = DoorConfiguration.readItem(result, level.registryAccess(), c);
      check(
          saved.movement().equals(movement)
              && saved.frame().is(Blocks.OAK_PLANKS)
              && saved.top().is(Blocks.GLASS)
              && saved.bottom().is(Blocks.STONE),
          "factory preserves movement and materials " + movement);
      BlockPos pos = new BlockPos(192 + (index % 6) * 4, 64, 16 + (index / 6) * 4);
      index++;
      var door = place(level, pos, result);
      CUSTOM.add(pos);
      check(door.configuration.equals(saved), "placed configuration round-trip " + movement);
      level
          .getBlockState(pos)
          .useWithoutItem(
              level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
      check(
          level.getBlockState(pos).getValue(DoorBlock.OPEN),
          "configured hand interaction " + movement);
      door.animation.reset(true, level.getGameTime());
      door.invalidate();
      for (var bounds : door.selectionBoxes())
        check(
            Double.isFinite(bounds.minX) && Double.isFinite(bounds.maxY),
            "finite movement geometry " + movement);
      level
          .getBlockState(pos)
          .useWithoutItem(
              level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
      check(!level.getBlockState(pos).getValue(DoorBlock.OPEN), "configured closes " + movement);
      var data = door.saveWithoutMetadata(level.registryAccess());
      var clone = new DoorBlockEntity(pos, level.getBlockState(pos));
      clone.loadWithComponents(data, level.registryAccess());
      check(clone.configuration.equals(door.configuration), "door persistence " + movement);
      check(
          ItemStack.isSameItemSameComponents(door.item(), result),
          "configured pick/drop item metadata " + movement);
    }
    for (Item special :
        List.of(Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.FLINT_AND_STEEL, Items.ENDER_PEARL)) {
      factory.setItem(0, new ItemStack(Items.OAK_PLANKS));
      factory.setItem(1, new ItemStack(special));
      factory.setItem(2, new ItemStack(Items.GLASS));
      factory.configuration = c;
      factory.createMode = true;
      check(factory.createDoor(), "special panel accepted " + special);
      var item = factory.removeItem(4, 1);
      check(
          DoorConfiguration.readItem(item, level.registryAccess(), c).top()
              == DoorConfiguration.materialState(new ItemStack(special)),
          "special panel state preserved");
    }
    factory.configuration = c;
    factory.createMode = true;
    factory.setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
    var savedFactory = factory.saveWithoutMetadata(level.registryAccess());
    var copy = new FactoryEntity(FACTORY, level.getBlockState(FACTORY));
    copy.loadWithComponents(savedFactory, level.registryAccess());
    check(
        copy.configuration.equals(c) && copy.getItem(0).getCount() == 5,
        "factory settings and inventory persistence");
    var coded =
        place(
            level,
            CODED,
            custom(
                level,
                settings(
                    "rotating_door", 0, false, "123456", DoorConfiguration.Redstone.STANDARD)));
    player.setPos(CODED.getX() + .5, 64, CODED.getZ() - .5);
    level
        .getBlockState(CODED)
        .useWithoutItem(
            level,
            player,
            new BlockHitResult(Vec3.atCenterOf(CODED), Direction.NORTH, CODED, false));
    check(
        !coded.getBlockState().getValue(DoorBlock.OPEN),
        "coded hand action stays locked before verification");
    player.containerMenu = new CodeMenu(71, player.getInventory(), CODED);
    int codeWindow = player.containerMenu.containerId;
    check(
        !FactoryNetwork.code(player, new FactoryNetwork.SubmitCode(codeWindow, CODED, "000000"))
            && !coded.getBlockState().getValue(DoorBlock.OPEN),
        "incorrect code stays closed");
    check(
        FactoryNetwork.code(player, new FactoryNetwork.SubmitCode(codeWindow, CODED, "123456"))
            && coded.getBlockState().getValue(DoorBlock.OPEN),
        "correct code opens door");
    DoorInteractions.setOpen(coded.block(), level, coded.getBlockState(), CODED, null, false);
    auto =
        place(
            level,
            new BlockPos(192, 64, 36),
            custom(
                level,
                settings("rotating_door", 6, false, "", DoorConfiguration.Redstone.HAND_ONLY)));
    DoorInteractions.setOpen(
        auto.block(), level, auto.getBlockState(), auto.getBlockPos(), null, true);
    proximity =
        place(
            level,
            new BlockPos(200, 64, 36),
            custom(
                level,
                settings("rotating_door", 8, true, "", DoorConfiguration.Redstone.STANDARD)));
    actor =
        FakePlayerFactory.get(
            level,
            new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("71a91734-6c4a-40a6-90db-c710bc81705c"),
                "DoorProximityTest"));
    actor.setPos(200.5, 64, 35.5);
    level.addNewPlayer(actor);
    started = level.getGameTime();
    ready = true;
    // Leave the displayed factory prepared for a real client screen/network create operation.
    factory.clearContent();
    factory.configuration = c;
    factory.createMode = true;
    factory.setItem(0, new ItemStack(Items.OAK_PLANKS, 3));
    factory.setItem(1, new ItemStack(Items.GLASS, 3));
    factory.setItem(2, new ItemStack(Items.STONE, 3));
    factory.sync();
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post event) {
    if (!ready || System.getProperty("malisis.verify.out") == null) return;
    long elapsed = event.getServer().overworld().getGameTime() - started;
    try {
      if (phase == 0 && elapsed >= 4) {
        check(proximity.getBlockState().getValue(DoorBlock.OPEN), "proximity opens for player");
        actor.setPos(240, 64, 36);
        phase = 1;
      }
      if (phase == 1 && elapsed >= 18) {
        check(!auto.getBlockState().getValue(DoorBlock.OPEN), "auto-close without proximity");
        check(
            !proximity.getBlockState().getValue(DoorBlock.OPEN),
            "proximity door closes after player leaves");
        actor.discard();
        java.nio.file.Files.writeString(
            java.nio.file.Path.of(System.getProperty("malisis.verify.out"), "factory-async.txt"),
            "PASS automatic close and player proximity lifecycle\n");
        ready = false;
      }
    } catch (Throwable error) {
      error.printStackTrace();
      ready = false;
      try {
        java.nio.file.Files.writeString(
            java.nio.file.Path.of(
                System.getProperty("malisis.verify.out"), "FACTORY-ASYNC-FAILED.txt"),
            error.toString());
      } catch (Exception ignored) {
      }
    }
  }
}
