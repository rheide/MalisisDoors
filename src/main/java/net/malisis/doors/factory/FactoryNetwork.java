package net.malisis.doors.factory;

import net.malisis.doors.*;
import net.malisis.doors.custom.DoorConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = "malisisdoors", bus = EventBusSubscriber.Bus.MOD)
public final class FactoryNetwork {
  public record Configure(
      int container, BlockPos pos, CompoundTag settings, boolean createMode, boolean produce)
      implements CustomPacketPayload {
    public static final Type<Configure> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("malisisdoors", "factory_configure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Configure> CODEC =
        StreamCodec.of(
            (buffer, value) -> {
              buffer.writeVarInt(value.container);
              buffer.writeBlockPos(value.pos);
              buffer.writeNbt(value.settings);
              buffer.writeBoolean(value.createMode);
              buffer.writeBoolean(value.produce);
            },
            buffer ->
                new Configure(
                    buffer.readVarInt(),
                    buffer.readBlockPos(),
                    buffer.readNbt(),
                    buffer.readBoolean(),
                    buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record SubmitCode(int container, BlockPos pos, String code)
      implements CustomPacketPayload {
    public static final Type<SubmitCode> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("malisisdoors", "door_code"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SubmitCode> CODEC =
        StreamCodec.of(
            (buffer, value) -> {
              buffer.writeVarInt(value.container);
              buffer.writeBlockPos(value.pos);
              buffer.writeUtf(value.code, 6);
            },
            buffer ->
                new SubmitCode(buffer.readVarInt(), buffer.readBlockPos(), buffer.readUtf(6)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  @SubscribeEvent
  public static void register(RegisterPayloadHandlersEvent event) {
    var registry = event.registrar("1");
    registry.playToServer(
        Configure.TYPE,
        Configure.CODEC,
        (message, context) -> {
          if (context.player() instanceof ServerPlayer player) configure(player, message);
        });
    registry.playToServer(
        SubmitCode.TYPE,
        SubmitCode.CODEC,
        (message, context) -> {
          if (context.player() instanceof ServerPlayer player) code(player, message);
        });
  }

  public static boolean configure(ServerPlayer player, Configure message) {
    if (!(player.containerMenu instanceof FactoryMenu menu)
        || menu.containerId != message.container
        || !menu.position.equals(message.pos)
        || !menu.stillValid(player)
        || !player.level().hasChunkAt(message.pos)
        || !(player.level().getBlockEntity(message.pos) instanceof FactoryEntity factory)
        || message.settings == null) return false;
    var incoming =
        DoorConfiguration.read(message.settings, player.registryAccess(), factory.configuration);
    if (!net.malisis.doors.custom.DoorMotion.FACTORY.contains(incoming.movement())) return false;
    factory.configuration = incoming;
    factory.createMode = message.createMode;
    factory.sync();
    boolean produced = !message.produce || factory.createDoor();
    menu.status.set(0, produced ? 0 : 1);
    menu.broadcastChanges();
    return produced;
  }

  public static boolean code(ServerPlayer player, SubmitCode message) {
    if (!(player.containerMenu instanceof CodeMenu menu)
        || menu.containerId != message.container
        || !menu.position.equals(message.pos)
        || !menu.stillValid(player)
        || !player.level().hasChunkAt(message.pos)
        || !(player.level().getBlockEntity(message.pos) instanceof DoorBlockEntity door))
      return false;
    if (door.configuration.code().isEmpty() || !door.configuration.code().equals(message.code)) {
      menu.status.set(0, 1);
      menu.broadcastChanges();
      return false;
    }
    DoorInteractions.setOpen(
        door.block(), player.level(), door.getBlockState(), door.getBlockPos(), player, true);
    player.closeContainer();
    return true;
  }
}
