package net.malisis.doors;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class AnimatedDoorBlock extends DoorBlock implements EntityBlock {
 public final DoorSpec spec;
 public AnimatedDoorBlock(DoorSpec spec,BlockBehaviour.Properties properties){super(spec.redstoneOnly()?BlockSetType.IRON:BlockSetType.OAK,properties);this.spec=spec;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(HALF)==DoubleBlockHalf.LOWER?new DoorBlockEntity(pos,state):null;}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(spec.redstoneOnly())return InteractionResult.PASS;
  if(!level.isClientSide)setOpen(player,level,state,pos,!state.getValue(OPEN));
  return InteractionResult.sidedSuccess(level.isClientSide);
 }
 @Override public void setOpen(@Nullable Entity entity,Level level,BlockState state,BlockPos pos,boolean open){
  super.setOpen(entity,level,state,pos,open);
  if(level.isClientSide||!spec.pairable())return;
  BlockPos root=state.getValue(HALF)==DoubleBlockHalf.LOWER?pos:pos.below();
  Direction side=state.getValue(HINGE)==DoorHingeSide.LEFT?state.getValue(FACING).getCounterClockWise():state.getValue(FACING).getClockWise();
  BlockPos other=root.relative(side);BlockState partner=level.getBlockState(other);
  if(partner.is(this)&&partner.getValue(FACING)==state.getValue(FACING)&&partner.getValue(HINGE)!=state.getValue(HINGE))super.setOpen(entity,level,partner,other,open);
 }
 @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
  super.neighborChanged(state,level,pos,block,from,moving);
  BlockState after=level.getBlockState(pos);
  if(!level.isClientSide&&after.is(this)&&state.getValue(OPEN)!=after.getValue(OPEN))setOpen(null,level,after,pos,after.getValue(OPEN));
 }
 @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
  BlockPos root=state.getValue(HALF)==DoubleBlockHalf.LOWER?pos:pos.below();
  return level.getBlockEntity(root) instanceof DoorBlockEntity door?door.outline(pos):super.getShape(state,level,pos,context);
 }
}
