package dev.abysslasea.aop;

import dev.abysslasea.aop.api.ParryApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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
import net.neoforged.neoforge.network.PacketDistributor;

public final class ParryEvents {
    private ParryEvents() {}

    public static void activate(ServerPlayer player) {
        if (player instanceof FakePlayer || !player.isAlive() || player.isSpectator()) return;
        if (!canParry(player)) return;
        var modifiers = resolveAccessories(player).modifiers();
        if (!modifiers.isEnabled()) return;
        var state = player.getData(ParryMod.PARRY_STATE);
        long tick = player.serverLevel().getGameTime();
        if (!state.activate(modifiers.windowTicks(), modifiers.cooldownTicks(), tick)) return;

        // Observers retain the vanilla swing fallback. The activating client's custom packet
        // handles its animation; echoing a swing there makes Punchy queue an ordinary attack.
        player.swing(InteractionHand.MAIN_HAND, false);
        player.serverLevel().playSound(null, player.blockPosition(), ParryMod.BLOCK_SOUND.get(),
                SoundSource.PLAYERS, 1.0F,
                0.85F + player.serverLevel().random.nextFloat() * 0.3F);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ParryNetwork.Animation(player.getId()));
    }

    @SubscribeEvent
    public static void incomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        // Void damage must remain lethal even while a parry window is open.
        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) return;
        // Recheck permissions so a class change cannot preserve an old window's access.
        if (!canParry(player)) return;

        var state = player.getData(ParryMod.PARRY_STATE);
        if (!state.isActive()) return;
        var accessories = resolveAccessories(player);
        var modifiers = accessories.modifiers();
        if (!modifiers.canBlock(event.getSource())) return;
        if (!state.parry(modifiers.refundCooldown())) return;

        event.setCanceled(true);
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
                SoundSource.PLAYERS, 1.0F,
                0.85F + level.random.nextFloat() * 0.3F);
        var attacker = event.getSource().getEntity();
        if (attacker == null) attacker = event.getSource().getDirectEntity();
        double knockback = modifiers.knockbackStrength();
        if (attacker != null && attacker != player && knockback > 0) {
            attacker.setDeltaMovement(look.x * knockback, 0, look.z * knockback);
            attacker.hurtMarked = true;
        }
        PacketDistributor.sendToPlayer(player, ParryNetwork.Success.INSTANCE);
        accessories.onSuccess(event.getSource());
    }

    private static ParryApi.Resolved resolveAccessories(ServerPlayer player) {
        return ParryApi.resolve(player, ParryConfig.PARRY_COOLDOWN.get().intValue(),
                ParryConfig.PARRY_WINDOW.get().intValue(), ParryConfig.REFUND_PARRY_COOLDOWN.get());
    }

    private static boolean canParry(ServerPlayer player) {
        return !ModList.get().isLoaded("neoorigins")
                || dev.abysslasea.aop.compat.NeoOriginsCompat.canParry(player);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        for (var player : event.getServer().getPlayerList().getPlayers()) {
            if (player.hasData(ParryMod.PARRY_STATE)) {
                player.getData(ParryMod.PARRY_STATE).tick();
            }
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        event.getEntity().setData(ParryMod.PARRY_STATE, new ParryState());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        event.getEntity().setData(ParryMod.PARRY_STATE, new ParryState());
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {

        // Returning from the End clones the player without death and should preserve cooldown.
        if (!event.isWasDeath() && event.getOriginal().hasData(ParryMod.PARRY_STATE)) {
            event.getEntity().setData(ParryMod.PARRY_STATE,
                    event.getOriginal().getData(ParryMod.PARRY_STATE).copy());
        }
    }
}
