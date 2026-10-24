package net.malisis.doors.client;
import java.io.*;
import java.util.*;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import net.malisis.core.client.BoxMesh;
import net.malisis.core.model.ObjMesh;
import net.malisis.core.util.BoundedCache;
import net.malisis.doors.flat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public final class FlatRenderer implements BlockEntityRenderer<FlatEntity> {
 public static final java.util.Set<net.minecraft.resources.ResourceLocation> RENDERED=new java.util.HashSet<>();
 public static final BoundedCache<BlockState,BoxMesh[][]> MODELS=new BoundedCache<>(256);
 private final Matrix4f matrix=new Matrix4f();
 public FlatRenderer(BlockEntityRendererProvider.Context context){}
 private static BoxMesh[][] bake(BlockState state){
  if(state.getBlock() instanceof AnimatedTrapdoor block)return new BoxMesh[][]{{BoxMesh.cuboid(0,0,0,1,block.sliding?.09375f:.1875f,1,MechanismRenderer.sprite(block.texture))}};
  if(state.getBlock() instanceof LadderBlock){try(var input=Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.fromNamespaceAndPath("malisisdoors","models/rusty_hatch.obj"))){return new BoxMesh[][]{{ObjMesh.read(input,MechanismRenderer.sprite("rusty_hatch_handle")).get("ladder")}};}catch(IOException e){throw new IllegalStateException(e);}}
  TextureAtlasSprite texture=state.getBlock() instanceof CamouflageGate?MechanismRenderer.sprite("camo_fencegate"):Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
  BoxMesh[][] parts=new BoxMesh[2][4];
  for(int half=0;half<2;half++){
   float min=half*.5f,max=(half+1)*.5f;
   parts[half][0]=BoxMesh.cuboid(half==0?0:.875f,.3125f,.4375f,half==0?.125f:1,1,.5625f,texture);
   parts[half][1]=BoxMesh.cuboid(half==0?.375f:.5f,.375f,.4375f,half==0?.5f:.625f,.9375f,.5625f,texture);
   parts[half][2]=BoxMesh.cuboid(min+.125f,.375f,.4375f,max-.125f,.5625f,.5625f,texture);
   parts[half][3]=BoxMesh.cuboid(min+.125f,.75f,.4375f,max-.125f,.9375f,.5625f,texture);
  }
  return parts;
 }
 @Override public void render(FlatEntity entity,float partial,PoseStack stack,MultiBufferSource buffers,int light,int overlay){
  if(System.getProperty("malisis.verify.out")!=null)RENDERED.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()));
  var key=entity.trap()||entity.ladder()?entity.getBlockState():entity.camouflage();var parts=MODELS.get(key,FlatRenderer::bake);float p=entity.progress(partial);var buffer=buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
  if(!entity.trap()&&!entity.ladder()&&entity.partner()!=null){stack.pushPose();entity.pose(matrix,0,p);stack.mulPose(matrix);for(var half:parts)for(var mesh:half)mesh.emit(stack.last(),buffer,light,overlay);stack.popPose();}
  else for(int half=0;half<parts.length;half++){stack.pushPose();entity.pose(matrix,half,p);stack.mulPose(matrix);for(var mesh:parts[half])mesh.emit(stack.last(),buffer,light,overlay);stack.popPose();}
 }
 @Override public AABB getRenderBoundingBox(FlatEntity entity){return entity.coverage();}
}
