package dev.abysslasea.aop.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;

public final class ParryHudTiming {
    private ParryHudTiming() {}
    public static float partialTick(DeltaTracker tracker) { return tracker.getGameTimeDeltaPartialTick(false); }
    public static boolean debugCrosshair(Minecraft client) {
        return client.getDebugOverlay().showDebugScreen() && !client.player.isReducedDebugInfo()
                && !client.options.reducedDebugInfo().get();
    }
}
