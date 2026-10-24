package dev.abysslasea.aop;

import dev.abysslasea.aop.api.ParryApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ParryEvents {
    private ParryEvents() {}
    @SubscribeEvent public static void reload(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new ParryItemRules.ReloadListener());
    }

    public static void activate(ServerPlayer player) {
        if (player instanceof FakePlayer || !player.isAlive() || player.isSpectator()) return;
        if (!canParry(player)) return;
        var activation = ParryItemRules.select(player);
        if (activation == null) return;
        var modifiers = resolveAccessories(player, activation).modifiers();
        if (!modifiers.isEnabled()) return;
        var state = player.getData(ParryMod.PARRY_STATE);
        long tick = player.serverLevel().getGameTime();
        if (!state.activate(modifiers.windowTicks(), modifiers.cooldownTicks(), tick)) return;

        state.activation(activation);

        // Observers retain the vanilla swing fallback. The activating client's custom packet
        // handles its animation; echoing a swing there makes Punchy queue an ordinary attack.
        player.swing(activation.hand(), false);
        ParrySounds.play(player, activation.profile().activationSound(), false);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ParryNetwork.Animation(player.getId()));
    }

    @SubscribeEvent
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        if (ParryPendingDamage.isReplaying(player) || ParryReflection.isReflecting()) return;
        // Void damage must remain lethal even while a parry window is open.
        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) return;
        if (ParryPendingDamage.queue(player, event.getSource(), event.getOriginalAmount())) {
            event.setCanceled(true);
            return;
        }
        // Recheck permissions so a class change cannot preserve an old window's access.
        if (!canParry(player)) return;

        var state = player.getData(ParryMod.PARRY_STATE);
        if (!state.isActive()) return;
        var activation = state.activation();
        if (activation == null || !activation.stillHeld(player)) return;
        var modifiers = resolveAccessories(player, activation).modifiers();
        if (!modifiers.canBlock(event.getSource())) return;
        var target = ParryFacing.viewTarget(event.getSource());
        if (target == null) {
            if (completeParry(player, state, event.getSource(), event.getOriginalAmount())) event.setCanceled(true);
            return;
        }
        if (ParryPendingDamage.request(player, state, event.getSource(), event.getOriginalAmount(),
                target,
                () -> player.getData(ParryMod.PARRY_STATE) == state
                        && completeParry(player, state, event.getSource(), event.getOriginalAmount()))) {
            event.setCanceled(true);
        }
    }

    private static boolean completeParry(ServerPlayer player, ParryState state,
                                         net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!canParry(player)) return false;
        var activation = state.activation();
        if (activation == null || !activation.stillHeld(player)) return false;
        var accessories = resolveAccessories(player, activation);
        var modifiers = accessories.modifiers();
        if (!modifiers.isEnabled() || !modifiers.canBlock(source)) return false;
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
        PacketDistributor.sendToPlayer(player, ParryNetwork.Success.INSTANCE);
        ParryReflection.reflect(player, source, amount, activation.profile());
        accessories.onSuccess(source);
        return true;
    }

    private static ParryApi.Resolved resolveAccessories(ServerPlayer player, ParryItemRules.Activation activation) {
        var profile = activation.profile();
        return ParryApi.resolve(player, activation.cooldown(), profile.window(), profile.refund());
    }

    private static boolean canParry(ServerPlayer player) {
        return !ModList.get().isLoaded("neoorigins")
                || dev.abysslasea.aop.compat.NeoOriginsCompat.canParry(player);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        ParryPendingDamage.tick();
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            if (player.hasData(ParryMod.PARRY_STATE)) {
                var state = player.getData(ParryMod.PARRY_STATE);
                state.tick();
                if (state.consumeCooldownUpdate()) {
                    PacketDistributor.sendToPlayer(player,
                            new ParryNetwork.Cooldown(state.cooldownTicks(), state.cooldownDuration()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ParryPendingDamage.discard(player);
        event.getEntity().setData(ParryMod.PARRY_STATE, new ParryState());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ParryPendingDamage.discard(player);
        event.getEntity().setData(ParryMod.PARRY_STATE, new ParryState());
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) ParryPendingDamage.discard(player);

        // Returning from the End clones the player without death and should preserve cooldown.
        if (!event.isWasDeath() && event.getOriginal().hasData(ParryMod.PARRY_STATE)) {
            event.getEntity().setData(ParryMod.PARRY_STATE,
                    event.getOriginal().getData(ParryMod.PARRY_STATE).copy());
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ParryPendingDamage.discard(player);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { ParryPendingDamage.clear(); ParryItemRules.clear(); }
}
