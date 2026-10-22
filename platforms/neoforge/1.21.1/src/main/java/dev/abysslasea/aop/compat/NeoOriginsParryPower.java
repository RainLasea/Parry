package dev.abysslasea.aop.compat;

import com.cyberday1.neoorigins.api.power.PowerConfiguration;
import com.cyberday1.neoorigins.api.power.PowerType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Stays passive so permission rules leave NeoOrigins skill hotkeys available. */
public final class NeoOriginsParryPower extends PowerType<NeoOriginsParryPower.Config> {
    public record Config(boolean enabled, int priority) implements PowerConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("enabled").forGetter(Config::enabled),
                Codec.INT.optionalFieldOf("priority", 0).forGetter(Config::priority)
        ).apply(instance, Config::new));
    }

    @Override
    public Codec<Config> codec() {
        return Config.CODEC;
    }
}
