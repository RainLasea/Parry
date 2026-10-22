package dev.abysslasea.aop.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.abysslasea.aop.ParryMod;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Reveals the ready sprite from bottom to top as the server's cooldown expires. */
public final class ParryHud {
    private static final ResourceLocation READY = ParryMod.id("textures/gui/parry_ready.png");
    private static final ResourceLocation COOLDOWN = ParryMod.id("textures/gui/parry_cooldown.png");
    private static final int TEXTURE_SIZE = 16;
    private static final int ICON_SIZE = 10;
    private static final int ATTACK_INDICATOR_Y = 9;
    private static final int INDICATOR_GAP = 2;
    // The shield occupies rows 2 through 14 of the original 16px canvas.
    private static final int FILL_BOTTOM = 15;
    private static final int FILL_HEIGHT = 13;
    private static final int READY_HOLD_TICKS = 8;
    private static final int FADE_TICKS = 12;
    private static LocalPlayer player;
    private static int remainingTicks;
    private static int durationTicks;
    private static int readyTicks = READY_HOLD_TICKS + FADE_TICKS;

    private ParryHud() {}

    public static void sync(int remaining, int duration) {
        player = Minecraft.getInstance().player;
        durationTicks = Math.max(0, duration);
        remainingTicks = Mth.clamp(remaining, 0, durationTicks);
        // Activation also sends an update when the configured cooldown is zero.
        readyTicks = 0;
    }

    public static void tick() {
        var client = Minecraft.getInstance();
        if (player != client.player || client.player == null) {
            player = client.player;
            remainingTicks = durationTicks = 0;
            readyTicks = READY_HOLD_TICKS + FADE_TICKS;
        } else if (!client.isPaused()) {
            if (remainingTicks > 0) {
                remainingTicks--;
                readyTicks = 0;
            } else if (readyTicks < READY_HOLD_TICKS + FADE_TICKS) {
                readyTicks++;
            }
        }
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        var client = Minecraft.getInstance();
        if (client.player == null || !client.player.isAlive() || client.player.isSpectator()
                || client.options.hideGui || !client.options.getCameraType().isFirstPerson()
                || ParryHudTiming.debugCrosshair(client)) return;

        float fade = remainingTicks > 0 ? 0.0F
                : Mth.clamp((readyTicks + partialTick - READY_HOLD_TICKS) / FADE_TICKS, 0.0F, 1.0F);
        float opacity = 1.0F - fade * fade * (3.0F - 2.0F * fade);
        if (opacity <= 0.0F) return;

        // Share vanilla's attack-indicator position, stacking below it when visible.
        int x = graphics.guiWidth() / 2 - ICON_SIZE / 2;
        int y = graphics.guiHeight() / 2 + ATTACK_INDICATOR_Y;
        int attackIndicatorHeight = attackIndicatorHeight(client);
        if (attackIndicatorHeight > 0) y += attackIndicatorHeight + INDICATOR_GAP;
        float progress = durationTicks == 0 || remainingTicks == 0 ? 1.0F
                : Mth.clamp(1.0F - (remainingTicks - partialTick) / durationTicks, 0.0F, 1.0F);
        // Fill the visible shield for the whole cooldown, so transparent margins cannot
        // make it look fully ready before the server's timer reaches zero.
        int height = progress >= 1.0F ? TEXTURE_SIZE
                : TEXTURE_SIZE - FILL_BOTTOM + (int) (progress * FILL_HEIGHT);
        int split = Mth.ceil((float) (TEXTURE_SIZE - height) * ICON_SIZE / TEXTURE_SIZE);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        // Vanilla's inverse-color blend uses source RGB, so fading only alpha would
        // leave the icon visible. Scaling RGB blends its effect back into the scene.
        graphics.setColor(opacity, opacity, opacity, 1.0F);
        // Draw both complete sprites on the same sampling grid. Clip at a whole GUI
        // pixel so shrinking the icon cannot shift texels at the moving fill boundary.
        // The clips must not overlap: inverse-color blending would draw those pixels twice.
        if (split > 0) {
            graphics.enableScissor(x, y, x + ICON_SIZE, y + split);
            graphics.blit(COOLDOWN, x, y, ICON_SIZE, ICON_SIZE,
                    0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.disableScissor();
        }
        if (split < ICON_SIZE) {
            graphics.enableScissor(x, y + split, x + ICON_SIZE, y + ICON_SIZE);
            graphics.blit(READY, x, y, ICON_SIZE, ICON_SIZE,
                    0.0F, 0.0F, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.disableScissor();
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static int attackIndicatorHeight(Minecraft client) {
        if (client.options.attackIndicator().get() != AttackIndicatorStatus.CROSSHAIR) return 0;

        // Vanilla samples at zero partial tick for both the cooldown bar and ready sword.
        float strength = client.player.getAttackStrengthScale(0.0F);
        if (strength < 1.0F) return 4;
        // The ready sword and plus occupy only the top 7 rows of their 16px canvas.
        if (client.crosshairPickEntity instanceof LivingEntity target && target.isAlive()
                && client.player.getCurrentItemAttackStrengthDelay() > 5.0F) return 7;
        return 0;
    }
}
