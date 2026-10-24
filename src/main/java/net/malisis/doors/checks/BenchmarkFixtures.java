package net.malisis.doors.checks;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.malisis.doors.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "malisisdoors")
public final class BenchmarkFixtures {
  public static boolean active;
  public static final String[] FAMILIES = {
    "jail_door", "wood_sliding_door", "shoji_door", "garage_door", "big_door_iron_3x3", "customdoor"
  };

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent event) {
    if (System.getProperty("malisis.verify.out") == null) return;
    event
        .getDispatcher()
        .register(
            Commands.literal("malisisbench")
                .then(
                    Commands.argument("family", StringArgumentType.word())
                        .executes(
                            context -> {
                              String family = StringArgumentType.getString(context, "family");
                              if (!java.util.Arrays.asList(FAMILIES).contains(family)) return 0;
                              active = true;
                              var level = context.getSource().getLevel();
                              var block =
                                  BuiltInRegistries.BLOCK.get(
                                      ResourceLocation.fromNamespaceAndPath(
                                          "malisisdoors", family));
                              for (int x = -4; x < 36; x++)
                                for (int z = -4; z < 36; z++)
                                  for (int y = 63; y < 71; y++)
                                    level.setBlock(
                                        new BlockPos(x, y, z),
                                        y == 63
                                            ? Blocks.STONE.defaultBlockState()
                                            : Blocks.AIR.defaultBlockState(),
                                        2);
                              for (int row = 0; row < 8; row++)
                                for (int col = 0; col < 8; col++) {
                                  var pos = new BlockPos(col * 4, 64, row * 4);
                                  var state = block.defaultBlockState();
                                  if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                                    state =
                                        state.setValue(
                                            BlockStateProperties.HORIZONTAL_FACING,
                                            Direction.SOUTH);
                                  level.setBlock(pos, state, 2);
                                  if (block instanceof DoorBlock)
                                    level.setBlock(
                                        pos.above(),
                                        state.setValue(
                                            DoorBlock.HALF,
                                            net.minecraft.world.level.block.state.properties
                                                .DoubleBlockHalf.UPPER),
                                        2);
                                  if (family.equals("garage_door"))
                                    for (int h = 1; h < 3; h++)
                                      level.setBlock(pos.above(h), state, 2);
                                  if (family.equals("customdoor")
                                      && level.getBlockEntity(pos)
                                          instanceof DoorBlockEntity door) {
                                    door.configuration =
                                        new net.malisis.doors.custom.DoorConfiguration(
                                            "rotating_door",
                                            "vanilla_door",
                                            6,
                                            0,
                                            false,
                                            false,
                                            net.malisis.doors.custom.DoorConfiguration.Redstone
                                                .STANDARD,
                                            "",
                                            Blocks.OAK_PLANKS.defaultBlockState(),
                                            Blocks.GLASS.defaultBlockState(),
                                            Blocks.GLASS.defaultBlockState());
                                    door.sync();
                                  }
                                }
                              var player = context.getSource().getPlayerOrException();
                              player.setGameMode(GameType.SPECTATOR);
                              player.connection.teleport(15, 79, -13, 0, 23);
                              return 1;
                            })));
  }
}
