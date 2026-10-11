package net.abysslasea.parry;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(ParryMod.MODID)
public final class ParryMod {
    public static final String MODID = "parry";
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> IRON_SPARK =
            PARTICLES.register("iron_spark", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK_FLASH =
            PARTICLES.register("spark_flash", () -> new SimpleParticleType(false));
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_SOUND =
            SOUNDS.register("block", () -> SoundEvent.createVariableRangeEvent(id("block")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PARRY_SOUND =
            SOUNDS.register("parry_sound", () -> SoundEvent.createVariableRangeEvent(id("parry_sound")));
    // Timers belong to the current session, so the attachment has no persistent codec.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ParryState>> PARRY_STATE =
            ATTACHMENTS.register("parry_state", () -> AttachmentType.builder(ParryState::new).build());

    public ParryMod(IEventBus modBus, ModContainer container) {
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        ATTACHMENTS.register(modBus);
        // Keep optional API classes unloaded until their mod is available.
        if (ModList.get().isLoaded("neoorigins")) {
            net.abysslasea.parry.compat.NeoOriginsCompat.register(modBus);
        }
        container.registerConfig(ModConfig.Type.SERVER, ParryConfig.SPEC, "parry.toml");
        container.registerConfig(ModConfig.Type.CLIENT, net.abysslasea.parry.client.ParryCameraConfig.SPEC,
                "parry-client.toml");
        modBus.addListener(ParryNetwork::register);
        NeoForge.EVENT_BUS.register(ParryEvents.class);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
