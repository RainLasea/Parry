package dev.abysslasea.aop.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Avoids client game classes because the mod entry point registers this spec on servers too. */
public final class ParryCameraConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue INTENSITY;
    public static final ForgeConfigSpec.DoubleValue DURATION;
    public static final ForgeConfigSpec.DoubleValue THIRD_PERSON_SCALE;

    public static final ForgeConfigSpec.BooleanValue PARRY_ON_RELEASE_ATTACK;
    public static final ForgeConfigSpec.IntValue PARRY_RELEASE_ATTACK_TICKS;
    public static final ForgeConfigSpec.DoubleValue BLOCK_SOUND_VOLUME;
    public static final ForgeConfigSpec.DoubleValue PARRY_SOUND_VOLUME;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("cameraoverhaul");
        ENABLED = builder.comment("Enable parry camera feedback. Requires CameraOverhaul.")
                .define("enabled", true);
        INTENSITY = builder.comment("First-person camera intensity. 0 disables feedback.")
                .defineInRange("intensity", 1.0, 0.0, 2.0);
        DURATION = builder.comment("Camera recoil duration in seconds.")
                .defineInRange("duration_seconds", 0.3, 0.1, 1.0);
        THIRD_PERSON_SCALE = builder.comment("Third-person intensity relative to first person. 0 disables feedback.")
                .defineInRange("third_person_scale", 0.45, 0.0, 1.0);
        builder.pop();

        builder.push("controls");
        PARRY_ON_RELEASE_ATTACK = builder.comment("Trigger parry when releasing the attack key after holding it.")
                .define("parry_on_release_attack", false);
        PARRY_RELEASE_ATTACK_TICKS = builder.comment("Required attack-key hold time in ticks.")
                .defineInRange("parry_release_attack_ticks", 3, 1, 100);
        builder.pop();

        builder.push("sounds");
        BLOCK_SOUND_VOLUME = builder.comment("Block sound volume. Set to 0 to mute.")
                .defineInRange("block_volume", 0.5, 0.0, 1.0);
        PARRY_SOUND_VOLUME = builder.comment("Successful parry sound volume. Set to 0 to mute.")
                .defineInRange("parry_volume", 0.5, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }

    private ParryCameraConfig() {}

    public static void save() { SPEC.save(); }
}
