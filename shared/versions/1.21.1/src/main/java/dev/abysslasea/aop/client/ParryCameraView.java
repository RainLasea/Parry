package dev.abysslasea.aop.client;

import net.minecraft.client.Camera;
import org.joml.Quaternionf;

/** 1.21.1 already stores the camera rotation in the screen projection's basis. */
final class ParryCameraView {
    private ParryCameraView() {}

    static Quaternionf rotation(Camera camera) {
        return camera.rotation();
    }
}
