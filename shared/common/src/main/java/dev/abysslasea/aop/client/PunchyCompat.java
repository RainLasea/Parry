package dev.abysslasea.aop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import punchy.client.animation.PunchyAnimationManager;
import punchy.client.animation.PoseHandler;
import punchy.client.animation.data.AnimationClip;
import punchy.config.PunchyConfig;

/** Load only on the client, after confirming that Punchy is installed. */
public final class PunchyCompat {
    private static final SwordParryTiming.SuccessJump SUCCESS_JUMP = new SwordParryTiming.SuccessJump();
    private static AbstractClientPlayer owner;
    private static ClientLevel ownerLevel;
    private static Item heldItem;
    private static AnimationClip swordClip;
    private static float playbackSpeed;
    private static boolean successHandled;

    private PunchyCompat() {}

    static void play(AbstractClientPlayer player) {
        var minecraft = Minecraft.getInstance();
        // Punchy's pose handler is global and only controls the local first-person arms.
        if (player != minecraft.player || minecraft.getCameraEntity() != player
                || !PunchyConfig.isModEnabled()) return;
        reset();
        if (PunchyAnimationManager.isAnyHandBlacklisted()) return;

        // Resolved anew for every accepted parry so F3+T/resource-pack overrides work.
        boolean holdingItem = !player.getMainHandItem().isEmpty();
        String clip = !holdingItem
                ? "parry" : (player.getRandom().nextBoolean() ? "sword_parry_1" : "sword_parry_2");
        var animation = PunchyAnimationManager.resolveCustomClip(clip);
        if (animation == null) return;
        // dual_handed is declared in the asset; older Punchy versions have no copyOf API.

        var handler = PunchyAnimationManager.POSE_HANDLER;
        PunchyAnimationManager.setSourceHand(minecraft, InteractionHand.MAIN_HAND);
        handler.setActiveArm(player.getMainArm());
        handler.setIgnoreDualHanded(false);
        handler.resetLoopSpeedParams();
        // Restart on each server-confirmed activation, including immediately refunded parries.
        float speed = PunchyAnimationManager.clipSpeedMultiplier(animation);
        if (holdingItem) speed *= SwordParryTiming.speedMultiplier(player.getAttributeValue(Attributes.ATTACK_SPEED));
        handler.play(animation, speed);
        if (holdingItem) {
            owner = player;
            ownerLevel = minecraft.level;
            heldItem = player.getMainHandItem().getItem();
            swordClip = animation;
            playbackSpeed = speed;
        }
    }

    static void onSuccess() {
        if (!validateOwner() || successHandled) return;
        successHandled = true;
        SUCCESS_JUMP.begin(PunchyAnimationManager.POSE_HANDLER.getCurrentTime(), swordClip.getLength(),
                playbackSpeed * PunchyConfig.getAnimationSpeed());
    }

    static void tick() { validateOwner(); }

    /** Called once after Punchy's own tick, preserving its previous frame for interpolation. */
    public static float advanceSuccess(PoseHandler handler, float deltaSeconds, float nativeTime) {
        if (handler != PunchyAnimationManager.POSE_HANDLER || !validateOwner()
                || !SUCCESS_JUMP.isActive() || !Float.isFinite(nativeTime)) return nativeTime;
        // A delayed packet, changed global speed or another update can never rewind the pose.
        return (float) Math.min(swordClip.getLength(), Math.max(nativeTime, SUCCESS_JUMP.advance(deltaSeconds)));
    }

    private static boolean validateOwner() {
        if (owner == null) return false;
        var minecraft = Minecraft.getInstance();
        var handler = PunchyAnimationManager.POSE_HANDLER;
        if (minecraft.player != owner || minecraft.level != ownerLevel
                || minecraft.getCameraEntity() != owner || !owner.isAlive() || owner.isSpectator()
                || !PunchyConfig.isModEnabled() || PunchyAnimationManager.isAnyHandBlacklisted()
                || owner.getMainHandItem().isEmpty() || owner.getMainHandItem().getItem() != heldItem
                || handler.getCurrentClip() != swordClip || !handler.isPlaying()) {
            reset();
            return false;
        }
        return true;
    }

    private static void reset() {
        SUCCESS_JUMP.reset();
        owner = null;
        ownerLevel = null;
        heldItem = null;
        swordClip = null;
        playbackSpeed = 1;
        successHandled = false;
    }
}
