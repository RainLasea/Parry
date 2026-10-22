package dev.abysslasea.aop;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/** Selects what the client must see; performs no server-side view checks. */
public final class ParryFacing {
    private ParryFacing() {}

    /** Null means this attack has no view requirement; other parry rules still apply. */
    static Entity viewTarget(DamageSource source) {
        var direct = source.getDirectEntity();
        // Follow the incoming projectile, regardless of where its shooter is looking or standing.
        if (direct instanceof Projectile) return direct;

        // Only melee attacks require facing a living attacker. Entity attribution alone also
        // occurs for special attacks such as dragon breath, sonic booms, thorns and explosions.
        boolean melee = source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
                || source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.STING);
        if (melee && direct instanceof LivingEntity) return direct;
        return null;
    }
}
