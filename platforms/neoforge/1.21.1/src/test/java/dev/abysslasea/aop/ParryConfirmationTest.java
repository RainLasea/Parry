package dev.abysslasea.aop;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ParryConfirmationTest {
    @Test
    void acceptsOnlyTheMatchingReplyAndBlocksExactlyOneHit() {
        var state = activeState(10);
        var damage = new ArrayList<Integer>();
        var confirmations = new AtomicInteger();
        var pending = ParryConfirmation.begin(42, state, 1, () -> {
            confirmations.incrementAndGet();
            return state.parry(true);
        }, damage::add);
        assertNotNull(pending);
        pending.queue(2);
        pending.queue(3);
        assertFalse(pending.reply(41, true));
        assertEquals(0, confirmations.get());
        assertTrue(pending.reply(42, true));
        assertEquals(List.of(2, 3), damage);
        assertEquals(1, confirmations.get());
        assertFalse(pending.reply(42, true));
        assertFalse(pending.queue(4));
        assertEquals(0, state.cooldownTicks());
    }

    @Test
    void denialReplaysEveryHitInOrderWithoutRefundingCooldown() {
        var state = activeState(10);
        var damage = new ArrayList<Integer>();
        var pending = ParryConfirmation.begin(42, state, 1, () -> fail("denial must not confirm"), damage::add);
        pending.queue(2);
        pending.queue(3);
        assertTrue(pending.reply(42, false));
        assertEquals(List.of(1, 2, 3), damage);
        assertEquals(60, state.cooldownTicks());
        assertTrue(state.isActive());
    }

    @Test
    void silenceTimesOutAndLateSuccessCannotEraseTheDamage() {
        var state = activeState(1);
        var damage = new ArrayList<Integer>();
        var pending = ParryConfirmation.begin(42, state, 1, () -> fail("timeout must not confirm"), damage::add);
        for (int tick = 0; tick < 19; tick++) {
            state.tick();
            assertFalse(pending.tick());
        }
        assertTrue(damage.isEmpty());
        assertTrue(pending.tick());
        assertEquals(List.of(1), damage);
        assertFalse(pending.reply(42, true));
        assertFalse(state.isActive());
    }

    @Test
    void acceptsAReservedHitAfterItsWindowExpires() {
        var state = activeState(1);
        var damage = new ArrayList<Integer>();
        var pending = ParryConfirmation.begin(42, state, 1, () -> state.parry(false), damage::add);
        state.tick();
        assertTrue(pending.reply(42, true));
        assertTrue(damage.isEmpty());
        assertEquals(59, state.cooldownTicks());
    }

    @Test
    void revokedServerPermissionStillReplaysDamageAfterClientSuccess() {
        var state = activeState(10);
        var damage = new ArrayList<Integer>();
        var pending = ParryConfirmation.begin(42, state, 1, () -> false, damage::add);
        assertTrue(pending.reply(42, true));
        assertEquals(List.of(1), damage);
        assertEquals(60, state.cooldownTicks());
    }

    @Test
    void sessionResetDiscardsRepliesAndReleasesTheReservation() {
        var state = activeState(10);
        var damage = new ArrayList<Integer>();
        var pending = ParryConfirmation.begin(42, state, 1, () -> fail("discarded request"), damage::add);
        pending.discard();
        assertFalse(pending.reply(42, true));
        assertTrue(damage.isEmpty());
        assertTrue(state.isActive());
    }

    @Test
    void cannotReserveAnInactiveWindowOrAnAlreadyReservedOne() {
        var state = new ParryState();
        assertNull(ParryConfirmation.begin(42, state, 1, () -> true, ignored -> {}));
        state.activate(10, 0, 100);
        var pending = ParryConfirmation.begin(43, state, 1, () -> true, ignored -> {});
        assertNotNull(pending);
        assertNull(ParryConfirmation.begin(44, state, 2, () -> true, ignored -> {}));
    }

    @Test
    void queuesAreBoundedWhileWaitingForAReply() {
        var state = activeState(10);
        var damage = new ArrayList<Integer>();
        var pending = ParryConfirmation.begin(42, state, 0, () -> false, damage::add);
        for (int hit = 1; hit < 64; hit++) assertTrue(pending.queue(hit));
        assertFalse(pending.queue(64));
        pending.reply(42, false);
        assertEquals(64, damage.size());
        assertEquals(0, damage.getFirst());
        assertEquals(63, damage.getLast());
    }

    private static ParryState activeState(int window) {
        var state = new ParryState();
        state.activate(window, 60, 100);
        return state;
    }
}
