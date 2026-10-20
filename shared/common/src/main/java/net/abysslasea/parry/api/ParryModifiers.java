package net.abysslasea.parry.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.abysslasea.parry.ParryPermission;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/** Per-resolution accumulator. Numeric bonuses add first, then multiply, independent of slot order. */
public final class ParryModifiers {
    private final double baseCooldown;
    private final double baseWindow;
    private double cooldownBonus;
    private double windowBonus;
    private double cooldownMultiplier = 1;
    private double windowMultiplier = 1;
    private double knockbackMultiplier = 1;
    private final ParryPermission enabled = new ParryPermission();
    private final ParryPermission refund = new ParryPermission();
    private final List<DamageRule> damageRules = new ArrayList<>();

    public ParryModifiers(int cooldownTicks, int windowTicks, boolean refundCooldown) {
        baseCooldown = cooldownTicks;
        baseWindow = windowTicks;
        refund.accept(refundCooldown, Integer.MIN_VALUE);
    }

    public void addCooldownTicks(double ticks) { cooldownBonus += finite(ticks); }
    public void multiplyCooldown(double multiplier) { cooldownMultiplier *= factor(multiplier); }
    public void addWindowTicks(double ticks) { windowBonus += finite(ticks); }
    public void multiplyWindow(double multiplier) { windowMultiplier *= factor(multiplier); }
    public void multiplyKnockback(double multiplier) { knockbackMultiplier *= factor(multiplier); }

    /** Higher priority wins; false wins equal-priority conflicts. */
    public void setEnabled(boolean value, int priority) { enabled.accept(value, priority); }
    public void setRefundCooldown(boolean value, int priority) { refund.accept(value, priority); }

    /** A whitelist: matching sources are allowed, every other source is denied at this priority. */
    public void setDamageRule(Predicate<DamageSource> allowed, int priority) {
        damageRules.add(new DamageRule(Objects.requireNonNull(allowed), priority, false, false));
    }

    /** Only matching sources contribute an allow rule; other sources keep their existing rules. */
    public void allowDamage(Predicate<DamageSource> matches, int priority) {
        damageRules.add(new DamageRule(Objects.requireNonNull(matches), priority, true, true));
    }

    /** Only matching sources contribute a deny rule. */
    public void denyDamage(Predicate<DamageSource> matches, int priority) {
        damageRules.add(new DamageRule(Objects.requireNonNull(matches), priority, true, false));
    }

    public int cooldownTicks() { return ticks(baseCooldown, cooldownBonus, cooldownMultiplier, 0); }
    public int windowTicks() { return ticks(baseWindow, windowBonus, windowMultiplier, 1); }
    public boolean isEnabled() { return enabled.isEnabled(); }
    public boolean refundCooldown() { return refund.isEnabled(); }
    public double knockbackStrength() {
        return Math.min(1.2 * knockbackMultiplier, 64);
    }

    public boolean canBlock(DamageSource source) {
        // Accessories cannot override the existing void protection rule.
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return false;
        var permission = new ParryPermission();
        for (var rule : damageRules) {
            boolean matches = rule.predicate.test(source);
            if (!rule.matchOnly || matches) {
                permission.accept(rule.matchOnly ? rule.allow : matches, rule.priority);
            }
        }
        return isEnabled() && permission.isEnabled();
    }

    private static int ticks(double base, double bonus, double multiplier, int minimum) {
        double value = (base + bonus) * multiplier;
        // Guard arithmetic overflow as well as invalid external modifiers.
        if (Double.isNaN(value)) value = base;
        return (int) Math.max(minimum, Math.min(72000, value));
    }

    private static double finite(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Parry modifier must be finite");
        return value;
    }

    private static double factor(double value) {
        if (finite(value) < 0) throw new IllegalArgumentException("Parry multiplier must be nonnegative");
        return value;
    }

    private record DamageRule(Predicate<DamageSource> predicate, int priority, boolean matchOnly, boolean allow) {}
}
