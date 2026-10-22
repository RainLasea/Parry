package dev.abysslasea.aop;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ParryNetwork {
    public static final ResourceLocation REQUEST = ParryMod.id("parry_request");
    public static final ResourceLocation ANIMATION = ParryMod.id("parry_animation");
    public static final ResourceLocation SUCCESS = ParryMod.id("parry_success");
    public static final ResourceLocation COOLDOWN = ParryMod.id("parry_cooldown");
    public static final ResourceLocation CHECK = ParryMod.id("parry_check");
    public static final ResourceLocation RESULT = ParryMod.id("parry_result");
    private ParryNetwork() {}
    public static boolean canCheck(ServerPlayer player) { return ServerPlayNetworking.canSend(player, CHECK); }
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(RESULT, (server, player, handler, buffer, sender) -> {
            int requestId = buffer.readVarInt();
            boolean visible = buffer.readBoolean();
            server.execute(() -> {
                if (player.connection == handler && !player.hasDisconnected()) {
                    ParryPendingDamage.reply(player, requestId, visible);
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buffer, sender) -> server.execute(() -> {
            if (player.connection == handler && !player.hasDisconnected()) ParryEvents.activate(player);
        }));
    }
    public static void check(ServerPlayer player, int requestId, int targetId) {
        var buffer = PacketByteBufs.create();
        buffer.writeVarInt(requestId);
        buffer.writeVarInt(targetId);
        ServerPlayNetworking.send(player, CHECK, buffer);
    }
    public static void animation(ServerPlayer player) {
        for (var observer : PlayerLookup.tracking(player)) sendAnimation(observer, player.getId());
        sendAnimation(player, player.getId());
    }
    private static void sendAnimation(ServerPlayer observer, int id) {
        var buffer = PacketByteBufs.create(); buffer.writeVarInt(id);
        ServerPlayNetworking.send(observer, ANIMATION, buffer);
    }
    public static void success(ServerPlayer player) { ServerPlayNetworking.send(player, SUCCESS, PacketByteBufs.empty()); }
    public static void cooldown(ServerPlayer player, int remainingTicks, int durationTicks) {
        var buffer = PacketByteBufs.create();
        buffer.writeVarInt(remainingTicks);
        buffer.writeVarInt(durationTicks);
        ServerPlayNetworking.send(player, COOLDOWN, buffer);
    }
}
