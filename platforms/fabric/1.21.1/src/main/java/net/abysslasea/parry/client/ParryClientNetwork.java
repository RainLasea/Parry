package net.abysslasea.parry.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.abysslasea.parry.ParryNetwork;

final class ParryClientNetwork {
    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.Animation.TYPE, (payload, context) -> ParryClient.animation(payload.entityId()));
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.Success.TYPE, (payload, context) -> ParryClient.success());
    }
    static void request() { ClientPlayNetworking.send(ParryNetwork.Request.INSTANCE); }
}
