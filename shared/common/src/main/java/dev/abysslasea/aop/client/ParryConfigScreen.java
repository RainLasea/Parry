package dev.abysslasea.aop.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import dev.abysslasea.aop.ParryConfig;
import dev.abysslasea.aop.ParryItemRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ParryConfigScreen {
    private ParryConfigScreen() {}

    private static Component text(String key, Object... args) {
        return Component.translatable("config.artofparrying." + key, args);
    }

    public static Screen create(Screen parent) {
        var minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        boolean editable = server != null || minecraft.getConnection() == null;
        var changes = new ArrayList<Runnable>();
        var builder = ConfigBuilder.create().setParentScreen(parent).setTitle(text("config")).transparentBackground()
                .setSavingRunnable(() -> {
                    ParryCameraConfig.save();
                    Runnable saveGameplay = () -> {
                        changes.forEach(Runnable::run);
                        ParryConfig.save();
                    };
                    if (server != null && server == minecraft.getSingleplayerServer()) server.execute(saveGameplay);
                    else if (editable && minecraft.getSingleplayerServer() == null && minecraft.getConnection() == null) saveGameplay.run();
                });
        var e = builder.entryBuilder();
        var general = builder.getOrCreateCategory(text("general"));
        if (editable) {
            var whitelist = e.startSubCategory(text("whitelist_status", text("whitelist_group"),
                    ParryConfig.WHITELIST.get().isEmpty() ? text("global_access") : text("whitelist_count", ParryConfig.WHITELIST.get().size()))
                    .copy().withStyle(ChatFormatting.GOLD)).setExpanded(false);
            whitelist.add(e.startStrList(text("whitelist"), new ArrayList<>(ParryConfig.WHITELIST.get()))
                    .setDefaultValue(List.of()).setExpanded(true).setTooltip(text("whitelist.tooltip"))
                    .setCellErrorSupplier(value -> ParryItemRules.validSelector(value) ? Optional.empty() : Optional.of(text("invalid_selector")))
                    .setSaveConsumer(value -> changes.add(() -> ParryConfig.WHITELIST.set(List.copyOf(value)))).build());
            general.addEntry(whitelist.build());
            var timing = section(e, "timing", true);
            timing.add(decimal(e, "parry_window", ParryConfig.PARRY_WINDOW.get(), 10, 1, 72000,
                    value -> changes.add(() -> ParryConfig.PARRY_WINDOW.set(value))));
            timing.add(decimal(e, "parry_cooldown", ParryConfig.PARRY_COOLDOWN.get(), 60, 0, 72000,
                    value -> changes.add(() -> ParryConfig.PARRY_COOLDOWN.set(value))));
            var shield = section(e, "shield_cooldown", false);
            shield.add(decimal(e, "shield_cooldown_percent", ParryConfig.SHIELD_COOLDOWN_PERCENT.get(), 100, 0, 10000,
                    value -> changes.add(() -> ParryConfig.SHIELD_COOLDOWN_PERCENT.set(value))));
            timing.add(shield.build());
            timing.add(e.startBooleanToggle(text("refund_parry_cooldown"), ParryConfig.REFUND_PARRY_COOLDOWN.get())
                    .setDefaultValue(true).setTooltip(text("refund_parry_cooldown.tooltip"))
                    .setSaveConsumer(value -> changes.add(() -> ParryConfig.REFUND_PARRY_COOLDOWN.set(value))).build());
            general.addEntry(timing.build());
            var reflect = section(e, "reflection", false);
            reflect.add(e.startBooleanToggle(text("reflect_damage"), ParryConfig.REFLECT_DAMAGE.get()).setDefaultValue(false)
                    .setTooltip(text("reflect_damage.tooltip"))
                    .setSaveConsumer(value -> changes.add(() -> ParryConfig.REFLECT_DAMAGE.set(value))).build());
            reflect.add(decimal(e, "reflect_multiplier", ParryConfig.REFLECT_MULTIPLIER.get(), 1, 0, 1000,
                    value -> changes.add(() -> ParryConfig.REFLECT_MULTIPLIER.set(value))));
            general.addEntry(reflect.build());
        } else general.addEntry(e.startTextDescription(text("server_managed")).build());
        var datapacks = builder.getOrCreateCategory(text("datapacks"));
        datapacks.addEntry(e.startTextDescription(text("datapacks_info")).build());
        if (server != null) {
            datapacks.addEntry(e.startTextDescription(text("rule_count", ParryItemRules.rules().size())
                    .copy().withStyle(ChatFormatting.AQUA)).build());
            for (var rule : ParryItemRules.rules()) {
                var group = e.startSubCategory(Component.literal(rule.id().toString()).withStyle(ChatFormatting.GOLD));
                var matching = section(e, "matching", true);
                for (var target : rule.targets()) matching.add(e.startTextDescription(Component.literal(target.toString())
                        .withStyle(ChatFormatting.AQUA)).build());
                matching.add(e.startTextDescription(text("rule_priority", rule.priority())).build());
                matching.add(e.startTextDescription(text("rule_source", rule.source())).build());
                group.add(matching.build());
                describe(e, group, rule.settings());
                datapacks.addEntry(group.build());
            }
        } else datapacks.addEntry(e.startTextDescription(text("rules_world_required")).build());
        var client = builder.getOrCreateCategory(text("client"));
        var controls = section(e, "controls", true);
        controls.add(e.startBooleanToggle(text("release_attack"), ParryCameraConfig.PARRY_ON_RELEASE_ATTACK.get())
                .setDefaultValue(false).setTooltip(text("release_attack.tooltip"))
                .setSaveConsumer(ParryCameraConfig.PARRY_ON_RELEASE_ATTACK::set).build());
        controls.add(e.startIntSlider(text("release_ticks"), ParryCameraConfig.PARRY_RELEASE_ATTACK_TICKS.get(), 1, 100)
                .setDefaultValue(3).setTextGetter(value -> text("ticks", value)).setTooltip(text("release_ticks.tooltip"))
                .setSaveConsumer(ParryCameraConfig.PARRY_RELEASE_ATTACK_TICKS::set).build());
        client.addEntry(controls.build());
        var camera = section(e, "camera", false).setTooltip(text("camera.tooltip"));
        camera.add(e.startBooleanToggle(text("camera_enabled"), ParryCameraConfig.ENABLED.get()).setDefaultValue(true)
                .setSaveConsumer(ParryCameraConfig.ENABLED::set).build());
        camera.add(decimal(e, "camera_intensity", ParryCameraConfig.INTENSITY.get(), 1, 0, 2, ParryCameraConfig.INTENSITY::set));
        camera.add(decimal(e, "camera_duration", ParryCameraConfig.DURATION.get(), 0.3, 0.1, 1, ParryCameraConfig.DURATION::set));
        camera.add(decimal(e, "third_person_scale", ParryCameraConfig.THIRD_PERSON_SCALE.get(), 0.45, 0, 1, ParryCameraConfig.THIRD_PERSON_SCALE::set));
        client.addEntry(camera.build());
        var sounds = section(e, "sounds", true);
        volume(sounds, e, "block_volume", ParryCameraConfig.BLOCK_SOUND_VOLUME.get(), ParryCameraConfig.BLOCK_SOUND_VOLUME::set);
        volume(sounds, e, "parry_volume", ParryCameraConfig.PARRY_SOUND_VOLUME.get(), ParryCameraConfig.PARRY_SOUND_VOLUME::set);
        client.addEntry(sounds.build());
        builder.setFallbackCategory(editable ? general : client).alwaysShowTabs();
        return builder.build();
    }

    private static SubCategoryBuilder section(ConfigEntryBuilder e, String key, boolean expanded) {
        return e.startSubCategory(text(key).copy().withStyle(ChatFormatting.GOLD)).setExpanded(expanded);
    }

    private static AbstractConfigListEntry<Double> decimal(ConfigEntryBuilder e, String key, double value,
                                                          double fallback, double min, double max, Consumer<Double> save) {
        return e.startDoubleField(text(key), value).setDefaultValue(fallback).setMin(min).setMax(max)
                .setTooltip(text(key + ".tooltip"))
                .setErrorSupplier(number -> Double.isFinite(number) && number >= min && number <= max ? Optional.empty() : Optional.of(text("invalid_number", min, max)))
                .setSaveConsumer(save).build();
    }

    private static void describe(ConfigEntryBuilder e, SubCategoryBuilder group, JsonObject settings) {
        for (var field : settings.entrySet()) {
            if (field.getValue().isJsonObject()) {
                var child = e.startSubCategory(text("profile." + field.getKey()));
                for (var inner : field.getValue().getAsJsonObject().entrySet()) {
                    child.add(e.startTextDescription(text("rule_field", text("profile." + inner.getKey()),
                            describeValue(inner.getKey(), inner.getValue()))).build());
                }
                group.add(child.build());
            } else group.add(e.startTextDescription(text("rule_field", text("profile." + field.getKey()),
                    describeValue(field.getKey(), field.getValue()))).build());
        }
    }

    private static Component describeValue(String key, JsonElement value) {
        if (value.getAsJsonPrimitive().isBoolean()) return text(value.getAsBoolean() ? "on" : "off");
        if (key.equals("mode")) return text("profile.mode." + value.getAsString());
        return value.getAsString().isEmpty() ? text("muted") : Component.literal(value.getAsString());
    }

    private static void volume(SubCategoryBuilder category, ConfigEntryBuilder entries, String key, double value, Consumer<Double> save) {
        category.add(entries.startIntSlider(text(key), (int) Math.round(value * 100), 0, 100)
                .setDefaultValue(50).setTextGetter(percent -> text("volume", percent)).setTooltip(text("volume.tooltip"))
                .setSaveConsumer(percent -> save.accept(percent / 100.0)).build());
    }
}
