package dev.abysslasea.aop.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ParryConfigScreen {
    private ParryConfigScreen() {}

    public static Screen create(Screen parent) {
        var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.artofparrying.config"))
                .setSavingRunnable(ParryCameraConfig::save);
        var sounds = builder.getOrCreateCategory(Component.translatable("config.artofparrying.sounds"));
        var entries = builder.entryBuilder();
        volume(sounds, entries, "block_volume", ParryCameraConfig.BLOCK_SOUND_VOLUME.get(),
                ParryCameraConfig.BLOCK_SOUND_VOLUME::set);
        volume(sounds, entries, "parry_volume", ParryCameraConfig.PARRY_SOUND_VOLUME.get(),
                ParryCameraConfig.PARRY_SOUND_VOLUME::set);
        return builder.build();
    }

    private static void volume(ConfigCategory category, ConfigEntryBuilder entries, String key,
                               double value, Consumer<Double> save) {
        category.addEntry(entries.startIntSlider(Component.translatable("config.artofparrying." + key),
                        (int) Math.round(value * 100), 0, 100)
                .setDefaultValue(50)
                .setTextGetter(percent -> Component.translatable("config.artofparrying.volume", percent))
                .setTooltip(Component.translatable("config.artofparrying.volume.tooltip"))
                .setSaveConsumer(percent -> save.accept(percent / 100.0))
                .build());
    }
}
