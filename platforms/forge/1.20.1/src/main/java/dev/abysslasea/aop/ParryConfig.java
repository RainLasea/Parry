package dev.abysslasea.aop;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ParryConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue PARRY_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue PARRY_WINDOW;
    public static final ForgeConfigSpec.BooleanValue REFUND_PARRY_COOLDOWN;

    static {
        var builder = new ForgeConfigSpec.Builder();
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
