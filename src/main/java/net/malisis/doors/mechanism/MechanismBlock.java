package net.malisis.doors.mechanism;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class MechanismBlock extends Block implements EntityBlock {
 public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
 public static final BooleanProperty OPEN=BlockStateProperties.OPEN,POWERED=BlockStateProperties.POWERED,TOP=BooleanProperty.create("top");
 public final MechanismSpec spec;
 public MechanismBlock(MechanismSpec spec,Properties properties){super(properties);this.spec=spec;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(OPEN,false).setValue(POWERED,false).setValue(TOP,false));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,OPEN,POWERED,TOP);}
 public List<BlockPos> footprint(BlockPos origin,BlockState state){List<BlockPos> positions=new ArrayList<>();int startY=spec.kind()==MechanismSpec.Kind.RUSTY_HATCH&&!state.getValue(TOP)?-2:0;for(int x=spec.minX();x<spec.minX()+spec.width();x++)for(int y=startY;y<startY+spec.height();y++)for(int z=0;z<spec.depth();z++)positions.add(origin.offset(MechanismPose.offset(state.getValue(FACING),x,y,z)));return positions;}
 @Override public BlockState getStateForPlacement(BlockPlaceContext context){
  if(spec.kind()==MechanismSpec.Kind.RUSTY_HATCH&&context.getClickedFace().getAxis().isVertical())return null;
  var state=defaultBlockState().setValue(FACING,context.getHorizontalDirection()).setValue(TOP,context.getClickLocation().y-Math.floor(context.getClickLocation().y)>.5);
  if(spec.kind()==MechanismSpec.Kind.GARAGE){var above=context.getLevel().getBlockState(context.getClickedPos().above());var below=context.getLevel().getBlockState(context.getClickedPos().below());if(above.is(this))state=state.setValue(FACING,above.getValue(FACING));else if(below.is(this))state=state.setValue(FACING,below.getValue(FACING));}
  for(var p:footprint(context.getClickedPos(),state))if(context.getLevel().isOutsideBuildHeight(p)||!context.getLevel().getBlockState(p).canBeReplaced(context))return null;
  return state;
 }
 @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity player,ItemStack stack){
  super.setPlacedBy(level,pos,state,player,stack);
  if(!level.isClientSide){for(var p:footprint(pos,state))if(!p.equals(pos)){level.setBlock(p,Mechanisms.PART.get().defaultBlockState(),3);if(level.getBlockEntity(p) instanceof PartEntity part)part.setRoot(pos);}}
  if(level.getBlockEntity(pos) instanceof MechanismEntity entity){entity.loadItem(stack);net.malisis.core.collision.GeometryIndex.of(level).add(entity);}
 }
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(spec.kind()==MechanismSpec.Kind.GARAGE)return InteractionResult.PASS;if(!level.isClientSide&&level.getBlockEntity(pos) instanceof MechanismEntity entity)entity.toggle();return InteractionResult.sidedSuccess(level.isClientSide);}
 @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(spec.kind()==MechanismSpec.Kind.CARRIAGE||spec.kind()==MechanismSpec.Kind.MEDIEVAL||spec.kind()==MechanismSpec.Kind.RUSTY_HATCH)return;if(!level.isClientSide&&level.getBlockEntity(pos) instanceof MechanismEntity entity){boolean power=level.hasNeighborSignal(pos);if(power!=state.getValue(POWERED)){level.setBlock(pos,state.setValue(POWERED,power),2);entity.open(power);}}}
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof MechanismEntity entity)entity.breakStructure();super.onRemove(state,level,pos,next,moving);}
 @Override protected java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params){var tile=params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);return tile instanceof MechanismEntity entity?java.util.List.of(entity.item()):super.getDrops(state,params);}
 @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state){return level.getBlockEntity(pos) instanceof MechanismEntity entity?entity.item():super.getCloneItemStack(level,pos,state);}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MechanismEntity(pos,state);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return type==Mechanisms.ENTITY.get()?(world,pos,s,tile)->((MechanismEntity)tile).tick():null;}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return level.getBlockEntity(pos) instanceof MechanismEntity entity?entity.selectionShape(pos):Shapes.empty();}
}
