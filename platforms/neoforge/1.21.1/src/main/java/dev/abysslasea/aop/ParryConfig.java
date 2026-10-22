package dev.abysslasea.aop;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ParryConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue PARRY_COOLDOWN;
    public static final ModConfigSpec.DoubleValue PARRY_WINDOW;
    public static final ModConfigSpec.BooleanValue REFUND_PARRY_COOLDOWN;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("general");
        PARRY_COOLDOWN = builder.comment("Ticks before another parry can start. Fractional ticks round down; 0 disables cooldown.")
                .defineInRange("parry_cooldown", 60.0, 0.0, 72000.0);
        PARRY_WINDOW = builder.comment("Ticks during which incoming non-void damage can be parried. Fractional ticks round down.")
                .defineInRange("parry_window", 10.0, 1.0, 72000.0);
        REFUND_PARRY_COOLDOWN = builder.comment("Clear the remaining cooldown after a successful parry.")
                .define("refund_parry_cooldown", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ParryConfig() {}
}
