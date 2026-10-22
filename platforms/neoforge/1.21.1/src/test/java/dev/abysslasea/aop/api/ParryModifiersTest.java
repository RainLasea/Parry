package dev.abysslasea.aop.api;

import dev.abysslasea.aop.ParryState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParryModifiersTest {
    private static DamageSource source(ResourceKey<DamageType> type) {
        // A keyed holder normally comes from a running world's registry. These tests need only keys.
        return new DamageSource(Holder.direct(new DamageType("test", 0))) {
            @Override
            public boolean is(ResourceKey<DamageType> key) { return key.equals(type); }
        };
    }

    @Test
    void preservesConfiguredDefaultsWithoutAccessories() {
        var modifiers = new ParryModifiers(60, 10, false);
        assertEquals(60, modifiers.cooldownTicks());
        assertEquals(10, modifiers.windowTicks());
        assertEquals(1.2, modifiers.knockbackStrength());
        assertTrue(modifiers.isEnabled());
        assertFalse(modifiers.refundCooldown());
        assertTrue(modifiers.canBlock(source(DamageTypes.ON_FIRE)));
    }

    @Test
    void combinesBonusesBeforeMultipliersRegardlessOfRegistrationOrder() {
        var first = new ParryModifiers(60, 10, true);
        first.multiplyCooldown(0.8);
        first.addCooldownTicks(-20);
        first.multiplyCooldown(0.5);
        first.addWindowTicks(4);
        first.multiplyWindow(1.5);
        var second = new ParryModifiers(60, 10, true);
        second.addCooldownTicks(-20);
        second.multiplyCooldown(0.5);
        second.multiplyCooldown(0.8);
        second.multiplyWindow(1.5);
        second.addWindowTicks(4);
        assertEquals(16, first.cooldownTicks());
        assertEquals(first.cooldownTicks(), second.cooldownTicks());
        assertEquals(21, first.windowTicks());
        assertEquals(first.windowTicks(), second.windowTicks());
    }

    @Test
    void clampsTimersAndRejectsInvalidModifiers() {
        var modifiers = new ParryModifiers(60, 10, true);
        modifiers.addCooldownTicks(-100);
        modifiers.multiplyWindow(0);
        assertEquals(0, modifiers.cooldownTicks());
        assertEquals(1, modifiers.windowTicks());
        modifiers.addCooldownTicks(1_000_000);
        assertEquals(72000, modifiers.cooldownTicks());
        assertThrows(IllegalArgumentException.class, () -> modifiers.multiplyCooldown(-1));
        assertThrows(IllegalArgumentException.class, () -> modifiers.addWindowTicks(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> modifiers.multiplyKnockback(Double.POSITIVE_INFINITY));
    }

    @Test
    void whitelistAndHigherPriorityExceptionsResolvePerDamageType() {
        var modifiers = new ParryModifiers(60, 10, true);
        modifiers.setDamageRule(damage -> damage.is(DamageTypes.ARROW), 10);
        assertTrue(modifiers.canBlock(source(DamageTypes.ARROW)));
        assertFalse(modifiers.canBlock(source(DamageTypes.ON_FIRE)));
        modifiers.allowDamage(damage -> damage.is(DamageTypes.ON_FIRE), 20);
        assertTrue(modifiers.canBlock(source(DamageTypes.ON_FIRE)));
        assertFalse(modifiers.canBlock(source(DamageTypes.FALL)));
    }

    @Test
    void denialWinsEqualPriorityRegardlessOfOrder() {
        for (boolean allowFirst : new boolean[] {true, false}) {
            var modifiers = new ParryModifiers(60, 10, true);
            if (allowFirst) modifiers.allowDamage(damage -> true, 10);
            modifiers.denyDamage(damage -> damage.is(DamageTypes.ARROW), 10);
            if (!allowFirst) modifiers.allowDamage(damage -> true, 10);
            assertFalse(modifiers.canBlock(source(DamageTypes.ARROW)));
            assertTrue(modifiers.canBlock(source(DamageTypes.FALL)));
        }
    }

    @Test
    void voidDamageCannotBeAllowedEvenAtMaximumPriority() {
        var modifiers = new ParryModifiers(60, 10, true);
        modifiers.allowDamage(damage -> true, Integer.MAX_VALUE);
        assertFalse(modifiers.canBlock(source(DamageTypes.FELL_OUT_OF_WORLD)));
    }

    @Test
    void switchesUsePriorityAndFalseWinsTies() {
        var modifiers = new ParryModifiers(60, 10, true);
        modifiers.setEnabled(false, 10);
        modifiers.setEnabled(true, 9);
        assertFalse(modifiers.isEnabled());
        modifiers.setEnabled(true, 10);
        assertFalse(modifiers.isEnabled());
        modifiers.setEnabled(true, 11);
        assertTrue(modifiers.isEnabled());
        modifiers.setRefundCooldown(false, 10);
        modifiers.setRefundCooldown(true, 10);
        assertFalse(modifiers.refundCooldown());
        modifiers.setRefundCooldown(true, 11);
        assertTrue(modifiers.refundCooldown());
        modifiers.setEnabled(false, 12);
        assertFalse(modifiers.canBlock(source(DamageTypes.ARROW)));
    }

    @Test
    void rejectedDamagePreservesWindowAndAcceptedDamageRefundsOnlyOnce() {
        var modifiers = new ParryModifiers(60, 10, false);
        modifiers.setDamageRule(damage -> damage.is(DamageTypes.ARROW), 10);
        modifiers.setRefundCooldown(true, 10);
        var state = new ParryState();
        assertTrue(state.activate(modifiers.windowTicks(), modifiers.cooldownTicks(), 100));
        assertFalse(modifiers.canBlock(source(DamageTypes.ON_FIRE))
                && state.parry(modifiers.refundCooldown()));
        assertTrue(state.isActive());
        assertTrue(modifiers.canBlock(source(DamageTypes.ARROW))
                && state.parry(modifiers.refundCooldown()));
        assertFalse(state.parry(modifiers.refundCooldown()));
        assertTrue(state.activate(10, 60, 101));
    }

    @Test
    void accessoriesCanDisableCooldownRefund() {
        var modifiers = new ParryModifiers(60, 10, true);
        modifiers.setRefundCooldown(false, 10);
        var state = new ParryState();
        state.activate(modifiers.windowTicks(), modifiers.cooldownTicks(), 100);
        assertTrue(state.parry(modifiers.refundCooldown()));
        assertFalse(state.activate(10, 60, 101));
    }
}
