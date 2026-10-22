package dev.abysslasea.aop.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import dev.abysslasea.aop.ParryNetwork;

final class ParryClientNetwork {
    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.ANIMATION, (client, handler, buffer, sender) -> {
            int id = buffer.readVarInt();
            client.execute(() -> ParryClient.animation(id));
        });
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.SUCCESS, (client, handler, buffer, sender) -> client.execute(ParryClient::success));
    }
    static void request() { ClientPlayNetworking.send(ParryNetwork.REQUEST, PacketByteBufs.empty()); }
}
