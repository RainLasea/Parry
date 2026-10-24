package dev.abysslasea.aop.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import dev.abysslasea.aop.ParryNetwork;

final class ParryClientNetwork {
    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.SOUND, (client, handler, buffer, sender) -> {
            String id = buffer.readUtf();
            double x = buffer.readDouble(), y = buffer.readDouble(), z = buffer.readDouble();
            float pitch = buffer.readFloat();
            boolean success = buffer.readBoolean();
            client.execute(() -> { if (client.getConnection() == handler) ParrySoundPlayer.play(id, x, y, z, pitch, success); });
        });

        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.CHECK, (client, handler, buffer, sender) -> {
            int requestId = buffer.readVarInt();
            int targetId = buffer.readVarInt();
            client.execute(() -> {
                if (client.getConnection() != handler) return;
                var reply = PacketByteBufs.create();
                reply.writeVarInt(requestId);
                reply.writeBoolean(ParryView.canSee(targetId));
                ClientPlayNetworking.send(ParryNetwork.RESULT, reply);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.COOLDOWN, (client, handler, buffer, sender) -> {
            int remaining = buffer.readVarInt();
            int duration = buffer.readVarInt();
            client.execute(() -> ParryHud.sync(remaining, duration));
        });
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.ANIMATION, (client, handler, buffer, sender) -> {
            int id = buffer.readVarInt();
            client.execute(() -> ParryClient.animation(id));
        });
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.SUCCESS, (client, handler, buffer, sender) -> client.execute(ParryClient::success));
    }
    static void request() { ClientPlayNetworking.send(ParryNetwork.REQUEST, PacketByteBufs.empty()); }
}
