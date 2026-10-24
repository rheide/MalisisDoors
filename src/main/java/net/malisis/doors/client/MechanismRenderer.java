package net.malisis.doors.client;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.mojang.blaze3d.vertex.*;
import net.malisis.core.client.BoxMesh;
import net.malisis.core.model.ObjMesh;
import net.malisis.core.util.BoundedCache;
import net.malisis.doors.mechanism.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
public final class MechanismRenderer implements BlockEntityRenderer<MechanismEntity> {
 public static final java.util.Set<String> RENDERED=new java.util.HashSet<>();
 public record Key(MechanismSpec spec,BlockState frame){}
 public record Part(String name,BoxMesh mesh){}
 public static final BoundedCache<Key,Part[]> MODELS=new BoundedCache<>(256);
 private final Matrix4f pose=new Matrix4f(),facing=new Matrix4f();
 public MechanismRenderer(BlockEntityRendererProvider.Context context){}
 public static TextureAtlasSprite sprite(String name){return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.fromNamespaceAndPath("malisisdoors","blocks/"+name));}
 private static Part[] bake(Key key){
  var spec=key.spec();var texture=sprite(spec.texture());List<Part> parts=new ArrayList<>();
  if(spec.kind()==MechanismSpec.Kind.GARAGE){parts.add(new Part("top",BoxMesh.cuboid(0,0,.40625f,1,1,.59375f,sprite("garage_door_top"))));parts.add(new Part("bottom",BoxMesh.cuboid(0,0,.40625f,1,1,.59375f,texture)));}
  else if(spec.kind()==MechanismSpec.Kind.LARGE){parts.add(new Part("left",BoxMesh.cuboidUV(-1,0,.8125f,.5f,3,1,texture,0,.5f)));parts.add(new Part("right",BoxMesh.cuboidUV(.5f,0,.8125f,2,3,1,texture,.5f,1)));}
  else {
   ResourceLocation path=ResourceLocation.fromNamespaceAndPath("malisisdoors","models/"+spec.model()+".obj");
   try(var input=Minecraft.getInstance().getResourceManager().openAsReader(path)){
    Map<String,BoxMesh> original=ObjMesh.read(input,texture);
    if(spec.kind()==MechanismSpec.Kind.CARRIAGE||spec.kind()==MechanismSpec.Kind.MEDIEVAL){
     var frameSprite=Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(key.frame());
     try(var frameInput=Minecraft.getInstance().getResourceManager().openAsReader(path)){var frames=ObjMesh.read(frameInput,frameSprite);if(frames.containsKey("frame"))parts.add(new Part("frame",frames.get("frame")));}
    }
    for(var entry:original.entrySet())if(!(entry.getKey().equals("frame")&&(spec.kind()==MechanismSpec.Kind.CARRIAGE||spec.kind()==MechanismSpec.Kind.MEDIEVAL))){
     if(spec.kind()==MechanismSpec.Kind.RUSTY_HATCH&&(entry.getKey().equals("handle")||entry.getKey().equals("ladder"))){try(var handleInput=Minecraft.getInstance().getResourceManager().openAsReader(path)){parts.add(new Part(entry.getKey(),ObjMesh.read(handleInput,sprite("rusty_hatch_handle")).get(entry.getKey())));}}
     else parts.add(new Part(entry.getKey(),entry.getValue()));
    }
   }catch(IOException e){throw new IllegalStateException("Unable to load original model "+path,e);}
  }
  return parts.toArray(Part[]::new);
 }
 public static Part[] model(MechanismSpec spec,BlockState frame){return MODELS.get(new Key(spec,frame),MechanismRenderer::bake);}
 @Override public void render(MechanismEntity entity,float partial,PoseStack stack,MultiBufferSource buffers,int light,int overlay){
  if(entity.spec().kind()==MechanismSpec.Kind.GARAGE&&entity.columnTop()!=entity)return;
  if(System.getProperty("malisis.verify.out")!=null)RENDERED.add(entity.spec().id());
  var parts=model(entity.spec(),entity.frame);float progress=entity.progress(partial);
  entity.facingPose(facing);
  stack.pushPose();stack.mulPose(facing);
  if(entity.spec().kind()==MechanismSpec.Kind.GARAGE){int count=entity.columnHeight();var buffer=buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));for(int down=0;down<count;down++){
   stack.pushPose();MechanismPose.garage(pose,count,down,progress);stack.mulPose(pose);int live=LevelRenderer.getLightColor(entity.getLevel(),entity.getBlockPos().below(down));parts[down==0?0:1].mesh.emit(stack.last(),buffer,live,overlay);stack.popPose();
  }}else for(var part:parts){
   if(part.name.equals("ladder"))continue; // Hatch ladders are emitted separately in each reserved shaft cell.
   stack.pushPose();entity.partPose(pose,part.name,progress);stack.mulPose(pose);
   part.mesh.emit(stack.last(),buffers.getBuffer(part.name.contains("glass")?RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS):RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),light,overlay);stack.popPose();
  }
  if(entity.spec().kind()==MechanismSpec.Kind.RUSTY_HATCH)for(var part:parts)if(part.name.equals("ladder")){
   boolean top=entity.getBlockState().getValue(MechanismBlock.TOP);for(int row=0;row<3;row++){stack.pushPose();stack.translate(0,top?row:-row,0);part.mesh.emit(stack.last(),buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS)),light,overlay);stack.popPose();}
  }
  stack.popPose();
 }
 @Override public int getViewDistance(){return 128;}
 @Override public AABB getRenderBoundingBox(MechanismEntity entity){return entity.coverage();}
}
