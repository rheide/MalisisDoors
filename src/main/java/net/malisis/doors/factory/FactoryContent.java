package net.malisis.doors.factory;

import net.malisis.doors.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

public final class FactoryContent {
  public static final DoorSpec CUSTOM_SPEC =
      new DoorSpec("customdoor", "sliding_door_wood", DoorSpec.Movement.SLIDE, 6, false, true);
  public static final DeferredBlock<AnimatedDoorBlock> CUSTOM_DOOR =
      MalisisDoors.BLOCKS.register(
          "customdoor",
          () ->
              new AnimatedDoorBlock(
                  CUSTOM_SPEC, Block.Properties.ofFullCopy(Blocks.OAK_DOOR).noOcclusion()));
  public static final DeferredItem<BlockItem> CUSTOM_ITEM =
      MalisisDoors.ITEMS.registerSimpleBlockItem("customdooritem", CUSTOM_DOOR);
  public static final DeferredBlock<FactoryBlock> FACTORY =
      MalisisDoors.BLOCKS.register(
          "door_factory",
          () -> new FactoryBlock(Block.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3)));
  public static final DeferredItem<BlockItem> FACTORY_ITEM =
      MalisisDoors.ITEMS.registerSimpleBlockItem(FACTORY);
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryEntity>> ENTITY =
      MalisisDoors.TYPES.register(
          "door_factory",
          () -> BlockEntityType.Builder.of(FactoryEntity::new, FACTORY.get()).build(null));
  public static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, "malisisdoors");
  public static final DeferredHolder<MenuType<?>, MenuType<FactoryMenu>> MENU =
      MENUS.register("door_factory", () -> IMenuTypeExtension.create(FactoryMenu::new));
  public static final DeferredHolder<MenuType<?>, MenuType<CodeMenu>> CODE_MENU =
      MENUS.register("door_code", () -> IMenuTypeExtension.create(CodeMenu::new));

  public static void init() {}
}
