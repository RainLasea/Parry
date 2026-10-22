package dev.abysslasea.aop;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ParryNetwork {
    public static final ResourceLocation REQUEST = ParryMod.id("parry_request");
    public static final ResourceLocation ANIMATION = ParryMod.id("parry_animation");
    public static final ResourceLocation SUCCESS = ParryMod.id("parry_success");
    private ParryNetwork() {}
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buffer, sender) -> server.execute(() -> {
            if (player.connection == handler && !player.hasDisconnected()) ParryEvents.activate(player);
        }));
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
}
