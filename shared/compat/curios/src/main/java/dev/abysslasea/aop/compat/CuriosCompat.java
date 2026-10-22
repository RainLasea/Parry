package dev.abysslasea.aop.compat;

import java.util.ArrayList;
import java.util.List;
import dev.abysslasea.aop.api.ParryApi;
import dev.abysslasea.aop.api.ParryContext;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

/** Loaded only after the loader has confirmed Curios is installed. */
public final class CuriosCompat {
    private CuriosCompat() {}

    public static void register() {
        ParryApi.registerEquipmentSource(CuriosCompat::equipped);
    }

    private static List<ParryContext> equipped(ServerPlayer player) {
        var result = new ArrayList<ParryContext>();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory ->
                inventory.getCurios().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            // getStacks excludes cosmetic slots; visibility does not disable effects.
                            var stacks = entry.getValue().getStacks();
                            for (int index = 0; index < stacks.getSlots(); index++) {
                                var stack = stacks.getStackInSlot(index);
                                if (!stack.isEmpty()) {
                                    result.add(new ParryContext(player, stack, entry.getKey(), index));
                                }
                            }
                        }));
        return result;
    }
}
