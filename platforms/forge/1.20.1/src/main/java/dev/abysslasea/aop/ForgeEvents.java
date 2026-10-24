package dev.abysslasea.aop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeEvents {
    private ForgeEvents() {}
    @SubscribeEvent public static void reload(net.minecraftforge.event.AddReloadListenerEvent event) {
        event.addListener(new ParryItemRules.ReloadListener());
    }
    @SubscribeEvent public static void damage(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ParryEvents.incomingDamage(player, event.getSource(), event.getAmount())) event.setCanceled(true);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ParryEvents.tick();
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ParryEvents.reset(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ParryEvents.reset(player);
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer old && event.getEntity() instanceof ServerPlayer player) ParryEvents.copy(old, player, !event.isWasDeath());
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { ParryEvents.clear(); ParryItemRules.clear(); }
}
