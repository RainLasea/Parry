package net.abysslasea.parry;

import net.abysslasea.parry.api.ParryApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
    public static void reset(ServerPlayer player) { STATES.remove(player); }
    public static void copy(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        ParryState old = STATES.remove(oldPlayer);
        if (alive && old != null) STATES.put(newPlayer, old.copy());
        else STATES.remove(newPlayer);
    }
    public static void clear() { STATES.clear(); }
    public static void tick() { STATES.values().forEach(ParryState::tick); }

    public static void activate(ServerPlayer player) {
        if (ParryMod.isFakePlayer(player) || !player.isAlive() || player.isSpectator()) return;
        var modifiers = resolveAccessories(player).modifiers();
        if (!modifiers.isEnabled()) return;
        var state = state(player);
        long tick = player.serverLevel().getGameTime();
        if (!state.activate(modifiers.windowTicks(), modifiers.cooldownTicks(), tick)) return;

        // Observers retain the vanilla swing fallback. The activating client's custom packet
        // handles its animation; echoing a swing there makes Punchy queue an ordinary attack.
        player.swing(InteractionHand.MAIN_HAND, false);
        player.serverLevel().playSound(null, player.blockPosition(), ParryMod.BLOCK_SOUND.get(),
                SoundSource.MASTER, 2.0F, 0.85F + player.serverLevel().random.nextFloat() * 0.3F);
        ParryNetwork.animation(player);
    }

    public static boolean incomingDamage(ServerPlayer player, net.minecraft.world.damagesource.DamageSource source) {
        if (ParryMod.isFakePlayer(player) || !player.isAlive() || player.isSpectator()) return false;
        // Void damage must remain lethal even while a parry window is open.
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return false;

        var state = state(player);
        if (!state.isActive()) return false;
        var accessories = resolveAccessories(player);
        var modifiers = accessories.modifiers();
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
        level.playSound(null, player.blockPosition(), ParryMod.PARRY_SOUND.get(),
                SoundSource.MASTER, 1.0F, 0.85F + level.random.nextFloat() * 0.3F);
        var attacker = source.getEntity();
        if (attacker == null) attacker = source.getDirectEntity();
        double knockback = modifiers.knockbackStrength();
        if (attacker != null && attacker != player && knockback > 0) {
            attacker.setDeltaMovement(look.x * knockback, 0, look.z * knockback);
            attacker.hurtMarked = true;
        }
        ParryNetwork.success(player);
        accessories.onSuccess(source);
        return true;
    }

    private static ParryApi.Resolved resolveAccessories(ServerPlayer player) {
        return ParryApi.resolve(player, ParryConfig.PARRY_COOLDOWN.get().intValue(),
                ParryConfig.PARRY_WINDOW.get().intValue(), ParryConfig.REFUND_PARRY_COOLDOWN.get());
    }

}
