package dev.abysslasea.aop.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Avoids client game classes because the mod entry point registers this spec on
 * servers too.
 */
public final class ParryCameraConfig {
        public static final ModConfigSpec SPEC;
        public static final ModConfigSpec.BooleanValue ENABLED;
        public static final ModConfigSpec.DoubleValue INTENSITY;
        public static final ModConfigSpec.DoubleValue DURATION;
        public static final ModConfigSpec.DoubleValue THIRD_PERSON_SCALE;

        public static final ModConfigSpec.BooleanValue PARRY_ON_RELEASE_ATTACK;
        public static final ModConfigSpec.IntValue PARRY_RELEASE_ATTACK_TICKS;

        static {
                var builder = new ModConfigSpec.Builder();
                builder.push("cameraoverhaul");
                ENABLED = builder
                                .comment("Enable parry camera feedback when CameraOverhaul is installed on the client.")
                                .define("enabled", true);
                INTENSITY = builder.comment("First-person camera motion multiplier. Set to 0 to disable feedback.")
                                .defineInRange("intensity", 1.0, 0.0, 2.0);
                DURATION = builder.comment(
                                "Spring recoil duration in seconds. Body motion lasts 0.52 seconds independently.")
                                .defineInRange("duration_seconds", 0.3, 0.1, 1.0);
                THIRD_PERSON_SCALE = builder.comment(
                                "Third-person camera motion multiplier relative to first person. Set to 0 to disable it.")
                                .defineInRange("third_person_scale", 0.45, 0.0, 1.0);
                builder.pop();

                builder.push("controls");
                PARRY_ON_RELEASE_ATTACK = builder
                                .comment("Trigger parry when releasing the attack key after holding it.")
                                .define("parry_on_release_attack", false);
                PARRY_RELEASE_ATTACK_TICKS = builder
                                .comment("Minimum ticks the attack key must be held to trigger parry on release.")
                                .defineInRange("parry_release_attack_ticks", 3, 1, 100);
                builder.pop();

                SPEC = builder.build();
        }

        private ParryCameraConfig() {
        }
}
