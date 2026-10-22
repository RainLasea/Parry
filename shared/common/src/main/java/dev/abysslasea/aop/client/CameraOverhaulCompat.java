
package dev.abysslasea.aop.client;

import mirsario.cameraoverhaul.CameraOverhaul;
import mirsario.cameraoverhaul.configuration.Configuration;
import mirsario.cameraoverhaul.utilities.Transform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;

/** Load on the physical client after confirming CameraOverhaul is installed. */
public final class CameraOverhaulCompat {

    private static final ParryRecoil RECOIL = new ParryRecoil();

    private static LocalPlayer owner;
    private static ClientLevel ownerLevel;
    private static double armSign = 1.0;

    private static final double IMPACT_DURATION = 0.52;
    private static double impactTime = IMPACT_DURATION;

    private CameraOverhaulCompat() {}

    static void onSuccess() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || !minecraft.player.isAlive()
                || minecraft.player.isSpectator()
                || minecraft.getCameraEntity() != minecraft.player
                || !enabled()) {
            return;
        }

        owner = minecraft.player;
        ownerLevel = minecraft.level;

        armSign = owner.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;

        RECOIL.trigger(ParryCameraConfig.DURATION.get());

        impactTime = 0.0;

        if (CameraOverhaul.camera != null) {
            CameraOverhaul.camera.notifyOfPlayerAction();
        }
    }

    static void tick() {
        validateOwner();
    }

    /** Advances once per camera update; repeated transform reads must not advance time. */
    public static void advance(double deltaTime) {
        if (!validateOwner() || Minecraft.getInstance().isPaused()) {
            return;
        }

        if (!Double.isFinite(deltaTime)) {
            return;
        }

        // Limit a stalled frame's contribution so camera motion does not jump on resume.
        double dt = Math.max(0.0, Math.min(deltaTime, 0.05));

        RECOIL.advance(dt);

        impactTime = Math.min(
                IMPACT_DURATION,
                impactTime + dt
        );
    }

    public static void apply(Transform transform) {
        if (!validateOwner() || transform == null) {
            return;
        }

        var cameraType = Minecraft.getInstance()
                .options
                .getCameraType();

        double scale = ParryCameraConfig.INTENSITY.get()
                * (cameraType.isFirstPerson()
                    ? 1.0
                    : ParryCameraConfig.THIRD_PERSON_SCALE.get());

        double side = armSign
                * (cameraType.isMirrored() ? -1.0 : 1.0);

        double t = Math.min(
                1.0,
                impactTime / IMPACT_DURATION
        );

        double brace = pulse(t, 0.09, 0.075);
        double drive = pulse(t, 0.20, 0.11);
        double recovery = pulse(t, 0.46, 0.17);
        double settle = pulse(t, 0.72, 0.15);

        // Fade both ends because Gaussian pulses retain small values outside their peaks.
        double fadeIn = smoothstep(0.0, 0.055, t);

        double fadeOut = 1.0 - smoothstep(0.82, 1.0, t);

        double envelope = fadeIn * fadeOut;

        brace *= envelope;
        drive *= envelope;
        recovery *= envelope;
        settle *= envelope;

        double base = RECOIL.sample() * scale * 0.35;

        double pitch =
                -1.8 * brace
                -7.5 * drive
                +3.8 * recovery
                -0.7 * settle;

        double yaw =
                -1.5 * brace
                +5.5 * drive
                -3.0 * recovery
                +0.5 * settle;

        double roll =
                -2.0 * brace
                +10.0 * drive
                -5.0 * recovery
                +0.8 * settle;

        transform.eulerRot.x +=
                pitch * scale
                + (cameraType.isMirrored() ? 1.0 : -1.0) * base;

        transform.eulerRot.y +=
                side * (yaw * scale + 0.3 * base);

        transform.eulerRot.z +=
                side * (roll * scale + 1.2 * base);
    }

    private static double pulse(
            double t,
            double center,
            double width
    ) {
        double x = (t - center) / width;
        return Math.exp(-x * x);
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        double x = Math.max(
                0.0,
                Math.min(
                        1.0,
                        (value - edge0) / (edge1 - edge0)
                )
        );

        return x * x * (3.0 - 2.0 * x);
    }

    private static boolean enabled() {
        return ParryCameraConfig.ENABLED.get()
                && ParryCameraConfig.INTENSITY.get() > 0
                && Configuration.get().general.enabled;
    }

    private static boolean validateOwner() {
        Minecraft minecraft = Minecraft.getInstance();

        if (owner == null) {
            return false;
        }

        if (minecraft.player != owner
                || minecraft.level != ownerLevel
                || !owner.isAlive()
                || owner.isSpectator()
                || minecraft.getCameraEntity() != owner
                || !enabled()) {

            // Clear ownership with the pose so it cannot carry into another player or world.
            RECOIL.reset();

            impactTime = IMPACT_DURATION;

            owner = null;
            ownerLevel = null;

            return false;
        }

        return true;
    }
}
