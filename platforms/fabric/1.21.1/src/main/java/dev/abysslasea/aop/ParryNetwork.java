package dev.abysslasea.aop;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.*;

public final class ParryNetwork {
    private ParryNetwork() {}
    public static boolean canCheck(ServerPlayer player) { return ServerPlayNetworking.canSend(player, Check.TYPE); }
    public static void register() {
        PayloadTypeRegistry.playC2S().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.playC2S().register(Result.TYPE, Result.CODEC);
        PayloadTypeRegistry.playS2C().register(Sound.TYPE, Sound.CODEC);
        PayloadTypeRegistry.playS2C().register(Check.TYPE, Check.CODEC);
        PayloadTypeRegistry.playS2C().register(Animation.TYPE, Animation.CODEC);
        PayloadTypeRegistry.playS2C().register(Success.TYPE, Success.CODEC);
        PayloadTypeRegistry.playS2C().register(Cooldown.TYPE, Cooldown.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE, (payload, context) -> ParryEvents.activate(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(Result.TYPE, (payload, context) ->
                ParryPendingDamage.reply(context.player(), payload.requestId(), payload.visible()));
    }
    public static void check(ServerPlayer player, int requestId, int targetId) {
        ServerPlayNetworking.send(player, new Check(requestId, targetId));
    }
    public static void animation(ServerPlayer player) {
        var packet = new Animation(player.getId());
        for (var observer : PlayerLookup.tracking(player)) ServerPlayNetworking.send(observer, packet);
        ServerPlayNetworking.send(player, packet);
    }
    public static void success(ServerPlayer player) { ServerPlayNetworking.send(player, Success.INSTANCE); }
    public static void cooldown(ServerPlayer player, int remainingTicks, int durationTicks) {
        ServerPlayNetworking.send(player, new Cooldown(remainingTicks, durationTicks));
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

    public record Sound(String sound, double x, double y, double z, float pitch, boolean success) implements CustomPacketPayload {
        public static final Type<Sound> TYPE = new Type<>(ParryMod.id("parry_sound"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sound> CODEC = new StreamCodec<>() {
            @Override public Sound decode(RegistryFriendlyByteBuf buffer) {
                return new Sound(buffer.readUtf(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readBoolean());
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, Sound sound) {
                buffer.writeUtf(sound.sound()); buffer.writeDouble(sound.x()); buffer.writeDouble(sound.y());
                buffer.writeDouble(sound.z()); buffer.writeFloat(sound.pitch()); buffer.writeBoolean(sound.success());
            }
        };
        @Override public Type<Sound> type() { return TYPE; }
    }
    public static void sound(ServerPlayer observer, net.minecraft.resources.ResourceLocation id, double x, double y, double z, float pitch, boolean success) {
        if (!ServerPlayNetworking.canSend(observer, Sound.TYPE)) return;
        ServerPlayNetworking.send(observer, new Sound(id.toString(), x, y, z, pitch, success));
    }
}
