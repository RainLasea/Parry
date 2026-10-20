package net.abysslasea.parry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(ParryMod.MODID)
public final class ParryMod {
    public static final String MODID = "parry";
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MODID);
    public static final RegistryObject<SimpleParticleType> IRON_SPARK = PARTICLES.register("iron_spark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SPARK_FLASH = PARTICLES.register("spark_flash", () -> new SimpleParticleType(false));
    public static final RegistryObject<SoundEvent> BLOCK_SOUND = SOUNDS.register("block", () -> SoundEvent.createVariableRangeEvent(id("block")));
    public static final RegistryObject<SoundEvent> PARRY_SOUND = SOUNDS.register("parry_sound", () -> SoundEvent.createVariableRangeEvent(id("parry_sound")));
    public ParryMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        SOUNDS.register(bus);
        PARTICLES.register(bus);
        if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
            net.abysslasea.parry.compat.CuriosCompat.register();
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ParryConfig.SPEC, "parry.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, net.abysslasea.parry.client.ParryCameraConfig.SPEC, "parry-client.toml");
        ParryNetwork.register();
        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
    }
    public static ResourceLocation id(String path) { return new ResourceLocation(MODID, path); }
    public static boolean isFakePlayer(ServerPlayer player) { return player instanceof FakePlayer; }
}
