package net.malisis.doors.field;

import net.malisis.doors.MalisisDoors;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;

public final class FieldContent {
  public static final DeferredBlock<FieldBlock> FIELD =
      MalisisDoors.BLOCKS.register(
          "forcefielddoor",
          () ->
              new FieldBlock(
                  Block.Properties.of().strength(-1, 60000000).noOcclusion().noLootTable()));
  public static final DeferredItem<ProjectorItem> PROJECTOR =
      MalisisDoors.ITEMS.register(
          "forcefielditem", () -> new ProjectorItem(new Item.Properties().stacksTo(1)));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FieldEntity>> ENTITY =
      MalisisDoors.TYPES.register(
          "forcefield",
          () -> BlockEntityType.Builder.of(FieldEntity::new, FIELD.get()).build(null));

  public static void init() {}
}
