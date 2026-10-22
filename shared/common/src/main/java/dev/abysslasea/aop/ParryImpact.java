
package dev.abysslasea.aop;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;

public final class ParryImpact {
    public static final int SPARK_COUNT = 24;

    private ParryImpact() {}

    private static Vec3 rightVector(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
    }

    public static Vec3 contactPoint(
            Vec3 eyes,
            Vec3 look,
            float yawDegrees,
            boolean rightArm
    ) {
        Vec3 forward = look.normalize();
        Vec3 right = rightVector(yawDegrees);

        // A fixed up axis keeps the offset stable when the player looks straight up or down.
        Vec3 up = new Vec3(0, 1, 0);

        return eyes
                .add(forward.scale(1.10))
                .add(right.scale(rightArm ? 0.18 : -0.18))
                .add(up.scale(-0.25));
    }

    public static Vec3 sparkVelocity(
            Vec3 look,
            float yawDegrees,
            int index,
            RandomSource random
    ) {
        Vec3 forward = look.normalize();
        Vec3 right = rightVector(yawDegrees);
        Vec3 up = new Vec3(0, 1, 0);

        double angle = Math.PI * (
                0.15 + 0.70 *
                ((index + random.nextDouble() * 0.6) / SPARK_COUNT)
        );

        double side = Math.cos(angle) * 0.85;
        double vertical = Math.sin(angle) * 0.75 - 0.15;

        double speed = index % 6 == 0
                ? 0.32 + random.nextDouble() * 0.10
                : 0.15 + random.nextDouble() * 0.12;

        return forward.scale(0.15)
                .add(right.scale(side))
                .add(up.scale(vertical))
                .normalize()
                .scale(speed);
    }
}
