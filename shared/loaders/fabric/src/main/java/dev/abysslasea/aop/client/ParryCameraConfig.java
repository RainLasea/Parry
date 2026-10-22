package dev.abysslasea.aop.client;

import dev.abysslasea.aop.FabricConfig;
import net.fabricmc.loader.api.FabricLoader;

public final class ParryCameraConfig {
    public static final FabricConfig.Value<Boolean> ENABLED = new FabricConfig.Value<>(true);
    public static final FabricConfig.Value<Double> INTENSITY = new FabricConfig.Value<>(1.0);
    public static final FabricConfig.Value<Double> DURATION = new FabricConfig.Value<>(0.3);
    public static final FabricConfig.Value<Double> THIRD_PERSON_SCALE = new FabricConfig.Value<>(0.45);

    public static final FabricConfig.Value<Boolean> PARRY_ON_RELEASE_ATTACK = new FabricConfig.Value<>(false);
    public static final FabricConfig.Value<Integer> PARRY_RELEASE_ATTACK_TICKS = new FabricConfig.Value<>(5);

    private ParryCameraConfig() {
    }

    public static void load() {
        FabricConfig.load(FabricLoader.getInstance().getConfigDir().resolve("artofparrying-client.toml"), config -> {
            ENABLED.set(FabricConfig.value(config, "cameraoverhaul.enabled", true));
            INTENSITY.set(FabricConfig.clamp(FabricConfig.value(config, "cameraoverhaul.intensity", 1.0), 0, 2));
            DURATION.set(
                    FabricConfig.clamp(FabricConfig.value(config, "cameraoverhaul.duration_seconds", 0.3), 0.1, 1));
            THIRD_PERSON_SCALE.set(
                    FabricConfig.clamp(FabricConfig.value(config, "cameraoverhaul.third_person_scale", 0.45), 0, 1));

            PARRY_ON_RELEASE_ATTACK.set(FabricConfig.value(config, "controls.parry_on_release_attack", false));
            PARRY_RELEASE_ATTACK_TICKS.set(
                    FabricConfig.clamp(FabricConfig.value(config, "controls.parry_release_attack_ticks", 3), 1, 100));
        });
    }
}
