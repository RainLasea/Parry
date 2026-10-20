package net.abysslasea.parry;

/** Resolves rules by priority; denial wins ties and missing rules allow parrying. */
public final class ParryPermission {
    private boolean hasRule;
    private int priority;
    private boolean enabled = true;

    public void accept(boolean enabled, int priority) {
        if (!hasRule || priority > this.priority) {
            hasRule = true;
            this.priority = priority;
            this.enabled = enabled;
        } else if (priority == this.priority) {
            this.enabled &= enabled;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
