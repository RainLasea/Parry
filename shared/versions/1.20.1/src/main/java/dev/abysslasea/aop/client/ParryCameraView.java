package dev.abysslasea.aop.client;

import net.minecraft.client.Camera;
import org.joml.Quaternionf;

/** Converts 1.20.1 camera angles to the rotation used by the screen projection. */
final class ParryCameraView {
    private ParryCameraView() {}

    static Quaternionf rotation(Camera camera) {
        // 1.20.1 renders with pitch followed by yaw + 180 degrees. Its camera
        // quaternion uses a positive-Z basis instead of the projection's negative Z.
        // Read the final angles so Forge's camera-angle adjustments are respected too.
        return rotation(camera.getYRot(), camera.getXRot());
    }

    static Quaternionf rotation(float yaw, float pitch) {
        return new Quaternionf().rotationYXZ((float) Math.toRadians(180.0 - yaw),
                (float) Math.toRadians(-pitch), 0);
    }
}
