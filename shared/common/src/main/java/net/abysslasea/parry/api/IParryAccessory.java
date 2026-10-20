package net.abysslasea.parry.api;

import net.minecraft.world.damagesource.DamageSource;

/** Implement on an Item, or register an adapter with {@link ParryApi#registerAccessory}. */
public interface IParryAccessory {
    /** Called on the server at activation and before incoming damage; do not mutate the world here. */
    default void configureParry(ParryContext context, ParryModifiers modifiers) {}

    /** Called once per equipped slot after a successful parry has consumed its window. */
    default void onParrySuccess(ParryContext context, DamageSource source) {}
}
