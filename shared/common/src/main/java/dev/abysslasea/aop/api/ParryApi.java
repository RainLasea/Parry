package dev.abysslasea.aop.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;

/** Loader-neutral extension API. Register adapters and equipment sources during common setup. */
public final class ParryApi {
    private static final ConcurrentHashMap<Item, IParryAccessory> ACCESSORIES = new ConcurrentHashMap<>();
    private static final List<Function<ServerPlayer, List<ParryContext>>> SOURCES = new CopyOnWriteArrayList<>();

    private ParryApi() {}

    /** An explicit adapter takes precedence over an Item's IParryAccessory implementation. */
    public static void registerAccessory(Item item, IParryAccessory accessory) {
        ACCESSORIES.put(Objects.requireNonNull(item), Objects.requireNonNull(accessory));
    }

    /** Sources must return functional slots only. Curios is registered automatically when present. */
    public static void registerEquipmentSource(Function<ServerPlayer, List<ParryContext>> source) {
        SOURCES.add(Objects.requireNonNull(source));
    }

    public static Resolved resolve(ServerPlayer player, int cooldown, int window, boolean refund) {
        var modifiers = new ParryModifiers(cooldown, window, refund);
        var equipped = new ArrayList<Equipped>();
        for (var source : SOURCES) {
            for (var context : source.apply(player)) {
                if (context.stack().isEmpty()) continue;
                var item = context.stack().getItem();
                IParryAccessory accessory = ACCESSORIES.get(item);
                if (accessory == null && item instanceof IParryAccessory implementation) {
                    accessory = implementation;
                }
                if (accessory != null) {
                    accessory.configureParry(context, modifiers);
                    equipped.add(new Equipped(context, accessory));
                }
            }
        }
        return new Resolved(modifiers, List.copyOf(equipped));
    }

    private record Equipped(ParryContext context, IParryAccessory accessory) {}

    /** Snapshot of the accessories used for this resolution; never cache across equipment changes. */
    public static final class Resolved {
        private final ParryModifiers modifiers;
        private final List<Equipped> equipped;

        private Resolved(ParryModifiers modifiers, List<Equipped> equipped) {
            this.modifiers = modifiers;
            this.equipped = equipped;
        }

        public ParryModifiers modifiers() { return modifiers; }

        public void onSuccess(DamageSource source) {
            for (var entry : equipped) entry.accessory.onParrySuccess(entry.context, source);
        }
    }
}
