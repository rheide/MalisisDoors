package net.malisis.doors.flat;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
public final class AnimatedTrapdoor extends TrapDoorBlock implements EntityBlock {
 public final String texture;public final boolean sliding;
 public AnimatedTrapdoor(String texture,boolean sliding,Properties properties){super(BlockSetType.OAK,properties);this.texture=texture;this.sliding=sliding;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FlatEntity(pos,state);}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
 @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return level.getBlockEntity(pos) instanceof FlatEntity flat?flat.outline():super.getShape(state,level,pos,context);}
}
