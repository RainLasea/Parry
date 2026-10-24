package dev.abysslasea.aop;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ParryConfig {
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> WHITELIST;
    public static final ForgeConfigSpec.DoubleValue SHIELD_COOLDOWN_PERCENT;
    public static final ForgeConfigSpec.BooleanValue REFLECT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue REFLECT_MULTIPLIER;
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue PARRY_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue PARRY_WINDOW;
    public static final ForgeConfigSpec.BooleanValue REFUND_PARRY_COOLDOWN;

    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        PARRY_COOLDOWN = builder.comment("Parry cooldown in ticks. 0 disables the cooldown.")
                .defineInRange("parry_cooldown", 60.0, 0.0, 72000.0);
        PARRY_WINDOW = builder.comment("Time available to parry after activation, in ticks.")
                .defineInRange("parry_window", 10.0, 1.0, 72000.0);
        REFUND_PARRY_COOLDOWN = builder.comment("Reset the parry cooldown on success.")
                .define("refund_parry_cooldown", true);
        WHITELIST = builder.comment("Allowed item IDs or #tags. Leave empty to allow all items and empty hands.")
                .defineListAllowEmpty("parry_whitelist", java.util.List.<String>of(), ParryItemRules::validSelector);
        SHIELD_COOLDOWN_PERCENT = builder.comment("Shield parry cooldown as a percentage of the shield's cooldown.")
                .defineInRange("shield_cooldown_percent", 100.0, 0.0, 10000.0);
        REFLECT_DAMAGE = builder.comment("Return damage to the attacker after a successful parry.")
                .define("reflect_damage", false);
        REFLECT_MULTIPLIER = builder.comment("Reflected damage multiplier. 1.0 returns 100%.")
                .defineInRange("reflect_multiplier", 1.0, 0.0, 1000.0);
        builder.pop();
        SPEC = builder.build();
    }

    public static void save() { SPEC.save(); }

    private ParryConfig() {}
}
