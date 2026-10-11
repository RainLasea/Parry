package net.abysslasea.parry.mixin.client;

import mirsario.cameraoverhaul.CameraContext;
import mirsario.cameraoverhaul.utilities.Transform;
import net.abysslasea.parry.client.CameraOverhaulCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// CameraOverhaul is optional; allow its target class to be absent.
@Pseudo
@Mixin(targets = "mirsario.cameraoverhaul.CameraSystem", remap = false)
public abstract class CameraSystemMixin {
    @Inject(method = "onCameraUpdate", at = @At("RETURN"), remap = false)
    private void parry$advance(CameraContext context, double deltaTime, CallbackInfo callback) {
        CameraOverhaulCompat.advance(deltaTime);
    }

    @Inject(method = "modifyCameraTransform", at = @At("RETURN"), remap = false)
    private void parry$impact(Transform transform, CallbackInfo callback) {
        CameraOverhaulCompat.apply(transform);
    }
}
