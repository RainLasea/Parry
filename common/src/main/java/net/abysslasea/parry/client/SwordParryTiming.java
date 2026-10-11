package net.abysslasea.parry.client;

/** Animation seconds are independent of the server's parry window. */
public final class SwordParryTiming {
    public static final double IRON_SWORD_ATTACK_SPEED = 1.6;
    public static final double SUCCESS_TIME = 1.33;

    private SwordParryTiming() {}

    public static float speedMultiplier(double attackSpeed) {
        if (!Double.isFinite(attackSpeed)) return 1.0F;
        // Bound extreme modded attributes so an animation neither freezes nor disappears.
        return (float) Math.max(0.25, Math.min(4.0, attackSpeed / IRON_SWORD_ATTACK_SPEED));
    }

    static double smootherstep(double progress) {
        double u = Math.max(0, Math.min(1, progress));
        return u * u * u * (u * (u * 6 - 15) + 10);
    }

    public static final class SuccessJump {
        private double start, target, speed, duration, elapsed, gain;
        private boolean active;

        public boolean begin(double currentTime, double clipLength, double playbackSpeed) {
            reset();
            if (!Double.isFinite(currentTime) || !Double.isFinite(clipLength)
                    || !Double.isFinite(playbackSpeed) || currentTime < 0 || playbackSpeed <= 0) return false;
            target = Math.min(SUCCESS_TIME, clipLength);
            if (target <= currentTime) return false;
            start = currentTime;
            speed = playbackSpeed;
            double gap = target - start;
            // Usually 100 ms for an iron sword. A nearby target needs a shorter handoff.
            duration = Math.min(Math.max(0.05, Math.min(0.15, 0.1 / speed)), gap / (2 * speed));
            gain = gap - speed * duration;
            active = true;
            return true;
        }

        public double advance(double deltaSeconds) {
            if (!active || !Double.isFinite(deltaSeconds) || deltaSeconds <= 0) return sample();
            elapsed += deltaSeconds;
            double time = sample();
            if (elapsed >= duration) active = false;
            return time;
        }

        private double sample() {
            if (duration <= 0) return start;
            // t = t0 + v*s + (target - t0 - v*D) * smootherstep(s/D).
            // Both endpoint velocities equal v; endpoint accelerations equal zero.
            return start + speed * elapsed + gain * smootherstep(elapsed / duration);
        }

        public boolean isActive() { return active; }
        public void reset() { active = false; start = target = speed = duration = elapsed = gain = 0; }
    }
}
