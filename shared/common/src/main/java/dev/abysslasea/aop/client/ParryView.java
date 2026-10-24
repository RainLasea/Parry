package dev.abysslasea.aop.client;

import dev.abysslasea.aop.mixin.client.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Uses the local camera, its current FOV and the target's bounds. Client thread only. */
public final class ParryView {
    private ParryView() {}

    public static boolean canSee(int entityId) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !client.player.isAlive()) return false;
        var target = client.level.getEntity(entityId);
        if (target == null || target == client.player || target.isRemoved()) return false;

        var renderer = client.gameRenderer;
        var camera = renderer.getMainCamera();
        double fov = ((GameRendererAccessor) renderer).parry$getFov(camera, 1.0F, true);
        var eyes = camera.getPosition();
        if (!isInView(ParryCameraView.rotation(camera), eyes, renderer.getProjectionMatrix(fov), target.getBoundingBox())) return false;

        var obstruction = client.level.clip(new ClipContext(eyes, target.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
        return obstruction.getType() == HitResult.Type.MISS;
    }

    static boolean isInView(Quaternionf rotation, Vec3 eyes, Matrix4f projection, AABB bounds) {
        var view = new Matrix4f().rotation(rotation.conjugate(new Quaternionf()));
        var frustum = new Frustum(view, projection);
        frustum.prepare(eyes.x, eyes.y, eyes.z);
        return frustum.isVisible(bounds);
    }
}
