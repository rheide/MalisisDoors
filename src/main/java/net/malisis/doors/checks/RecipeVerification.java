package net.malisis.doors.checks;

import static net.malisis.doors.checks.ServerVerification.check;

import net.malisis.doors.recipe.FramedDoorRecipe;
import net.malisis.doors.sound.DoorSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class RecipeVerification {
  public static void run(ServerLevel level) {
    var tab =
        net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("malisisdoors", "doors"));
    tab.buildContents(
        new CreativeModeTab.ItemDisplayParameters(
            level.enabledFeatures(), true, level.registryAccess()));
    check(tab.getDisplayItems().size() == 49, "all 49 items present in actual creative tab");
    var player = FakePlayerFactory.getMinecraft(level);
    var menu = player.inventoryMenu;
    var grid = menu.getCraftSlots();
    for (var door : java.util.List.of(Items.OAK_DOOR, Items.SPRUCE_DOOR))
      for (int count : new int[] {5, 12}) {
        grid.clearContent();
        grid.setItem(0, new ItemStack(door, 2));
        grid.setItem(1, new ItemStack(Items.COBBLESTONE, count));
        var found =
            level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), level);
        check(
            found.isPresent() && found.get().value() instanceof FramedDoorRecipe,
            "stacked framed recipe matches");
        var output = found.get().value().assemble(grid.asCraftInput(), level.registryAccess());
        check(
            !output.isEmpty()
                && output
                    .get(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
                    .copyTag()
                    .contains("frame"),
            "frame material stored in crafted item");
        menu.getSlot(0).onTake(player, output);
        check(grid.getItem(0).getCount() == 1, "framed native crafting consumes one door");
        check(
            grid.getItem(1).getCount() == count - 5, "framed native crafting consumes five blocks");
      }
    grid.clearContent();
    grid.setItem(0, new ItemStack(Items.OAK_DOOR));
    grid.setItem(1, new ItemStack(Items.STONE, 3));
    grid.setItem(2, new ItemStack(Items.STONE, 4));
    grid.setItem(3, new ItemStack(Items.STONE, 7));
    var recipe =
        level
            .getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), level)
            .orElseThrow();
    menu.getSlot(0)
        .onTake(player, recipe.value().assemble(grid.asCraftInput(), level.registryAccess()));
    int left = 0;
    for (int i = 1; i < 4; i++) left += grid.getItem(i).getCount();
    check(left == 9, "framed split-stack consumption preserves unused slots");
    grid.clearContent();
    for (String family : net.malisis.doors.custom.DoorConfiguration.SOUNDS) {
      check(
          family.equals("silent_door") || DoorSounds.sound(family, true, false) != null,
          "sound family opening event " + family);
      check(DoorSounds.sound(family, true, true) == null, "no spurious opened impact " + family);
    }
  }
}
