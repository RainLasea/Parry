package dev.abysslasea.aop;

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
    public static boolean canCheck(ServerPlayer player) { return true; } // Protocol 4 is required at login.

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToServer(Result.TYPE, Result.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ParryPendingDamage.reply(player, payload.requestId(), payload.visible());
            }
        });
        registrar.playToClient(Check.TYPE, Check.CODEC,
                (payload, context) -> NeoForge.EVENT_BUS.post(new CheckEvent(payload.requestId(), payload.targetId())));
        // Use the connection's player so a request cannot target somebody else.
        registrar.playToServer(Request.TYPE, Request.CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) ParryEvents.activate(player);
        });
        registrar.playToClient(Animation.TYPE, Animation.CODEC,
                (payload, context) -> NeoForge.EVENT_BUS.post(new AnimationEvent(payload.entityId())));
        registrar.playToClient(Success.TYPE, Success.CODEC,
                (payload, context) -> NeoForge.EVENT_BUS.post(new SuccessEvent()));
        registrar.playToClient(Cooldown.TYPE, Cooldown.CODEC,
                (payload, context) -> NeoForge.EVENT_BUS.post(
                        new CooldownEvent(payload.remainingTicks(), payload.durationTicks())));
    }

    public static void check(ServerPlayer player, int requestId, int targetId) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new Check(requestId, targetId));
    }

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

    public record Check(int requestId, int targetId) implements CustomPacketPayload {
        public static final Type<Check> TYPE = new Type<>(ParryMod.id("parry_check"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Check> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Check::requestId,
                ByteBufCodecs.VAR_INT, Check::targetId, Check::new);
        @Override public Type<Check> type() { return TYPE; }
    }

    public record Result(int requestId, boolean visible) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(ParryMod.id("parry_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Result::requestId,
                ByteBufCodecs.BOOL, Result::visible, Result::new);
        @Override public Type<Result> type() { return TYPE; }
    }

    public record Cooldown(int remainingTicks, int durationTicks) implements CustomPacketPayload {
        public static final Type<Cooldown> TYPE = new Type<>(ParryMod.id("parry_cooldown"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Cooldown> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Cooldown::remainingTicks,
                ByteBufCodecs.VAR_INT, Cooldown::durationTicks, Cooldown::new);
        @Override public Type<Cooldown> type() { return TYPE; }
    }

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
