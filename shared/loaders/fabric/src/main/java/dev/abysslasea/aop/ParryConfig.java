package dev.abysslasea.aop;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Only the server reads gameplay parameters; clients cannot alter damage decisions. */
public final class ParryConfig {
    public static final FabricConfig.Value<Double> PARRY_COOLDOWN = new FabricConfig.Value<>(60.0);
    public static final FabricConfig.Value<Double> PARRY_WINDOW = new FabricConfig.Value<>(10.0);
    public static final FabricConfig.Value<Boolean> REFUND_PARRY_COOLDOWN = new FabricConfig.Value<>(true);
    public static final FabricConfig.Value<java.util.List<? extends String>> WHITELIST = new FabricConfig.Value<>(java.util.List.of());
    public static final FabricConfig.Value<Double> SHIELD_COOLDOWN_PERCENT = new FabricConfig.Value<>(100.0);
    public static final FabricConfig.Value<Boolean> REFLECT_DAMAGE = new FabricConfig.Value<>(false);
    public static final FabricConfig.Value<Double> REFLECT_MULTIPLIER = new FabricConfig.Value<>(1.0);
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("artofparrying.toml");
    private ParryConfig() {}
    public static void save() {
        FabricConfig.load(PATH, config -> {
            config.set("general.parry_cooldown", PARRY_COOLDOWN.get());
            config.set("general.parry_window", PARRY_WINDOW.get());
            config.set("general.refund_parry_cooldown", REFUND_PARRY_COOLDOWN.get());
            config.set("general.parry_whitelist", WHITELIST.get());
            config.set("general.shield_cooldown_percent", SHIELD_COOLDOWN_PERCENT.get());
            config.set("general.reflect_damage", REFLECT_DAMAGE.get());
            config.set("general.reflect_multiplier", REFLECT_MULTIPLIER.get());
        });
    }
    public static void load() {
        PARRY_COOLDOWN.set(60.0); PARRY_WINDOW.set(10.0); REFUND_PARRY_COOLDOWN.set(true);
        WHITELIST.set(java.util.List.of()); SHIELD_COOLDOWN_PERCENT.set(100.0);
        REFLECT_DAMAGE.set(false); REFLECT_MULTIPLIER.set(1.0);
        FabricConfig.load(PATH, config -> {
            PARRY_COOLDOWN.set(FabricConfig.clamp(FabricConfig.value(config, "general.parry_cooldown", 60.0), 0, 72000));
            PARRY_WINDOW.set(FabricConfig.clamp(FabricConfig.value(config, "general.parry_window", 10.0), 1, 72000));
            REFUND_PARRY_COOLDOWN.set(FabricConfig.value(config, "general.refund_parry_cooldown", true));
            if (!config.contains("general.parry_whitelist")) config.set("general.parry_whitelist", java.util.List.of());
            Object raw = config.get("general.parry_whitelist");
            if (raw instanceof java.util.List<?> list) {
                // Keep malformed selectors restrictive rather than silently opening global access.
                WHITELIST.set(list.stream().map(String::valueOf).toList());
            } else WHITELIST.set(java.util.List.of("invalid selector"));
            SHIELD_COOLDOWN_PERCENT.set(FabricConfig.clamp(FabricConfig.value(config, "general.shield_cooldown_percent", 100.0), 0, 10000));
            REFLECT_DAMAGE.set(FabricConfig.value(config, "general.reflect_damage", false));
            REFLECT_MULTIPLIER.set(FabricConfig.clamp(FabricConfig.value(config, "general.reflect_multiplier", 1.0), 0, 1000));
        });
    }
}
