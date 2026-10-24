package net.malisis.doors.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.malisis.core.client.BoxMesh;
import net.malisis.core.util.BoundedCache;
import net.malisis.doors.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
public final class DoorRenderer implements BlockEntityRenderer<DoorBlockEntity> {
 public static int resourceGeneration;
 public static final BoundedCache<DoorSpec,BoxMesh[]> MESHES=new BoundedCache<>(512);
 public DoorRenderer(BlockEntityRendererProvider.Context context){}
 @Override public void render(DoorBlockEntity door,float partial,PoseStack stack,MultiBufferSource buffers,int light,int overlay){
  if(!(door.getBlockState().getBlock() instanceof AnimatedDoorBlock block))return;
  var state=door.getBlockState();var spec=block.spec;
  boolean left=state.getValue(DoorBlock.HINGE)==DoorHingeSide.LEFT;
  float p=door.progress(partial);float dx=(left?1:-1)*13f/16*p;
  var consumer=buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
  stack.pushPose();stack.translate(.5,0,.5);stack.mulPose(Axis.YP.rotationDegrees(-state.getValue(DoorBlock.FACING).toYRot()));stack.translate(-.5,0,-.5);
  for(int half=0;half<2;half++){
   stack.pushPose();stack.translate(0,half,0);
   switch(spec.movement()){
    case SLIDE->stack.translate(dx,0,0);
    case UP->stack.translate(0,(2-3f/16)*p,0);
    case SPLIT->stack.translate(0,(half==1?13f/16:-1.1f)*p,0);
    case CURTAIN->{if(left)stack.translate(13f/16*p,0,0);stack.scale(1-13f/16*p,1,1);}
   }
   BoxMesh mesh=MESHES.get(spec,DoorRenderer::bake)[half*2+(left?1:0)];
   int liveLight=LevelRenderer.getLightColor(door.getLevel(),door.getBlockPos().above(half));
   mesh.emit(stack.last(),consumer,liveLight,overlay);stack.popPose();
  }
  stack.popPose();
 }
 private static BoxMesh[] bake(DoorSpec spec){
  BoxMesh[] result=new BoxMesh[4];var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
  var side=atlas.apply(ResourceLocation.fromNamespaceAndPath("malisisdoors","blocks/"+spec.texture()+"_side"));
  for(int half=0;half<2;half++)for(int mirror=0;mirror<2;mirror++){
   var front=atlas.apply(ResourceLocation.fromNamespaceAndPath("malisisdoors","blocks/"+spec.texture()+(half==1?"_upper":"_lower")));
   result[half*2+mirror]=BoxMesh.panel(front,side,half==1,mirror==1);
  }
  return result;
 }
 @Override public AABB getRenderBoundingBox(DoorBlockEntity door){return door.renderBounds;}
 @Override public int getViewDistance(){return 128;}
}
