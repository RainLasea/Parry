package dev.abysslasea.aop.mixin.client;

import dev.abysslasea.aop.ParryMod;
import dev.abysslasea.aop.client.ParryCameraConfig;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSoundInstance.class)
public abstract class AbstractSoundInstanceMixin {
    @Shadow @Final protected ResourceLocation location;

    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void parry$volume(CallbackInfoReturnable<Float> callback) {
        if (!ParryMod.MODID.equals(location.getNamespace())) return;
        double volume = switch (location.getPath()) {
            case "block" -> ParryCameraConfig.BLOCK_SOUND_VOLUME.get();
            case "parry_sound" -> ParryCameraConfig.PARRY_SOUND_VOLUME.get();
            default -> 1.0;
        };
        callback.setReturnValue(callback.getReturnValueF() * (float) volume);
    }
}
