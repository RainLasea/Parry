package dev.abysslasea.aop.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** The functional equipment slot, including its live stack for success callbacks. */
public record ParryContext(ServerPlayer player, ItemStack stack, String slotIdentifier, int slotIndex) {}
