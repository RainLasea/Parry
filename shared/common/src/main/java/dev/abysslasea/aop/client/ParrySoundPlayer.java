package dev.abysslasea.aop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class ParrySoundPlayer {
    private ParrySoundPlayer() {}
    public static void play(String sound, double x, double y, double z, float pitch, boolean success) {
        var id = ResourceLocation.tryParse(sound);
        if (id == null || !BuiltInRegistries.SOUND_EVENT.containsKey(id)) return;
        var event = BuiltInRegistries.SOUND_EVENT.get(id);
        float volume = (success ? ParryCameraConfig.PARRY_SOUND_VOLUME.get() : ParryCameraConfig.BLOCK_SOUND_VOLUME.get()).floatValue();
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(event, SoundSource.PLAYERS,
                volume, pitch, RandomSource.create(), x, y, z));
    }
}
