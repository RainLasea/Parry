package net.abysslasea.parry;

/** Replaces timers on activation so an earlier parry cannot close a newer window. */
public final class ParryState {
    private int activeTicks;
    private int cooldownTicks;
    private long lastActivationTick = Long.MIN_VALUE;

    public boolean activate(int window, int cooldown, long serverTick) {
        // Accept one activation per tick even at zero cooldown to bound feedback packets.
        if (cooldownTicks > 0 || lastActivationTick == serverTick) {
            return false;
        }
        activeTicks = Math.max(1, window);
        cooldownTicks = Math.max(0, cooldown);
        lastActivationTick = serverTick;
        return true;
    }

    public boolean isActive() {
        return activeTicks > 0;
    }

    public boolean parry(boolean refundCooldown) {
        if (!isActive()) {
            return false;
        }
        // Consume the window before feedback so further hits require a new activation.
        activeTicks = 0;
        if (refundCooldown) {
            cooldownTicks = 0;
        }
        return true;
    }

    public void tick() {
        if (activeTicks > 0) activeTicks--;
        if (cooldownTicks > 0) cooldownTicks--;
    }

    public ParryState copy() {
        var copy = new ParryState();
        copy.activeTicks = activeTicks;
        copy.cooldownTicks = cooldownTicks;
        copy.lastActivationTick = lastActivationTick;
        return copy;
    }
}
