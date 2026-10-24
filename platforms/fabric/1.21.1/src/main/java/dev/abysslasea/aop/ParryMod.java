package dev.abysslasea.aop;

import java.util.function.Supplier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

public final class ParryMod implements ModInitializer {
    public static final String MODID = "artofparrying";
    public static final String PLAYER_ANIMATOR_ID = "playeranimator";
    private static SimpleParticleType ironSpark, sparkFlash;
    private static SoundEvent blockSound, parrySound;
    public static final Supplier<SimpleParticleType> IRON_SPARK = () -> ironSpark;
    public static final Supplier<SimpleParticleType> SPARK_FLASH = () -> sparkFlash;
    public static final Supplier<SoundEvent> BLOCK_SOUND = () -> blockSound;
    public static final Supplier<SoundEvent> PARRY_SOUND = () -> parrySound;
    @Override public void onInitialize() {
        ParryConfig.load();
        ironSpark = Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("iron_spark"), FabricParticleTypes.simple());
        sparkFlash = Registry.register(BuiltInRegistries.PARTICLE_TYPE, id("spark_flash"), FabricParticleTypes.simple());
        blockSound = Registry.register(BuiltInRegistries.SOUND_EVENT, id("block"), SoundEvent.createVariableRangeEvent(id("block")));
        parrySound = Registry.register(BuiltInRegistries.SOUND_EVENT, id("parry_sound"), SoundEvent.createVariableRangeEvent(id("parry_sound")));
        ParryNetwork.register();
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new FabricParryReloadListener());
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ParryEvents.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { ParryEvents.clear(); ParryItemRules.clear(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> ParryEvents.tick());
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player) || !ParryEvents.incomingDamage(player, source, amount));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ParryEvents.reset(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ParryEvents.reset(handler.player));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> ParryEvents.copy(oldPlayer, newPlayer, alive));
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MODID, path); }
    public static boolean isFakePlayer(ServerPlayer player) { return player.connection == null; }
}
