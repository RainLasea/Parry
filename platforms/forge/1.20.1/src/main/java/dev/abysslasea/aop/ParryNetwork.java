package dev.abysslasea.aop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ParryNetwork {
    private static final String PROTOCOL = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ParryMod.id("network"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private ParryNetwork() {}
    public static boolean canCheck(ServerPlayer player) { return true; } // Protocol 4 is required at login.
    public static void register() {
        CHANNEL.messageBuilder(Check.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {
                    buffer.writeVarInt(message.requestId());
                    buffer.writeVarInt(message.targetId());
                })
                .decoder(buffer -> new Check(buffer.readVarInt(), buffer.readVarInt()))
                .consumerMainThread((message, context) -> MinecraftForge.EVENT_BUS.post(
                        new CheckEvent(message.requestId(), message.targetId()))).add();
        CHANNEL.messageBuilder(Result.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> {
                    buffer.writeVarInt(message.requestId());
                    buffer.writeBoolean(message.visible());
                })
                .decoder(buffer -> new Result(buffer.readVarInt(), buffer.readBoolean()))
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null) ParryPendingDamage.reply(player, message.requestId(), message.visible());
                }).add();
        CHANNEL.messageBuilder(Cooldown.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> {
                    buffer.writeVarInt(message.remainingTicks());
                    buffer.writeVarInt(message.durationTicks());
                })
                .decoder(buffer -> new Cooldown(buffer.readVarInt(), buffer.readVarInt()))
                .consumerMainThread((message, context) -> MinecraftForge.EVENT_BUS.post(
                        new CooldownEvent(message.remainingTicks(), message.durationTicks()))).add();
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
    public static void check(ServerPlayer player, int requestId, int targetId) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Check(requestId, targetId));
    }
    public static void result(int requestId, boolean visible) {
        CHANNEL.sendToServer(new Result(requestId, visible));
    }
    public record Check(int requestId, int targetId) {}
    public record Result(int requestId, boolean visible) {}
    public static final class CheckEvent extends Event {
        private final int requestId;
        private final int targetId;
        public CheckEvent(int requestId, int targetId) {
            this.requestId = requestId;
            this.targetId = targetId;
        }
        public int requestId() { return requestId; }
        public int targetId() { return targetId; }
    }

    public static void request() { CHANNEL.sendToServer(new Request()); }
    public static void animation(ServerPlayer player) { CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new Animation(player.getId())); }
    public static void success(ServerPlayer player) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Success()); }
    public static void cooldown(ServerPlayer player, int remainingTicks, int durationTicks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Cooldown(remainingTicks, durationTicks));
    }
    public record Cooldown(int remainingTicks, int durationTicks) {}
    public static final class CooldownEvent extends Event {
        private final int remainingTicks;
        private final int durationTicks;
        public CooldownEvent(int remainingTicks, int durationTicks) {
            this.remainingTicks = remainingTicks;
            this.durationTicks = durationTicks;
        }
        public int remainingTicks() { return remainingTicks; }
        public int durationTicks() { return durationTicks; }
    }
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
