package net.abysslasea.parry.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.abysslasea.parry.ParryMod;
import net.abysslasea.parry.ParryNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ParryMod.MODID, value = Dist.CLIENT)
public final class ParryClient {
    public static final KeyMapping PARRY_KEY = new KeyMapping("key.parry.parry_key",
            KeyConflictContext.IN_GAME, InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5,
            "key.categories.parry");

    private ParryClient() {}

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var minecraft = Minecraft.getInstance();
        if (ModList.get().isLoaded("cameraoverhaul")) CameraOverhaulCompat.tick();
        if (ModList.get().isLoaded("punchy")) PunchyCompat.tick();
        while (PARRY_KEY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null && !minecraft.isPaused()) {
                ParryNetwork.request();
            }
        }
    }

    @SubscribeEvent
    public static void success(ParryNetwork.SuccessEvent event) {
        if (ModList.get().isLoaded("punchy")) PunchyCompat.onSuccess();
        if (ModList.get().isLoaded("cameraoverhaul")) CameraOverhaulCompat.onSuccess();
    }

    @SubscribeEvent
    public static void animation(ParryNetwork.AnimationEvent event) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(event.entityId()) instanceof AbstractClientPlayer player) {
            if (ModList.get().isLoaded("punchy")) {
                PunchyCompat.play(player);
            } else if (player == Minecraft.getInstance().player) {
                // Keep the ordinary swing when the first-person animation integration is absent.
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (ModList.get().isLoaded("playeranimator")) PlayerAnimatorCompat.play(player);
        }
    }

    @EventBusSubscriber(modid = ParryMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void particles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ParryMod.IRON_SPARK.get(), IronSparkParticle.Provider::new);
            event.registerSpriteSet(ParryMod.SPARK_FLASH.get(), SparkFlashParticle.Provider::new);
        }

        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent event) { event.register(PARRY_KEY); }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            if (ModList.get().isLoaded("playeranimator")) {
                event.enqueueWork(PlayerAnimatorCompat::setup);
            }
        }
    }
}
