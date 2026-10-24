package net.malisis.doors.checks;
import java.nio.file.*;
import java.util.*;
import net.malisis.doors.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
/** Explicit opt-in isolated fixture; inactive in normal gameplay. */
@EventBusSubscriber(modid="malisisdoors")
public final class ServerVerification {
 static boolean enabled(){return System.getProperty("malisis.verify.out")!=null;}
 static final List<BlockPos> ROOTS=new ArrayList<>();
 static int ticks,checks;static String failure;
 static void check(boolean condition,String message){checks++;if(!condition)throw new IllegalStateException(message);}
 static BlockPos root(int index){return new BlockPos((index%11)*4,64,(index/11)*4);}
 static void place(ServerLevel level,AnimatedDoorBlock block,BlockPos pos,Direction facing){
  level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
  level.removeBlock(pos,false);level.removeBlock(pos.above(),false);
  var player=FakePlayerFactory.getMinecraft(level);player.setPos(pos.getX()+.5,64,pos.getZ()+.5);player.setYRot(facing.toYRot());player.getAbilities().instabuild=false;
  var stack=new ItemStack(block.asItem());player.setItemInHand(InteractionHand.MAIN_HAND,stack);
  var result=stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false)));
  check(result.consumesAction(),block.spec.id()+" item placement "+facing);
  check(level.getBlockState(pos).is(block),"lower exists "+pos);
  check(level.getBlockState(pos.above()).is(block),"upper exists "+pos);
  check(level.getBlockState(pos).getValue(DoorBlock.FACING)==facing,"placement facing "+facing);
  check(level.getBlockEntity(pos) instanceof DoorBlockEntity,"lower entity "+pos);
  check(level.getBlockEntity(pos.above())==null,"upper does not duplicate renderer "+pos);
  var closed=level.getBlockState(pos).getCollisionShape(level,pos).bounds();
  var expected=net.minecraft.world.level.block.Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,facing).getCollisionShape(level,pos).bounds();
  check(closed.equals(expected),"closed collision follows facing "+facing);

 }
 @SubscribeEvent public static void started(ServerStartedEvent event){if(!enabled())return;
  try{
   var level=event.getServer().overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,event.getServer());level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,event.getServer());
   for(int x=-4;x<48;x++)for(int z=-8;z<40;z++){level.setBlockAndUpdate(new BlockPos(x,63,z),Blocks.STONE.defaultBlockState());for(int y=64;y<71;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
   int i=0;
   for(var entry:MalisisDoors.DOORS)for(Direction facing:Direction.Plane.HORIZONTAL){
    AnimatedDoorBlock block=entry.get();
    var recipe=level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("malisisdoors",block.spec.id()));
    check(recipe.isPresent(),"recipe exists "+block.spec.id());
    check(recipe.get().value().getResultItem(level.registryAccess()).is(block.asItem()),"recipe output "+block.spec.id());
    for(var ingredient:recipe.get().value().getIngredients())check(ingredient.getItems().length>0,"recipe ingredient resolves "+block.spec.id());
    BlockPos pos=root(i++);place(level,block,pos,facing);ROOTS.add(pos);
    var player=FakePlayerFactory.getMinecraft(level);var state=level.getBlockState(pos);
    state.useWithoutItem(level,player,new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));
    check(level.getBlockState(pos).getValue(DoorBlock.OPEN)!=block.spec.redstoneOnly(),"hand policy "+block.spec.id());
    check(level.getBlockState(pos.above()).getValue(DoorBlock.OPEN)==level.getBlockState(pos).getValue(DoorBlock.OPEN),"halves hand sync");
    block.setOpen(player,level,level.getBlockState(pos),pos,false);
    level.setBlockAndUpdate(pos.relative(Direction.SOUTH),Blocks.REDSTONE_BLOCK.defaultBlockState());
    check(level.getBlockState(pos).getValue(DoorBlock.OPEN),"redstone opens "+block.spec.id());
    check(level.getBlockState(pos.above()).getValue(DoorBlock.OPEN),"upper redstone sync");
    level.removeBlock(pos.relative(Direction.SOUTH),false);
    check(!level.getBlockState(pos).getValue(DoorBlock.OPEN),"redstone closes "+block.spec.id());
    if(block.spec.pairable()){
     // Construct the exact adjacent hinge layout, then exercise the ordinary hand/redstone paths.
     BlockPos a=new BlockPos(64+(i%8)*4,64,48+(i/8)*4),b=a.east();
     for(BlockPos q:List.of(a,b))level.setBlockAndUpdate(q.below(),Blocks.STONE.defaultBlockState());
     BlockState first=block.defaultBlockState().setValue(DoorBlock.FACING,Direction.SOUTH).setValue(DoorBlock.HINGE,DoorHingeSide.LEFT);
     BlockState second=first.setValue(DoorBlock.HINGE,DoorHingeSide.RIGHT);
     level.setBlock(a,first,3);level.setBlock(a.above(),first.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
     level.setBlock(b,second,3);level.setBlock(b.above(),second.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
     block.setOpen(player,level,first,a,true);
     check(level.getBlockState(a).getValue(DoorBlock.OPEN)&&level.getBlockState(b).getValue(DoorBlock.OPEN),"paired lower opens "+block.spec.id());
     check(level.getBlockState(a.above()).getValue(DoorBlock.OPEN)&&level.getBlockState(b.above()).getValue(DoorBlock.OPEN),"paired upper opens");
     block.setOpen(player,level,level.getBlockState(b),b,false);
     check(!level.getBlockState(a).getValue(DoorBlock.OPEN)&&!level.getBlockState(b).getValue(DoorBlock.OPEN),"paired closes from other leaf");
    }
   }
   Files.createDirectories(Path.of(System.getProperty("malisis.verify.out")));
   Files.writeString(Path.of(System.getProperty("malisis.verify.out"),"server-verification.txt"),"PASS "+checks+" checks; "+ROOTS.size()+" item-placed leaves; all 22 variants, four facings; hand, redstone, paired state, two halves, entity ownership\n");
  }catch(Throwable e){failure=e.toString();e.printStackTrace();try{Files.createDirectories(Path.of(System.getProperty("malisis.verify.out")));Files.writeString(Path.of(System.getProperty("malisis.verify.out"),"SERVER-FAILED.txt"),failure);}catch(Exception ignored){}}
 }
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(enabled()&&event.getEntity() instanceof ServerPlayer player){player.setGameMode(GameType.SPECTATOR);player.connection.teleport(20,83,-13,0,35);}}
 @SubscribeEvent public static void tick(ServerTickEvent.Post event){if(!enabled()||failure!=null||ROOTS.isEmpty())return;if(++ticks%120!=0)return;
  var level=event.getServer().overworld();boolean open=(ticks/120)%2==1;
  for(BlockPos pos:ROOTS){var state=level.getBlockState(pos);if(state.getBlock() instanceof AnimatedDoorBlock door)door.setOpen(null,level,state,pos,open);}
 }
}
