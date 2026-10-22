package dev.abysslasea.aop.client;

import net.minecraft.client.Minecraft;

public final class ParryHudTiming {
    private ParryHudTiming() {}
    public static float partialTick(float partialTick) { return partialTick; }
    public static boolean debugCrosshair(Minecraft client) {
        return client.options.renderDebug && !client.player.isReducedDebugInfo()
                && !client.options.reducedDebugInfo().get();
    }
}
