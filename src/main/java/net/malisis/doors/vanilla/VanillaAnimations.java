package net.malisis.doors.vanilla;

import java.util.*;
import net.malisis.doors.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.*;

public final class VanillaAnimations {
  private static final Map<Block, DoorSpec> SPECS =
      Collections.synchronizedMap(new IdentityHashMap<>());
  private static final ClassValue<Boolean> OWN_ENTITIES =
      new ClassValue<>() {
        protected Boolean computeValue(Class<?> type) {
          for (Class<?> c = type;
              c != null
                  && c != DoorBlock.class
                  && c != TrapDoorBlock.class
                  && c != FenceGateBlock.class;
              c = c.getSuperclass())
            for (var contract : c.getInterfaces())
              if (EntityBlock.class.isAssignableFrom(contract)) return true;
          return false;
        }
      };

  public static boolean ownsEntity(Class<?> type) {
    return OWN_ENTITIES.get(type);
  }

  public static boolean enabled(Block block) {
    var key = BuiltInRegistries.BLOCK.getKey(block);
    return Settings.modifyVanillaDoors && key != null && key.getNamespace().equals("minecraft");
  }

  public static DoorSpec spec(Block block) {
    if (block instanceof AnimatedDoorBlock animated) return animated.spec;
    return SPECS.computeIfAbsent(
        block,
        key ->
            new DoorSpec(
                BuiltInRegistries.BLOCK.getKey(block).toString(),
                "",
                DoorSpec.Movement.SLIDE,
                6,
                !((DoorBlock) block).type().canOpenByHand(),
                true));
  }

  public static java.util.stream.Stream<Block> doors() {
    return BuiltInRegistries.BLOCK.stream()
        .filter(
            block ->
                block instanceof DoorBlock
                    && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft"));
  }

  public static java.util.stream.Stream<Block> flats() {
    return BuiltInRegistries.BLOCK.stream()
        .filter(
            block ->
                (block instanceof TrapDoorBlock || block instanceof FenceGateBlock)
                    && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft"));
  }
}
