package dev.abysslasea.aop.client;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParryViewTest {
    private static final Vec3 EYES = new Vec3(10, 65, -20);
    // Minecraft's yaw 0 camera looks along positive Z.
    private static final Quaternionf FORWARD = new Quaternionf().rotationY((float) Math.PI);

    @Test
    void screenFrustumRejectsRearAndSideTargets() {
        assertTrue(visible(70, 16F / 9F, FORWARD, new Vec3(0, 0, 3)));
        assertFalse(visible(70, 16F / 9F, FORWARD, new Vec3(0, 0, -3)));
        assertFalse(visible(70, 16F / 9F, FORWARD, new Vec3(3, 0, 0)));
    }

    @Test
    void fieldOfViewAndScreenAspectChangeVisibility() {
        var nearEdge = new Vec3(3, 0, 3);
        assertFalse(visible(70, 1, FORWARD, nearEdge));
        assertTrue(visible(110, 1, FORWARD, nearEdge));
        assertTrue(visible(70, 16F / 9F, FORWARD, nearEdge));
    }

    @Test
    void lookingUpChangesWhichTargetsFitOnScreen() {
        var above = new Vec3(0, 3, 1);
        assertFalse(visible(70, 16F / 9F, FORWARD, above));
        var up = new Quaternionf().rotationYXZ((float) Math.PI, (float) Math.toRadians(70), 0);
        assertTrue(visible(70, 16F / 9F, up, above));
        assertFalse(visible(70, 16F / 9F, up, new Vec3(0, -3, 1)));
    }

    @Test
    void checksTheWholeEntityBoundsAtTheScreenEdge() {
        var projection = projection(90, 1);
        var crossingEdge = new AABB(EYES.x + 2.8, EYES.y - 0.2, EYES.z + 3,
                EYES.x + 3.4, EYES.y + 0.2, EYES.z + 3.2);
        assertTrue(ParryView.isInView(FORWARD, EYES, projection, crossingEdge));
        assertFalse(ParryView.isInView(FORWARD, EYES, projection, crossingEdge.move(1, 0, 0)));
    }

    private static boolean visible(float fov, float aspect, Quaternionf rotation, Vec3 offset) {
        Vec3 position = EYES.add(offset);
        var bounds = new AABB(position.x - 0.05, position.y - 0.05, position.z - 0.05,
                position.x + 0.05, position.y + 0.05, position.z + 0.05);
        return ParryView.isInView(rotation, EYES, projection(fov, aspect), bounds);
    }

    private static Matrix4f projection(float fov, float aspect) {
        return new Matrix4f().perspective((float) Math.toRadians(fov), aspect, 0.05F, 256);
    }
}
