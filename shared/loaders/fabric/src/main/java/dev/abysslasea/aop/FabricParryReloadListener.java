package dev.abysslasea.aop;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

public final class FabricParryReloadListener extends ParryItemRules.ReloadListener implements IdentifiableResourceReloadListener {
    @Override public ResourceLocation getFabricId() { return ParryMod.id("parry"); }
}
