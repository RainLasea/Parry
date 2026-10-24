package dev.abysslasea.aop;

import net.minecraft.world.item.Item;

/** Session-local last applied total duration; expiry does not discard the recorded baseline. */
public interface ParryCooldownHistory {
    Integer parry$duration(Item item);
}
