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
 public static final java.util.List<BlockPos> MECHANISMS=new java.util.ArrayList<>();
 static int ticks,checks;static String failure;
 public static void check(boolean condition,String message){checks++;if(!condition)throw new IllegalStateException(message);}
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
  var door=(DoorBlockEntity)level.getBlockEntity(pos);var closed=door.collisionBoxes().getFirst();
  var expected=net.minecraft.world.level.block.Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,facing).getCollisionShape(level,pos).bounds().move(pos);
  check(Math.abs(closed.minX-expected.minX)+Math.abs(closed.minY-expected.minY)+Math.abs(closed.minZ-expected.minZ)+Math.abs(closed.maxX-expected.maxX)+Math.abs(closed.maxY-expected.maxY)+Math.abs(closed.maxZ-expected.maxZ)<.00001,"closed carried collision follows facing "+facing);

 }
 @SubscribeEvent public static void started(ServerStartedEvent event){if(!enabled())return;
  try{
   var level=event.getServer().overworld();level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,event.getServer());level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,event.getServer());
   for(int x=-4;x<48;x++)for(int z=-8;z<40;z++){level.setBlockAndUpdate(new BlockPos(x,63,z),Blocks.STONE.defaultBlockState());for(int y=64;y<71;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),2);}
   // Core hooks must add displaced geometry to both physical collision and ray selection.
   var index=net.malisis.core.collision.GeometryIndex.of(level);
   net.malisis.core.collision.MovingGeometry fixture=new net.malisis.core.collision.MovingGeometry(){
    public net.minecraft.world.phys.AABB coverage(){return new net.minecraft.world.phys.AABB(199,63,199,203,67,203);}
    public java.util.List<net.minecraft.world.phys.AABB> collisionBoxes(){return java.util.List.of(new net.minecraft.world.phys.AABB(200,64,200,202,66,201));}
    public BlockPos interactionPosition(net.minecraft.world.phys.Vec3 p){return new BlockPos(200,64,200);}
    public boolean alive(){return true;}
   };
   index.add(fixture);boolean collision=false;for(var shape:level.getBlockCollisions(null,new net.minecraft.world.phys.AABB(200.5,64.5,200.25,201,65,200.75)))if(shape.bounds().intersects(200.5,64.5,200.25,201,65,200.75))collision=true;
   check(collision,"Core dynamic geometry collision hook");
   var ray=level.clip(new net.minecraft.world.level.ClipContext(new net.minecraft.world.phys.Vec3(199,65,200.5),new net.minecraft.world.phys.Vec3(203,65,200.5),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level)));
   check(ray.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&ray.getBlockPos().equals(new BlockPos(200,64,200)),"Core displaced geometry selection hook");index.remove(fixture);
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
   int mechanismIndex=0;
   for(var entry:net.malisis.doors.mechanism.Mechanisms.BLOCKS)for(Direction face:Direction.Plane.HORIZONTAL){
    var block=entry.get();BlockPos origin=new BlockPos((mechanismIndex%8)*8,64,48+(mechanismIndex/8)*8);mechanismIndex++;
    var state=block.defaultBlockState().setValue(net.malisis.doors.mechanism.MechanismBlock.FACING,face).setValue(net.malisis.doors.mechanism.MechanismBlock.TOP,true);
    for(BlockPos q:block.footprint(origin,state)){level.removeBlock(q,false);level.setBlockAndUpdate(new BlockPos(q.getX(),63,q.getZ()),Blocks.STONE.defaultBlockState());}
    BlockPos support=origin.below();Direction placingFace=Direction.UP;Vec3 point=Vec3.atCenterOf(support).add(0,.5,0);
    if(block.spec.kind()==net.malisis.doors.mechanism.MechanismSpec.Kind.RUSTY_HATCH){support=origin.offset(net.malisis.doors.mechanism.MechanismPose.offset(face,-1,0,0));placingFace=face.getCounterClockWise();level.setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());point=Vec3.atCenterOf(support).add(placingFace.getStepX()*.5,.25,placingFace.getStepZ()*.5);}
    var player=FakePlayerFactory.getMinecraft(level);player.setYRot(face.toYRot());player.setPos(origin.getX()+.5,64,origin.getZ()+.5);var stack=new net.minecraft.world.item.ItemStack(block);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
    var result=stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(point,placingFace,support,false)));
    check(result.consumesAction(),"mechanism placement "+block.spec.id()+" "+face);
    check(level.getBlockEntity(origin) instanceof net.malisis.doors.mechanism.MechanismEntity,"mechanism anchor "+block.spec.id());
    var entity=(net.malisis.doors.mechanism.MechanismEntity)level.getBlockEntity(origin);MECHANISMS.add(origin);
    for(BlockPos q:block.footprint(origin,level.getBlockState(origin)))if(!q.equals(origin))check(level.getBlockEntity(q) instanceof net.malisis.doors.mechanism.PartEntity part&&part.root.equals(origin),"mechanism proxy points to root");
    check(!entity.collisionBoxes().isEmpty(),"mechanism has closed collision "+block.spec.id());
    entity.open(true);entity.animation.reset(true,level.getGameTime());entity.sync();check(entity.isOpen(),"mechanism opens "+block.spec.id());
    var bounds=entity.collisionBoxes().getFirst();boolean physical=false;for(var shape:level.getBlockCollisions(null,bounds.deflate(.001)))if(shape.bounds().intersects(bounds))physical=true;
    check(physical,"mechanism carried collision "+block.spec.id());
    entity.open(false);entity.animation.reset(false,level.getGameTime());entity.sync();
   }
   Files.createDirectories(Path.of(System.getProperty("malisis.verify.out")));
   Files.writeString(Path.of(System.getProperty("malisis.verify.out"),"server-verification.txt"),"PASS "+checks+" checks; "+MECHANISMS.size()+" item-placed multiblock mechanisms; "+ROOTS.size()+" item-placed leaves; all 22 variants, four facings; hand, redstone, paired state, two halves, entity ownership\n");
  }catch(Throwable e){failure=e.toString();e.printStackTrace();try{Files.createDirectories(Path.of(System.getProperty("malisis.verify.out")));Files.writeString(Path.of(System.getProperty("malisis.verify.out"),"SERVER-FAILED.txt"),failure);}catch(Exception ignored){}}
 }
 @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent event){if(!enabled())return;
  event.getDispatcher().register(net.minecraft.commands.Commands.literal("malisisverifyview").then(net.minecraft.commands.Commands.argument("view",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(context->{
   var player=context.getSource().getPlayerOrException();var view=com.mojang.brigadier.arguments.StringArgumentType.getString(context,"view");
   if(view.equals("mechanisms"))player.connection.teleport(28,88,36,0,35);else player.connection.teleport(20,83,-13,0,35);return 1;
  })));
 }
 @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(enabled()&&event.getEntity() instanceof ServerPlayer player){player.setGameMode(GameType.SPECTATOR);player.connection.teleport(20,83,-13,0,35);}}
 @SubscribeEvent public static void tick(ServerTickEvent.Post event){if(!enabled()||failure!=null||ROOTS.isEmpty())return;if(++ticks%120!=0)return;
  var level=event.getServer().overworld();boolean open=(ticks/120)%2==1;
  for(BlockPos pos:ROOTS){var state=level.getBlockState(pos);if(state.getBlock() instanceof AnimatedDoorBlock door)door.setOpen(null,level,state,pos,open);}
 }
}
