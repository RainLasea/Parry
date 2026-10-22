package dev.abysslasea.aop;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Holds a real hit until that player's client reports its view check. Server thread only. */
public final class ParryPendingDamage {
    private static final Map<ServerPlayer, Pending> PENDING = new IdentityHashMap<>();
    private static final Set<ServerPlayer> REPLAYING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static int nextId;

    private ParryPendingDamage() {}

    public static boolean isReplaying(ServerPlayer player) { return REPLAYING.contains(player); }

    /** Preserve hit order while the first hit is awaiting a reply. */
    public static boolean queue(ServerPlayer player, DamageSource source, float amount) {
        var pending = PENDING.get(player);
        if (pending == null) return false;
        if (!pending.confirmation.queue(new Hit(source, amount))) {
            finish(player, pending, false);
            return false;
        }
        return true;
    }

    public static boolean request(ServerPlayer player, ParryState state, DamageSource source,
                                  float amount, Entity target, BooleanSupplier confirm) {
        if (PENDING.containsKey(player) || !ParryNetwork.canCheck(player)) return false;
        var confirmation = ParryConfirmation.begin(++nextId, state, new Hit(source, amount),
                confirm, hit -> replay(player, hit));
        if (confirmation == null) return false;
        var pending = new Pending(player.serverLevel(), confirmation);
        PENDING.put(player, pending);
        // No positions or view lists are sent.
        ParryNetwork.check(player, confirmation.id(), target.getId());
        return true;
    }

    public static void reply(ServerPlayer player, int requestId, boolean visible) {
        var pending = PENDING.get(player);
        if (pending != null && pending.confirmation.matches(requestId)) finish(player, pending, visible);
    }

    public static void tick() {
        for (var entry : new ArrayList<>(PENDING.entrySet())) {
            var player = entry.getKey();
            var pending = entry.getValue();
            if (!valid(player, pending)) discard(player);
            else if (pending.confirmation.tick()) PENDING.remove(player, pending);
        }
    }

    public static void discard(ServerPlayer player) {
        var pending = PENDING.remove(player);
        if (pending != null) pending.confirmation.discard();
    }

    public static void clear() {
        for (var player : new ArrayList<>(PENDING.keySet())) discard(player);
    }

    private static boolean valid(ServerPlayer player, Pending pending) {
        return player.isAlive() && !player.isRemoved() && !player.hasDisconnected()
                && !player.isSpectator() && player.serverLevel() == pending.level;
    }

    private static void finish(ServerPlayer player, Pending pending, boolean visible) {
        // Remove first: a reply can settle exactly one actual hit, never grant future immunity.
        if (!PENDING.remove(player, pending)) return;
        if (!valid(player, pending)) {
            pending.confirmation.discard();
            return;
        }
        pending.confirmation.reply(pending.confirmation.id(), visible);
    }

    private static void replay(ServerPlayer player, Hit hit) {
        if (!player.isAlive()) return;
        REPLAYING.add(player);
        try {
            player.hurt(hit.source, hit.amount);
        } finally {
            REPLAYING.remove(player);
        }
    }

    private record Hit(DamageSource source, float amount) {}

    private record Pending(ServerLevel level, ParryConfirmation<Hit> confirmation) {}
}
