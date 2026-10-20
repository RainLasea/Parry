package net.abysslasea.parry.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Avoids client game classes because the mod entry point registers this spec on servers too. */
public final class ParryCameraConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue INTENSITY;
    public static final ForgeConfigSpec.DoubleValue DURATION;
    public static final ForgeConfigSpec.DoubleValue THIRD_PERSON_SCALE;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("cameraoverhaul");
        ENABLED = builder.comment("Enable parry camera feedback when CameraOverhaul is installed on the client.")
                .define("enabled", true);
        INTENSITY = builder.comment("First-person camera motion multiplier. Set to 0 to disable feedback.")
                .defineInRange("intensity", 1.0, 0.0, 2.0);
        DURATION = builder.comment("Spring recoil duration in seconds. Body motion lasts 0.52 seconds independently.")
                .defineInRange("duration_seconds", 0.3, 0.1, 1.0);
        THIRD_PERSON_SCALE = builder.comment("Third-person camera motion multiplier relative to first person. Set to 0 to disable it.")
                .defineInRange("third_person_scale", 0.45, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private ParryCameraConfig() {}
}
