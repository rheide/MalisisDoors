package net.malisis.doors.flat;

import java.util.*;
import net.malisis.doors.MalisisDoors;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.registries.*;

public final class FlatBlocks {
  public static final List<DeferredBlock<? extends Block>> BLOCKS = new ArrayList<>();
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlatEntity>> ENTITY =
      MalisisDoors.TYPES.register(
          "flat_door",
          () ->
              BlockEntityType.Builder.of(
                      FlatEntity::new,
                      java.util.stream.Stream.concat(
                              BLOCKS.stream().map(DeferredBlock::get),
                              net.malisis.doors.vanilla.VanillaAnimations.flats())
                          .toArray(Block[]::new))
                  .build(null));

  public static void init() {
    for (String wood : List.of("acacia", "birch", "dark_oak", "jungle", "spruce")) {
      String id = "trapdoor_" + wood;
      add(
          id,
          MalisisDoors.BLOCKS.register(
              id,
              () ->
                  new AnimatedTrapdoor(
                      id, false, Block.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).noOcclusion())));
    }
    add(
        "sliding_trapdoor",
        MalisisDoors.BLOCKS.register(
            "sliding_trapdoor",
            () ->
                new AnimatedTrapdoor(
                    "sliding_trapdoor",
                    true,
                    Block.Properties.ofFullCopy(Blocks.IRON_TRAPDOOR).noOcclusion())));
    add(
        "camofencegate",
        MalisisDoors.BLOCKS.register(
            "camofencegate",
            () ->
                new CamouflageGate(
                    Block.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).noOcclusion())));
    add(
        "rustyladder",
        MalisisDoors.BLOCKS.register(
            "rustyladder",
            () -> new RustyLadder(Block.Properties.ofFullCopy(Blocks.LADDER).noOcclusion())));
  }

  private static void add(String id, DeferredBlock<? extends Block> block) {
    BLOCKS.add(block);
    MalisisDoors.ITEMS.registerSimpleBlockItem(id, block);
  }
}
