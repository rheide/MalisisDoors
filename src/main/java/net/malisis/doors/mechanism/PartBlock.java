package net.malisis.doors.mechanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
public final class PartBlock extends Block implements EntityBlock {
 public PartBlock(Properties properties){super(properties);}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PartEntity(pos,state);}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
 @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return level.getBlockEntity(pos) instanceof PartEntity part&&part.origin()!=null?part.origin().selectionShape(pos):Shapes.empty();}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(level.getBlockEntity(pos) instanceof PartEntity part&&part.origin()!=null){if(!level.isClientSide)part.origin().toggle();return InteractionResult.sidedSuccess(level.isClientSide);}return InteractionResult.PASS;}
 @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){if(!level.isClientSide&&level.getBlockEntity(pos) instanceof PartEntity part&&part.origin()!=null)level.destroyBlock(part.root,!player.getAbilities().instabuild,player);return super.playerWillDestroy(level,pos,state,player);}
 @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof PartEntity part&&part.origin()!=null&&!part.origin().breaking)level.destroyBlock(part.root,true);super.onRemove(state,level,pos,next,moving);}
 @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){if(!level.isClientSide&&level.getBlockEntity(pos) instanceof PartEntity part&&part.origin()!=null&&level.hasNeighborSignal(pos))part.origin().open(true);}
}
