package dev.abysslasea.aop;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** One reserved hit and its ordered follow-up damage, settled by one client reply or timeout. */
final class ParryConfirmation<T> {
    private static final int TIMEOUT_TICKS = 20;
    private static final int MAX_QUEUED_HITS = 64;
    private final int id;
    private final ParryState state;
    private final BooleanSupplier confirm;
    private final Consumer<T> replay;
    private final ArrayList<T> hits = new ArrayList<>();
    private int ticks = TIMEOUT_TICKS;
    private boolean resolved;

    static <T> ParryConfirmation<T> begin(int id, ParryState state, T hit,
                                         BooleanSupplier confirm, Consumer<T> replay) {
        if (!state.awaitConfirmation()) return null;
        return new ParryConfirmation<>(id, state, hit, confirm, replay);
    }

    private ParryConfirmation(int id, ParryState state, T hit,
                              BooleanSupplier confirm, Consumer<T> replay) {
        this.id = id;
        this.state = state;
        this.confirm = confirm;
        this.replay = replay;
        hits.add(hit);
    }

    int id() { return id; }
    boolean matches(int requestId) { return !resolved && id == requestId; }

    boolean queue(T hit) {
        if (resolved || hits.size() >= MAX_QUEUED_HITS) return false;
        hits.add(hit);
        return true;
    }

    boolean reply(int requestId, boolean visible) {
        if (!matches(requestId)) return false;
        resolved = true;
        boolean blocked = false;
        try {
            blocked = visible && confirm.getAsBoolean();
        } finally {
            if (!blocked) state.cancelConfirmation();
        }
        for (int i = blocked ? 1 : 0; i < hits.size(); i++) replay.accept(hits.get(i));
        return true;
    }

    boolean tick() {
        if (!resolved && --ticks <= 0) reply(id, false);
        return resolved;
    }

    void discard() {
        resolved = true;
        state.cancelConfirmation();
    }
}
