package net.abysslasea.parry.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.abysslasea.parry.ParryMod;
import net.abysslasea.parry.ParryNetwork;
import org.lwjgl.glfw.GLFW;

public final class ParryClient implements ClientModInitializer {
    public static final KeyMapping PARRY_KEY = new KeyMapping("key.parry.parry_key", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_5, "key.categories.parry");
    private static int attackHeldTicks = 0;

    @Override public void onInitializeClient() {
        ParryCameraConfig.load();
        KeyBindingHelper.registerKeyBinding(PARRY_KEY);
        ParticleFactoryRegistry.getInstance().register(ParryMod.IRON_SPARK.get(), IronSparkParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ParryMod.SPARK_FLASH.get(), SparkFlashParticle.Provider::new);
        ParryClientNetwork.register();
        if (loaded(ParryMod.PLAYER_ANIMATOR_ID)) PlayerAnimatorCompat.setup();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (loaded("cameraoverhaul")) CameraOverhaulCompat.tick();
            if (loaded("punchy")) PunchyCompat.tick();

            boolean inGame = client.player != null && client.screen == null && !client.isPaused();
            while (PARRY_KEY.consumeClick()) {
                if (inGame) ParryClientNetwork.request();
            }

            if (ParryCameraConfig.PARRY_ON_RELEASE_ATTACK.get()) {
                if (inGame) {
                    if (client.options.keyAttack.isDown()) {
                        attackHeldTicks++;
                    } else {
                        if (attackHeldTicks >= ParryCameraConfig.PARRY_RELEASE_ATTACK_TICKS.get()) {
                            ParryClientNetwork.request();
                        }
                        attackHeldTicks = 0;
                    }
                } else {
                    attackHeldTicks = 0;
                }
            }
        });
    }
    private static boolean loaded(String id) { return FabricLoader.getInstance().isModLoaded(id); }
    public static void success() {
        if (loaded("punchy")) PunchyCompat.onSuccess();
        if (loaded("cameraoverhaul")) CameraOverhaulCompat.onSuccess();
    }
    public static void animation(int entityId) {
        var client = Minecraft.getInstance();
        if (client.level != null && client.level.getEntity(entityId) instanceof AbstractClientPlayer player) {
            if (loaded("punchy")) PunchyCompat.play(player);
            else if (player == client.player) player.swing(InteractionHand.MAIN_HAND);
            if (loaded(ParryMod.PLAYER_ANIMATOR_ID)) PlayerAnimatorCompat.play(player);
        }
    }
}
