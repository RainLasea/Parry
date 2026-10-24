package dev.abysslasea.aop.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import dev.abysslasea.aop.ParryMod;
import dev.abysslasea.aop.ParryNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ParryMod.MODID, value = Dist.CLIENT)
public final class ParryClient {
    public static final KeyMapping PARRY_KEY = new KeyMapping("key.artofparrying.parry_key",
            KeyConflictContext.IN_GAME, InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5,
            "key.categories.artofparrying");

    private static int attackHeldTicks = 0;

    private ParryClient() {}

    @SubscribeEvent
    public static void sound(ParryNetwork.SoundEvent event) {
        var sound = event.sound;
        ParrySoundPlayer.play(sound.sound(), sound.x(), sound.y(), sound.z(), sound.pitch(), sound.success());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        ParryHud.tick();
        if (ModList.get().isLoaded("cameraoverhaul")) CameraOverhaulCompat.tick();
        if (ModList.get().isLoaded("punchy")) PunchyCompat.tick();

        boolean inGame = minecraft.player != null && minecraft.screen == null && !minecraft.isPaused();
        while (PARRY_KEY.consumeClick()) {
            if (inGame) PacketDistributor.sendToServer(ParryNetwork.Request.INSTANCE);
        }

        if (ParryCameraConfig.PARRY_ON_RELEASE_ATTACK.get()) {
            if (inGame) {
                if (minecraft.options.keyAttack.isDown()) {
                    attackHeldTicks++;
                } else {
                    if (attackHeldTicks >= ParryCameraConfig.PARRY_RELEASE_ATTACK_TICKS.get()) {
                        PacketDistributor.sendToServer(ParryNetwork.Request.INSTANCE);
                    }
                    attackHeldTicks = 0;
                }
            } else {
                attackHeldTicks = 0;
            }
        }
    }

    @SubscribeEvent
    public static void renderHud(RenderGuiEvent.Post event) {
        ParryHud.render(event.getGuiGraphics(), ParryHudTiming.partialTick(event.getPartialTick()));
    }

    @SubscribeEvent
    public static void check(ParryNetwork.CheckEvent event) {
        PacketDistributor.sendToServer(new ParryNetwork.Result(event.requestId(), ParryView.canSee(event.targetId())));
    }

    @SubscribeEvent
    public static void cooldown(ParryNetwork.CooldownEvent event) {
        ParryHud.sync(event.remainingTicks(), event.durationTicks());
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
            if (ModList.get().isLoaded("cloth_config")) {
                ModList.get().getModContainerById(ParryMod.MODID).orElseThrow().registerExtensionPoint(
                        net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                        (container, parent) -> ParryConfigScreen.create(parent));
            }
            if (ModList.get().isLoaded("playeranimator")) {
                event.enqueueWork(PlayerAnimatorCompat::setup);
            }
        }
    }
}
