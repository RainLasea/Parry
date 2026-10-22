package dev.abysslasea.aop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ParryNetwork {
    private static final String PROTOCOL = "2";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ParryMod.id("network"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private ParryNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> {}).decoder(buffer -> new Request())
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null) ParryEvents.activate(player);
                }).add();
        CHANNEL.messageBuilder(Animation.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> buffer.writeVarInt(message.entityId()))
                .decoder(buffer -> new Animation(buffer.readVarInt()))
                .consumerMainThread((message, context) -> MinecraftForge.EVENT_BUS.post(new AnimationEvent(message.entityId()))).add();
        CHANNEL.messageBuilder(Success.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {}).decoder(buffer -> new Success())
                .consumerMainThread((message, context) -> MinecraftForge.EVENT_BUS.post(new SuccessEvent())).add();
    }
    public static void request() { CHANNEL.sendToServer(new Request()); }
    public static void animation(ServerPlayer player) { CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new Animation(player.getId())); }
    public static void success(ServerPlayer player) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Success()); }
    public record Request() {}
    public record Animation(int entityId) {}
    public record Success() {}
    public static final class SuccessEvent extends Event {}
    public static final class AnimationEvent extends Event {
        private final int entityId;
        public AnimationEvent(int entityId) { this.entityId = entityId; }
        public int entityId() { return entityId; }
    }
}
