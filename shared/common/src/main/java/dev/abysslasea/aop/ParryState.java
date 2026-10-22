package dev.abysslasea.aop;

/** Replaces timers on activation so an earlier parry cannot close a newer window. */
public final class ParryState {
    private int activeTicks;
    private int cooldownTicks;
    private int cooldownDuration;
    private boolean cooldownChanged;
    private boolean awaitingConfirmation;
    private long lastActivationTick = Long.MIN_VALUE;

    public boolean activate(int window, int cooldown, long serverTick) {
        // Accept one activation per tick even at zero cooldown to bound feedback packets.
        if (awaitingConfirmation || cooldownTicks > 0 || lastActivationTick == serverTick) {
            return false;
        }
        activeTicks = Math.max(1, window);
        cooldownTicks = Math.max(0, cooldown);
        cooldownDuration = cooldownTicks;
        cooldownChanged = true;
        lastActivationTick = serverTick;
        return true;
    }

    public boolean isActive() {
        return activeTicks > 0 && !awaitingConfirmation;
    }

    public boolean awaitConfirmation() {
        if (!isActive()) return false;
        awaitingConfirmation = true;
        return true;
    }

    public void cancelConfirmation() {
        awaitingConfirmation = false;
    }

    public boolean parry(boolean refundCooldown) {
        // The hit was inside the window even if its client reply arrives after it expires.
        if (!isActive() && !awaitingConfirmation) {
            return false;
        }
        // Consume the window before feedback so further hits require a new activation.
        activeTicks = 0;
        awaitingConfirmation = false;
        if (refundCooldown) {
            cooldownTicks = 0;
            cooldownChanged = true;
        }
        return true;
    }

    public void tick() {
        if (activeTicks > 0) activeTicks--;
        if (cooldownTicks > 0) {
            cooldownTicks--;
            // Correct client drift once a second and confirm readiness at zero.
            if (cooldownTicks % 20 == 0) cooldownChanged = true;
        }
    }

    public int cooldownTicks() { return cooldownTicks; }
    public int cooldownDuration() { return cooldownDuration; }

    public boolean consumeCooldownUpdate() {
        boolean changed = cooldownChanged;
        cooldownChanged = false;
        return changed;
    }

    public ParryState copy() {
        var copy = new ParryState();
        copy.activeTicks = activeTicks;
        copy.cooldownTicks = cooldownTicks;
        copy.cooldownDuration = cooldownDuration;
        copy.cooldownChanged = true;
        copy.lastActivationTick = lastActivationTick;
        return copy;
    }
}
