package dev.abysslasea.aop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ParryReflection {
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);
    private ParryReflection() {}
    public static boolean isReflecting() { return REFLECTING.get(); }

    public static void reflect(ServerPlayer player, DamageSource source, float amount, ParryProfile profile) {
        if (!profile.reflect() || isReflecting() || !Float.isFinite(amount) || amount <= 0) return;
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player || !attacker.isAlive()) return;
        float reflected = (float) Math.min(Float.MAX_VALUE, amount * profile.reflectMultiplier());
        if (reflected <= 0) return;
        REFLECTING.set(true);
        try {
            attacker.hurt(player.damageSources().playerAttack(player), reflected);
        } finally {
            REFLECTING.remove();
        }
    }
}
