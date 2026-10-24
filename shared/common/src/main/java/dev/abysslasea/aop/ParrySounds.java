package dev.abysslasea.aop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ParrySounds {
    private ParrySounds() {}
    public static void play(ServerPlayer player, String sound, boolean success) {
        if (sound.isEmpty()) return;
        var id = ResourceLocation.tryParse(sound);
        if (id == null || !BuiltInRegistries.SOUND_EVENT.containsKey(id)) return;
        float pitch = 0.85F + player.serverLevel().random.nextFloat() * 0.3F;
        for (var observer : player.serverLevel().players()) {
            if (observer.distanceToSqr(player) <= 32 * 32) {
                ParryNetwork.sound(observer, id, player.getX(), player.getY(), player.getZ(), pitch, success);
            }
        }
    }
}
