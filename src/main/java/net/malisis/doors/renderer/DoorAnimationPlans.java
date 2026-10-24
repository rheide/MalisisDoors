package net.malisis.doors.renderer;

import java.util.*;
import net.malisis.core.renderer.RenderParameters;
import net.malisis.core.renderer.animation.Animation;
import net.malisis.core.renderer.model.MalisisModel;
import net.malisis.doors.DoorState;
import net.malisis.doors.movement.*;
import net.malisis.doors.tileentity.*;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Bounded, renderer-owned plans. Pose/time and model resetting remain live. */
@Mod.EventBusSubscriber(modid="malisisdoors",value=Side.CLIENT)
public final class DoorAnimationPlans {
    private static final int LIMIT=512;
    private static final Set<DoorAnimationPlans> INSTANCES=Collections.newSetFromMap(new WeakHashMap<DoorAnimationPlans,Boolean>());
    private final Map<Key,Animation<?>[]> plans=new LinkedHashMap<Key,Animation<?>[]>(32,.75f,true);
    public DoorAnimationPlans(){INSTANCES.add(this);}
    public Animation<?>[] get(DoorTileEntity tile,MalisisModel model,RenderParameters parameters){
        IDoorMovement movement=tile.getMovement();
        if(movement==null)return null;
        // Third-party movements may depend on data beyond this deliberately audited key.
        if(!movement.getClass().getName().startsWith("net.malisis.doors.movement."))
            return movement.getAnimations(tile,model,parameters);
        int flags=(tile.isHingeLeft()?1:0)|(tile.isOpened()?2:0);
        if(tile instanceof RustyHatchTileEntity && ((RustyHatchTileEntity)tile).isTop() || tile instanceof TrapDoorTileEntity && ((TrapDoorTileEntity)tile).isTop())flags|=1<<14;
        if(tile instanceof SaloonDoorTileEntity && ((SaloonDoorTileEntity)tile).isBackward())flags|=4;
        if(movement instanceof FenceGateMovement){
            flags|=tile.getBlockMetadata()<<4;flags|=tile.getDirection().ordinal()<<9;
            DoorTileEntity partner=tile.getDoubleDoor();
            if(partner!=null){flags|=1<<12;boolean left=tile.getDirection().getAxis()==net.minecraft.util.EnumFacing.Axis.X
                    ?tile.getPos().getZ()<partner.getPos().getZ():tile.getPos().getX()>partner.getPos().getX();if(left)flags|=1<<13;}
        }else if(movement instanceof SpinningAroundDoorMovement && tile.getDoubleDoor()!=null)flags|=1<<12;
        Key key=new Key(movement,model,parameters,tile.getState(),tile.getDescriptor().getOpeningTime(),flags);
        Animation<?>[] result=plans.get(key);
        if(result==null){result=movement.getAnimations(tile,model,parameters);plans.put(key,result);if(plans.size()>LIMIT)plans.remove(plans.keySet().iterator().next());}
        return result;
    }
    public int size(){return plans.size();}
    public static void clearAll(){for(DoorAnimationPlans cache:new ArrayList<>(INSTANCES))cache.plans.clear();}
    @SubscribeEvent public static void textures(TextureStitchEvent.Post event){clearAll();}
    @SubscribeEvent public static void world(WorldEvent.Unload event){if(event.getWorld().isRemote)clearAll();}
    private static final class Key {
        final Object movement,model,parameters;final DoorState state;final int duration,flags;
        Key(Object movement,Object model,Object parameters,DoorState state,int duration,int flags){this.movement=movement;this.model=model;this.parameters=parameters;this.state=state;this.duration=duration;this.flags=flags;}
        public int hashCode(){int h=System.identityHashCode(movement);h=31*h+System.identityHashCode(model);h=31*h+System.identityHashCode(parameters);return 31*(31*(31*h+state.ordinal())+duration)+flags;}
        public boolean equals(Object o){if(!(o instanceof Key))return false;Key k=(Key)o;return movement==k.movement&&model==k.model&&parameters==k.parameters&&state==k.state&&duration==k.duration&&flags==k.flags;}
    }
}
