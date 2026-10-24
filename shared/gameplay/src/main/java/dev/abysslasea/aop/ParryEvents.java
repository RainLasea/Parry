package dev.abysslasea.aop;

import dev.abysslasea.aop.api.ParryApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

/** Session-only state is scoped to the actual player, never a persistent UUID. */
public final class ParryEvents {
    private static final Map<ServerPlayer, ParryState> STATES = new WeakHashMap<>();
    private ParryEvents() {}
    public static ParryState state(ServerPlayer player) { return STATES.computeIfAbsent(player, ignored -> new ParryState()); }
    public static void reset(ServerPlayer player) { ParryPendingDamage.discard(player); STATES.remove(player); }
    public static void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        ParryPendingDamage.discard(oldPlayer);
        ParryState old = STATES.remove(oldPlayer);
        if (alive && old != null) STATES.put(newPlayer, old.copy());
        else STATES.remove(newPlayer);
    }
    public static void clear() { ParryPendingDamage.clear(); STATES.clear(); }
    public static void tick() {
        ParryPendingDamage.tick();
        STATES.forEach((player, state) -> {
            state.tick();
            if (state.consumeCooldownUpdate()) {
                ParryNetwork.cooldown(player, state.cooldownTicks(), state.cooldownDuration());
            }
        });
    }

    public static void activate(ServerPlayer player) {
        if (ParryMod.isFakePlayer(player) || !player.isAlive() || player.isSpectator()) return;
        var activation = ParryItemRules.select(player);
        if (activation == null) return;
        var modifiers = resolveAccessories(player, activation).modifiers();
        if (!modifiers.isEnabled()) return;
        var state = state(player);
        long tick = player.serverLevel().getGameTime();
        if (!state.activate(modifiers.windowTicks(), modifiers.cooldownTicks(), tick)) return;

        state.activation(activation);

        // Observers retain the vanilla swing fallback. The activating client's custom packet
        // handles its animation; echoing a swing there makes Punchy queue an ordinary attack.
        player.swing(activation.hand(), false);
        ParrySounds.play(player, activation.profile().activationSound(), false);
        ParryNetwork.animation(player);
    }

    public static boolean incomingDamage(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (ParryMod.isFakePlayer(player) || !player.isAlive() || player.isSpectator()) return false;
        if (ParryPendingDamage.isReplaying(player) || ParryReflection.isReflecting()) return false;
        // Void damage must remain lethal even while a parry window is open.
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return false;
        if (ParryPendingDamage.queue(player, source, amount)) return true;

        var state = state(player);
        if (!state.isActive()) return false;
        var activation = state.activation();
        if (activation == null || !activation.stillHeld(player)) return false;
        var modifiers = resolveAccessories(player, activation).modifiers();
        if (!modifiers.canBlock(source)) return false;
        var target = ParryFacing.viewTarget(source);
        // Special attacks have no view check and need no client round trip.
        if (target == null) return completeParry(player, state, source, amount);
        return ParryPendingDamage.request(player, state, source, amount, target,
                () -> state(player) == state && completeParry(player, state, source, amount));
    }

    private static boolean completeParry(ServerPlayer player, ParryState state,
                                         net.minecraft.world.damagesource.DamageSource source, float amount) {
        var activation = state.activation();
        if (activation == null || !activation.stillHeld(player)) return false;
        var accessories = resolveAccessories(player, activation);
        var modifiers = accessories.modifiers();
        if (!modifiers.isEnabled()) return false;
        if (!modifiers.canBlock(source)) return false;
        if (!state.parry(modifiers.refundCooldown())) return false;

        var level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 contact = ParryImpact.contactPoint(player.getEyePosition(), look, player.getYRot(),
                player.getMainArm() == HumanoidArm.RIGHT);
        var obstruction = level.clip(new ClipContext(player.getEyePosition(), contact,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (obstruction.getType() == HitResult.Type.BLOCK) {
            // Leave a small gap so the contact flash stays on the visible side of the wall.
            contact = obstruction.getLocation().subtract(contact.subtract(player.getEyePosition()).normalize().scale(0.04));
        }
        for (int spark = 0; spark < ParryImpact.SPARK_COUNT; spark++) {
            Vec3 velocity = ParryImpact.sparkVelocity(look, player.getYRot(), spark, level.random);

            // A zero particle count makes packet offsets carry velocity instead of random spread.
            level.sendParticles(ParryMod.IRON_SPARK.get(), contact.x, contact.y, contact.z,
                    0, velocity.x, velocity.y, velocity.z, 1.0);
        }
        level.sendParticles(ParryMod.SPARK_FLASH.get(), contact.x, contact.y, contact.z,
                1, 0, 0, 0, 0);
        ParrySounds.play(player, activation.profile().successSound(), true);
        var attacker = source.getEntity();
        if (attacker == null) attacker = source.getDirectEntity();
        double knockback = modifiers.knockbackStrength();
        if (attacker != null && attacker != player && knockback > 0) {
            attacker.setDeltaMovement(look.x * knockback, 0, look.z * knockback);
            attacker.hurtMarked = true;
        }
        ParryNetwork.success(player);
        ParryReflection.reflect(player, source, amount, activation.profile());
        accessories.onSuccess(source);
        return true;
    }

    private static ParryApi.Resolved resolveAccessories(ServerPlayer player, ParryItemRules.Activation activation) {
        var profile = activation.profile();
        return ParryApi.resolve(player, activation.cooldown(), profile.window(), profile.refund());
    }

}
