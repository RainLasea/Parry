package net.abysslasea.parry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ParryNetwork {
    private ParryNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        // Use the connection's player so a request cannot target somebody else.
        registrar.playToServer(Request.TYPE, Request.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ParryEvents.activate(player);
        });
        registrar.playToClient(Animation.TYPE, Animation.CODEC,
                (payload, context) -> NeoForge.EVENT_BUS.post(new AnimationEvent(payload.entityId())));
        registrar.playToClient(Success.TYPE, Success.CODEC,
                (payload, context) -> NeoForge.EVENT_BUS.post(new SuccessEvent()));
    }

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

    public static final class SuccessEvent extends Event {}

    /** Keeps network handlers independent of client classes on dedicated servers. */
    public static final class AnimationEvent extends Event {
        private final int entityId;
        public AnimationEvent(int entityId) { this.entityId = entityId; }
        public int entityId() { return entityId; }
    }
}
