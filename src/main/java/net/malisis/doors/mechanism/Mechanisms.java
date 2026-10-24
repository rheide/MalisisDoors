package net.malisis.doors.mechanism;
import java.util.*;
import net.malisis.doors.MalisisDoors;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.neoforged.neoforge.registries.*;
import static net.malisis.doors.mechanism.MechanismSpec.Kind.*;
public final class Mechanisms {
 public static final List<DeferredBlock<MechanismBlock>> BLOCKS=new ArrayList<>();
 public static final DeferredBlock<PartBlock> PART=MalisisDoors.BLOCKS.register("door_part",()->new PartBlock(Block.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().noLootTable()));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<PartEntity>> PART_ENTITY=MalisisDoors.TYPES.register("door_part",()->BlockEntityType.Builder.of(PartEntity::new,PART.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MechanismEntity>> ENTITY=MalisisDoors.TYPES.register("mechanism",()->BlockEntityType.Builder.of(MechanismEntity::new,BLOCKS.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
 public static void init(){
  add(new MechanismSpec("garage_door",GARAGE,1,1,1,0,"garage_door",null,10));
  for(String wood:List.of("oak","spruce","birch","jungle","acacia","dark_oak","iron","rusty"))add(new MechanismSpec("big_door_"+wood+"_3x3",LARGE,3,3,1,-1,"big_door_"+wood+"_3x3",null,20));
  add(new MechanismSpec("carriage_door",CARRIAGE,4,5,1,0,"carriage_door","big_door",20));
  add(new MechanismSpec("medieval_door",MEDIEVAL,4,5,1,0,"medieval_door","big_door",20));
  add(new MechanismSpec("hitechdoor",HITECH,3,2,1,-1,"hitechdoor","hitechdoor",21));
  add(new MechanismSpec("rustyhatch",RUSTY_HATCH,2,3,2,0,"rusty_hatch","rusty_hatch",80));
 }
 private static void add(MechanismSpec spec){var block=MalisisDoors.BLOCKS.register(spec.id(),()->new MechanismBlock(spec,Block.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4).noOcclusion()));BLOCKS.add(block);MalisisDoors.ITEMS.registerSimpleBlockItem(spec.id(),block);}
}
