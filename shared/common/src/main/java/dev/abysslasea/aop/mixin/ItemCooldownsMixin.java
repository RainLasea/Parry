package dev.abysslasea.aop.mixin;

import dev.abysslasea.aop.ParryCooldownHistory;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCooldowns.class)
public abstract class ItemCooldownsMixin implements ParryCooldownHistory {
    @Unique private final Map<Item, Integer> parry$durations = new HashMap<>();

    @Inject(method = "addCooldown", at = @At("HEAD"))
    private void parry$record(Item item, int ticks, CallbackInfo callback) {
        if (ticks > 0) parry$durations.put(item, ticks);
    }

    @Override public Integer parry$duration(Item item) { return parry$durations.get(item); }
}
