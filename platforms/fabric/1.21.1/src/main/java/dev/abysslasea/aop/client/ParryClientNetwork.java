package dev.abysslasea.aop.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import dev.abysslasea.aop.ParryNetwork;

final class ParryClientNetwork {
    static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.Sound.TYPE, (payload, context) ->
                ParrySoundPlayer.play(payload.sound(), payload.x(), payload.y(), payload.z(), payload.pitch(), payload.success()));

        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.Check.TYPE, (payload, context) ->
                ClientPlayNetworking.send(new ParryNetwork.Result(payload.requestId(), ParryView.canSee(payload.targetId()))));
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.Cooldown.TYPE,
                (payload, context) -> ParryHud.sync(payload.remainingTicks(), payload.durationTicks()));
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.Animation.TYPE, (payload, context) -> ParryClient.animation(payload.entityId()));
        ClientPlayNetworking.registerGlobalReceiver(ParryNetwork.Success.TYPE, (payload, context) -> ParryClient.success());
    }
    static void request() { ClientPlayNetworking.send(ParryNetwork.Request.INSTANCE); }
}
