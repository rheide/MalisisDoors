package net.malisis.doors.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.MapCodec;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import net.malisis.doors.compat.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkStorage.class)
public abstract class LegacyChunkMixin {
  @Unique private Path malisis$region;
  @Unique private LegacyRegistry malisis$registry;

  @Inject(method = "<init>", at = @At("TAIL"))
  private void malisis$capture(
      RegionStorageInfo info, Path folder, DataFixer fixer, boolean sync, CallbackInfo ci) {
    malisis$region = folder;
    malisis$registry = LegacyRegistry.forRegion(folder).orElse(null);
  }

  @WrapMethod(method = "upgradeChunkTag")
  private CompoundTag malisis$upgrade(
      ResourceKey<Level> dimension,
      Supplier<DimensionDataStorage> storage,
      CompoundTag raw,
      Optional<ResourceKey<MapCodec<? extends ChunkGenerator>>> generator,
      Operation<CompoundTag> original) {
    if (malisis$registry == null || ChunkStorage.getVersion(raw) > 1343 || !raw.contains("Level"))
      return original.call(dimension, storage, raw, generator);
    try {
      var snapshot =
          new LegacyChunk(malisis$registry, raw, malisis$region, dimension.location().toString());
      if (!snapshot.relevant()) return original.call(dimension, storage, raw, generator);
      malisis$registry.backupChunk(snapshot.dimension, raw);
      return new LegacyDoorsUpgrade(snapshot)
          .restore(original.call(dimension, storage, snapshot.vanillaInput(), generator));
    } catch (Exception failure) {
      throw new LegacyUpgradeFailure(
          "Refusing to regenerate an unreadable legacy Malisis chunk; original data is retained",
          failure);
    }
  }
}
