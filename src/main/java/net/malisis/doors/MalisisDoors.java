package net.malisis.doors;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.*;
import static net.malisis.doors.DoorSpec.Movement.*;
@Mod("malisisdoors")
public final class MalisisDoors {
 public static final String ID="malisisdoors";
 public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
 public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
 public static final DeferredRegister<BlockEntityType<?>> TYPES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ID);
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
 public static final List<DoorSpec> SPECS=new ArrayList<>();
 public static final List<DeferredBlock<AnimatedDoorBlock>> DOORS=new ArrayList<>();
 static {
  add(new DoorSpec("wood_sliding_door","sliding_door_wood",SLIDE,6,false,true));
  add(new DoorSpec("iron_sliding_door","sliding_door_iron",SLIDE,6,true,true));
  add(new DoorSpec("jail_door","jail_door",SLIDE,12,true,true));
  add(new DoorSpec("shoji_door","shoji_door",SLIDE,20,false,true));
  add(new DoorSpec("laboratory_door","laboratory_door",UP,12,false,false));
  add(new DoorSpec("factory_door","factory_door",SPLIT,12,false,true));
  for(DyeColor color:DyeColor.values())add(new DoorSpec("curtain_"+(color==DyeColor.LIGHT_GRAY?"silver":color.getName()),"curtains/curtain_"+(color==DyeColor.LIGHT_GRAY?"silver":color.getName()),CURTAIN,6,false,true));
 }
 private static void add(DoorSpec spec){SPECS.add(spec);DeferredBlock<AnimatedDoorBlock> block=BLOCKS.register(spec.id(),()->new AnimatedDoorBlock(spec,BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).strength(spec.redstoneOnly()?5:2).noOcclusion()));DOORS.add(block);ITEMS.registerSimpleBlockItem(spec.id(),block);}
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<DoorBlockEntity>> DOOR_ENTITY=TYPES.register("door",()->BlockEntityType.Builder.of(DoorBlockEntity::new,DOORS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
 static {TABS.register("doors",()->CreativeModeTab.builder().title(Component.literal("Malisis Doors · experimental 1.21.1")).icon(()->new ItemStack(DOORS.getFirst().get())).displayItems((parameters,output)->DOORS.forEach(b->output.accept(b.get()))).build());}
 public MalisisDoors(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);TYPES.register(bus);TABS.register(bus);}
}
