package dev.abysslasea.aop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.*;

public final class ParryNetwork {
    private ParryNetwork() {}
    public static void register() {
        PayloadTypeRegistry.playC2S().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.playS2C().register(Animation.TYPE, Animation.CODEC);
        PayloadTypeRegistry.playS2C().register(Success.TYPE, Success.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (payload, context) -> ParryEvents.activate(context.player()));
    }
    public static void animation(ServerPlayer player) {
        var packet = new Animation(player.getId());
        for (var observer : PlayerLookup.tracking(player)) ServerPlayNetworking.send(observer, packet);
        ServerPlayNetworking.send(player, packet);
    }
    public static void success(ServerPlayer player) { ServerPlayNetworking.send(player, Success.INSTANCE); }
    public record Request() implements CustomPacketPayload {
        public static final Request INSTANCE = new Request();
        public static final Type<Request> TYPE = new Type<>(ParryMod.id("parry_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<Request> type() { return TYPE; }
    }

    public record Animation(int entityId) implements CustomPacketPayload {
        public static final Type<Animation> TYPE = new Type<>(ParryMod.id("parry_animation"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Animation> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Animation::entityId, Animation::new);
        @Override public Type<Animation> type() { return TYPE; }
    }

    /** Targets the parrying player so nearby observers do not receive camera recoil. */
    public record Success() implements CustomPacketPayload {
        public static final Success INSTANCE = new Success();
        public static final Type<Success> TYPE = new Type<>(ParryMod.id("parry_success"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Success> CODEC = StreamCodec.unit(INSTANCE);
        @Override public Type<Success> type() { return TYPE; }
    }

}
