package net.abysslasea.parry.client;

/** Samples recoil without render dependencies so elapsed time alone determines the pose. */
public final class ParryRecoil {
    private double elapsed = 1;
    private double duration = 0.3;
    private double initial;

    public void trigger(double durationSeconds) {
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0) {
            throw new IllegalArgumentException("Recoil duration must be finite and positive");
        }
        // Continue from the current pose to prevent a visible jump when hits overlap.
        initial = sample();
        duration = durationSeconds;
        elapsed = 0;
    }

    public void advance(double seconds) {
        if (Double.isFinite(seconds) && seconds > 0) elapsed = Math.min(duration, elapsed + seconds);
    }

    public double sample() {
        double progress = elapsed / duration;
        if (progress >= 1) return 0;
        if (progress < 0.15) return lerp(initial, 1, smooth(progress / 0.15));
        if (progress < 0.55) return lerp(1, -0.16, smooth((progress - 0.15) / 0.4));
        return lerp(-0.16, 0, smooth((progress - 0.55) / 0.45));
    }

    public void reset() { elapsed = duration; initial = 0; }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }
    private static double lerp(double from, double to, double t) { return from + (to - from) * t; }
}
