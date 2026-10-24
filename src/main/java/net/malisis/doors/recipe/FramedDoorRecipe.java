package net.malisis.doors.recipe;

import net.malisis.doors.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.*;

/** One oak/spruce door and five identical frame blocks, including stacked 2x2 inputs. */
public final class FramedDoorRecipe extends CustomRecipe {
  public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
      DeferredRegister.create(Registries.RECIPE_SERIALIZER, MalisisDoors.ID);
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FramedDoorRecipe>> TYPE =
      SERIALIZERS.register(
          "big_door", () -> new SimpleCraftingRecipeSerializer<>(FramedDoorRecipe::new));

  public FramedDoorRecipe(CraftingBookCategory category) {
    super(category);
  }

  private ItemStack frame(CraftingInput input) {
    boolean door = false;
    int count = 0;
    ItemStack frame = ItemStack.EMPTY;
    for (int i = 0; i < input.size(); i++) {
      var stack = input.getItem(i);
      if (stack.isEmpty()) continue;
      if (stack.is(Items.OAK_DOOR) || stack.is(Items.SPRUCE_DOOR)) {
        if (door) return ItemStack.EMPTY;
        door = true;
      } else {
        if (!(stack.getItem() instanceof BlockItem)
            || !frame.isEmpty() && !ItemStack.isSameItemSameComponents(frame, stack))
          return ItemStack.EMPTY;
        frame = stack;
        count += stack.getCount();
      }
    }
    return door && count >= 5 ? frame : ItemStack.EMPTY;
  }

  @Override
  public boolean matches(CraftingInput input, Level level) {
    return !frame(input).isEmpty();
  }

  @Override
  public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
    var frame = frame(input);
    if (frame.isEmpty()) return ItemStack.EMPTY;
    boolean spruce = false;
    for (int i = 0; i < input.size(); i++)
      if (input.getItem(i).is(Items.SPRUCE_DOOR)) spruce = true;
    String id = spruce ? "medieval_door" : "carriage_door";
    var output =
        new ItemStack(
            BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    MalisisDoors.ID, id)));
    var tag = new CompoundTag();
    tag.put(
        "frame",
        NbtUtils.writeBlockState(((BlockItem) frame.getItem()).getBlock().defaultBlockState()));
    output.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    return output;
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return width * height >= 2;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TYPE.get();
  }

  @Override
  public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
    var remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
    int needed = 5;
    // CraftingInput shares native container stacks. ResultSlot consumes one per
    // non-empty slot after this callback; consume only the additional frame units.
    for (int i = 0; i < input.size(); i++) {
      var stack = input.getItem(i);
      if (stack.isEmpty() || stack.is(Items.OAK_DOOR) || stack.is(Items.SPRUCE_DOOR)) continue;
      int take = Math.min(needed, stack.getCount());
      needed -= take;
      if (take == 0) remaining.set(i, stack.copyWithCount(1));
      else if (take > 1) stack.shrink(take - 1);
    }
    return remaining;
  }
}
