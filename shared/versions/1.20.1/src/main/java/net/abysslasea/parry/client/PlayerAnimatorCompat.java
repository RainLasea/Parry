package net.abysslasea.parry.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.abysslasea.parry.ParryMod;

/** Load only after the client confirms Player Animator is installed. */
final class PlayerAnimatorCompat {
    private static final ResourceLocation LAYER = ParryMod.id("player_animation");
    private static final ResourceLocation ANIMATION = ParryMod.id("block.animation");

    private PlayerAnimatorCompat() {}

    static void setup() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1000,
                player -> new ModifierLayer<IAnimation>());
    }

    @SuppressWarnings("unchecked")
    static void play(AbstractClientPlayer player) {
        var layer = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER);
        var animation = PlayerAnimationRegistry.getAnimation(ANIMATION);
        if (layer instanceof ModifierLayer<?> modifier && !modifier.isActive() && animation != null) {
            // This layer comes from our factory, which always creates ModifierLayer<IAnimation>.
            ((ModifierLayer<IAnimation>) modifier).setAnimation(new dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer(animation));
        }
    }
}
