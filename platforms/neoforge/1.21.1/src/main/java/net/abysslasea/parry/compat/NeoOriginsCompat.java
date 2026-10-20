package net.abysslasea.parry.compat;

import com.cyberday1.neoorigins.api.NeoOriginsAPI;
import com.cyberday1.neoorigins.api.power.PowerType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.abysslasea.parry.ParryMod;
import net.abysslasea.parry.ParryPermission;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Load after the mod presence check to avoid resolving NeoOrigins classes when absent. */
public final class NeoOriginsCompat {
    private static final ResourceKey<Registry<PowerType<?>>> POWER_TYPES_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("neoorigins", "power_type"));
    private static final DeferredRegister<PowerType<?>> POWER_TYPES =
            DeferredRegister.create(POWER_TYPES_KEY, ParryMod.MODID);

    private NeoOriginsCompat() {}

    public static void register(IEventBus modBus) {
        POWER_TYPES.register("parry", NeoOriginsParryPower::new);
        POWER_TYPES.register(modBus);
    }

    public static boolean canParry(ServerPlayer player) {
        var permission = new ParryPermission();

        // Older 2.2 releases do not filter conditions in forEachOfType; check each holder.
        // powers() still includes origin layers, dynamic grants, and global grants.
        for (var power : NeoOriginsAPI.powers(player)) {
            if (power.type() instanceof NeoOriginsParryPower
                    && power.config() instanceof NeoOriginsParryPower.Config config
                    && power.isConditionSatisfied(player)) {
                permission.accept(config.enabled(), config.priority());
            }
        }
        return permission.isEnabled();
    }
}
