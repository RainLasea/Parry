package dev.abysslasea.aop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParryStateTest {
    @Test
    void reservesOneHitAndAcceptsItsReplyAfterTheWindowExpires() {
        var state = new ParryState();
        state.activate(1, 0, 100);
        assertTrue(state.awaitConfirmation());
        assertFalse(state.awaitConfirmation());
        assertFalse(state.isActive());
        state.tick();
        assertFalse(state.activate(10, 0, 101));
        assertTrue(state.parry(true));
        assertFalse(state.parry(true));
        assertTrue(state.activate(10, 0, 102));
    }

    @Test
    void deniedConfirmationDoesNotExtendAnExpiredWindow() {
        var state = new ParryState();
        state.activate(1, 60, 100);
        state.awaitConfirmation();
        state.tick();
        state.cancelConfirmation();
        assertFalse(state.isActive());
        assertFalse(state.parry(true));
        assertEquals(59, state.cooldownTicks());
    }

    @Test
    void deniedConfirmationPreservesTheRemainderOfTheOriginalWindow() {
        var state = new ParryState();
        state.activate(5, 60, 100);
        state.awaitConfirmation();
        state.tick();
        state.cancelConfirmation();
        assertTrue(state.isActive());
        assertEquals(59, state.cooldownTicks());
    }

    @Test
    void reportsActualDurationAndConfirmsReadinessWhenCooldownExpires() {
        var state = new ParryState();
        assertTrue(state.activate(5, 47, 100));
        assertEquals(47, state.cooldownDuration());
        assertEquals(47, state.cooldownTicks());
        assertTrue(state.consumeCooldownUpdate());
        assertFalse(state.consumeCooldownUpdate());
        for (int remaining = 46; remaining >= 0; remaining--) {
            state.tick();
            assertEquals(remaining, state.cooldownTicks());
            assertEquals(47, state.cooldownDuration());
            assertEquals(remaining % 20 == 0, state.consumeCooldownUpdate());
        }
        state.tick();
        assertEquals(0, state.cooldownTicks());
        assertFalse(state.consumeCooldownUpdate());
    }

    @Test
    void successfulParryOnlyReportsReadinessWhenCooldownIsRefunded() {
        for (boolean refund : new boolean[] {false, true}) {
            var state = new ParryState();
            state.activate(10, 60, 100);
            state.consumeCooldownUpdate();
            state.tick();
            assertTrue(state.parry(refund));
            assertEquals(refund ? 0 : 59, state.cooldownTicks());
            assertEquals(60, state.cooldownDuration());
            assertEquals(refund, state.consumeCooldownUpdate());
        }
    }

    @Test
    void dimensionCloneKeepsCooldownAndRequestsFreshClientSync() {
        var state = new ParryState();
        state.activate(10, 60, 100);
        state.consumeCooldownUpdate();
        state.tick();
        var copy = state.copy();
        assertEquals(59, copy.cooldownTicks());
        assertEquals(60, copy.cooldownDuration());
        assertTrue(copy.consumeCooldownUpdate());
        assertFalse(copy.activate(10, 60, 101));
        copy.tick();
        assertEquals(58, copy.cooldownTicks());
        assertEquals(59, state.cooldownTicks());
    }

    @Test
    void zeroCooldownRemainsReadyAndNextActivationUsesItsNewDuration() {
        var state = new ParryState();
        assertTrue(state.activate(10, 0, 100));
        assertEquals(0, state.cooldownTicks());
        assertEquals(0, state.cooldownDuration());
        assertTrue(state.consumeCooldownUpdate());
        assertFalse(state.activate(10, 60, 100));
        assertFalse(state.consumeCooldownUpdate());
        assertTrue(state.activate(10, 23, 101));
        assertEquals(23, state.cooldownDuration());
        assertTrue(state.consumeCooldownUpdate());
    }
}
