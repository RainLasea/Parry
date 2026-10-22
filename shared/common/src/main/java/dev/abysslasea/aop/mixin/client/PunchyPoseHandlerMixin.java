package dev.abysslasea.aop.mixin.client;

import dev.abysslasea.aop.client.PunchyCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import punchy.client.animation.PoseHandler;

/** These fields and tick(float,float) exist in both Punchy 2.4 and 2.8e. */
@Pseudo
@Mixin(targets = "punchy.client.animation.PoseHandler", remap = false)
public abstract class PunchyPoseHandlerMixin {
    @Shadow private float currentTime;
    @Shadow private float expressionTime;
    @Shadow private boolean paused;
    @Shadow private boolean frozen;

    @Inject(method = "tick(FF)V", at = @At("RETURN"), remap = false)
    private void parry$successJump(float deltaSeconds, float globalSpeed, CallbackInfo callback) {
        if (paused || frozen) return;
        float correctedTime = PunchyCompat.advanceSuccess((PoseHandler) (Object) this, deltaSeconds, currentTime);
        if (correctedTime == currentTime) return;
        expressionTime += correctedTime - currentTime;
        currentTime = correctedTime;
    }
}
