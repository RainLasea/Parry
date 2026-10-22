package dev.abysslasea.aop;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Only the server reads gameplay parameters; clients cannot alter damage decisions. */
public final class ParryConfig {
    public static final FabricConfig.Value<Double> PARRY_COOLDOWN = new FabricConfig.Value<>(60.0);
    public static final FabricConfig.Value<Double> PARRY_WINDOW = new FabricConfig.Value<>(10.0);
    public static final FabricConfig.Value<Boolean> REFUND_PARRY_COOLDOWN = new FabricConfig.Value<>(true);
    private ParryConfig() {}
    public static void load(MinecraftServer server) {
        PARRY_COOLDOWN.set(60.0); PARRY_WINDOW.set(10.0); REFUND_PARRY_COOLDOWN.set(true);
        FabricConfig.load(server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/artofparrying.toml"), config -> {
            PARRY_COOLDOWN.set(FabricConfig.clamp(FabricConfig.value(config, "general.parry_cooldown", 60.0), 0, 72000));
            PARRY_WINDOW.set(FabricConfig.clamp(FabricConfig.value(config, "general.parry_window", 10.0), 1, 72000));
            REFUND_PARRY_COOLDOWN.set(FabricConfig.value(config, "general.refund_parry_cooldown", true));
        });
    }
}
