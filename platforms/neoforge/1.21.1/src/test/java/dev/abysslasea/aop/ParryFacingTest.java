package dev.abysslasea.aop;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParryFacingTest {
    private static final Vec3 EYES = new Vec3(10, 65.62, -20);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void ownedProjectileIsTheTargetRegardlessOfShooterPosition() {
        var shooter = new ArmorStand(EntityType.ARMOR_STAND, null);
        var arrow = new Arrow(EntityType.ARROW, null);
        shooter.setPos(EYES.add(0, 0, 5));
        arrow.setPos(EYES.add(0, 0, -1));
        var source = source(DamageTypes.ARROW, arrow, shooter);
        var target = ParryFacing.viewTarget(source);
        assertSame(arrow, target);

        shooter.setPos(EYES.add(0, 0, -5));
        arrow.setPos(EYES.add(0, 0, 1));
        assertSame(arrow, ParryFacing.viewTarget(source));
    }

    @Test
    void ownerlessProjectileStillRequiresFacingTheProjectile() {
        var arrow = new Arrow(EntityType.ARROW, null);
        assertSame(arrow, ParryFacing.viewTarget(source(DamageTypes.ARROW, arrow, null)));
    }

    @Test
    void meleeRequiresFacingTheLivingAttacker() {
        var attacker = new ArmorStand(EntityType.ARMOR_STAND, null);
        for (var type : List.of(DamageTypes.MOB_ATTACK, DamageTypes.MOB_ATTACK_NO_AGGRO,
                DamageTypes.PLAYER_ATTACK, DamageTypes.STING)) {
            assertSame(attacker, ParryFacing.viewTarget(source(type, attacker, attacker)));
        }
    }

    @Test
    void dragonBreathCloudDoesNotRequireFacingTheDragonOrCloud() {
        var owner = new ArmorStand(EntityType.ARMOR_STAND, null);
        var cloud = new AreaEffectCloud(EntityType.AREA_EFFECT_CLOUD, null);
        assertNull(ParryFacing.viewTarget(source(DamageTypes.INDIRECT_MAGIC, cloud, owner)));
        assertNull(ParryFacing.viewTarget(source(DamageTypes.DRAGON_BREATH, cloud, owner)));
    }

    @Test
    void specialAndEnvironmentalDamageHasNoViewRequirement() {
        var attacker = new ArmorStand(EntityType.ARMOR_STAND, null);
        for (var type : List.of(DamageTypes.SONIC_BOOM, DamageTypes.THORNS,
                DamageTypes.EXPLOSION)) {
            assertNull(ParryFacing.viewTarget(source(type, attacker, attacker)));
        }
        assertNull(ParryFacing.viewTarget(source(DamageTypes.ON_FIRE, null, null)));
    }

    private static DamageSource source(ResourceKey<DamageType> type, Entity direct, Entity causing) {
        return new DamageSource(Holder.direct(new DamageType("test", 0)), direct, causing) {
            @Override
            public boolean is(ResourceKey<DamageType> key) { return key.equals(type); }
        };
    }
}
