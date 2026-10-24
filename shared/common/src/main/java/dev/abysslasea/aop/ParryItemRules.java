package dev.abysslasea.aop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import org.slf4j.LoggerFactory;

public final class ParryItemRules {
    private static volatile List<Rule> rules = List.of();
    private ParryItemRules() {}

    public record Target(ResourceLocation id, boolean tag) {
        public static Target parse(String value) {
            boolean tag = value.startsWith("#");
            var id = ResourceLocation.tryParse(tag ? value.substring(1) : value);
            if (id == null || value.isBlank()) throw new JsonParseException("Invalid item selector: " + value);
            return new Target(id, tag);
        }
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && (tag ? stack.is(TagKey.create(Registries.ITEM, id))
                    : BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id));
        }
        @Override public String toString() { return (tag ? "#" : "") + id; }
    }

    public record Rule(ResourceLocation id, List<Target> targets, int priority, JsonObject settings, String source) {
        public int specificity(ItemStack stack) {
            int match = -1;
            for (var target : targets) if (target.matches(stack)) match = Math.max(match, target.tag ? 0 : 1);
            return match;
        }
    }

    public record Activation(InteractionHand hand, ItemStack held, Item item, ParryProfile profile, int cooldown) {
        public boolean stillHeld(ServerPlayer player) {
            return player.getItemInHand(hand) == held && held.getItem() == item && allowed(held)
                    && (!isShield(held) || !player.getCooldowns().isOnCooldown(item));
        }
    }

    public static List<Rule> rules() { return rules; }
    public static void clear() { rules = List.of(); }

    public static Rule parse(ResourceLocation id, JsonObject json) {
        var patch = json.deepCopy();
        var targets = patch.remove("targets");
        if (targets == null || !targets.isJsonArray() || targets.getAsJsonArray().isEmpty()) throw new JsonParseException("targets must be a nonempty array");
        var parsed = new ArrayList<Target>();
        for (var target : targets.getAsJsonArray()) parsed.add(Target.parse(ParryProfile.string(target, "targets entry")));
        ParryProfile.number(patch, "priority", -1000000, 1000000);
        if (patch.has("priority") && patch.get("priority").getAsDouble() != Math.rint(patch.get("priority").getAsDouble())) {
            throw new JsonParseException("priority must be an integer");
        }
        int priority = patch.has("priority") ? patch.remove("priority").getAsInt() : 0;
        ParryProfile.validate(patch);
        return new Rule(id, List.copyOf(parsed), priority, patch, "");
    }

    public static boolean validSelector(Object value) {
        if (!(value instanceof String text)) return false;
        try { Target.parse(text); return true; } catch (RuntimeException invalid) { return false; }
    }

    public static boolean allowed(ItemStack stack) {
        var whitelist = ParryConfig.WHITELIST.get();
        if (whitelist.isEmpty()) return true;
        for (String entry : whitelist) {
            try { if (Target.parse(entry).matches(stack)) return true; }
            catch (RuntimeException invalid) { /* Invalid entries never turn a whitelist into global access. */ }
        }
        return false;
    }

    public static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ShieldItem || stack.getUseAnimation() == UseAnim.BLOCK
                || stack.getTags().anyMatch(tag -> Set.of("shields", "tools/shield").contains(tag.location().getPath())));
    }

    public static ParryProfile resolve(ItemStack stack) {
        var json = ParryProfile.defaults();
        rules.stream().filter(rule -> rule.specificity(stack) >= 0)
                .sorted(Comparator.<Rule>comparingInt(rule -> rule.specificity(stack))
                        .thenComparingInt(Rule::priority).thenComparing(rule -> rule.id.toString()))
                .forEach(rule -> ParryProfile.merge(json, rule.settings));
        return ParryProfile.read(json);
    }

    public static Activation select(ServerPlayer player) {
        // The active use hand wins. Otherwise explicit main-hand rules take precedence
        // over an off-hand shield, which takes precedence over the global empty-hand fallback.
        if (player.isUsingItem()) return candidate(player, player.getUsedItemHand());
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        if (allowed(main) && rules.stream().anyMatch(rule -> rule.specificity(main) >= 0)) {
            return candidate(player, InteractionHand.MAIN_HAND);
        }
        if (allowed(off) && (isShield(off) || rules.stream().anyMatch(rule -> rule.specificity(off) >= 0))) {
            return candidate(player, InteractionHand.OFF_HAND);
        }
        if (!main.isEmpty() && allowed(main)) return candidate(player, InteractionHand.MAIN_HAND);
        if (!off.isEmpty() && allowed(off)) return candidate(player, InteractionHand.OFF_HAND);
        if (main.isEmpty() && off.isEmpty()) return candidate(player, InteractionHand.MAIN_HAND);
        return null;
    }

    private static Activation candidate(ServerPlayer player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!allowed(stack)) return null;
        var profile = resolve(stack);
        if (!profile.enabled()) return null;
        boolean shield = isShield(stack);
        if (shield && player.getCooldowns().isOnCooldown(stack.getItem())) return null;
        Integer duration = ((ParryCooldownHistory) player.getCooldowns()).parry$duration(stack.getItem());
        return new Activation(hand, stack, stack.getItem(), profile, profile.cooldown().resolve(shield, duration));
    }

    public static class ReloadListener extends SimplePreparableReloadListener<List<Rule>> {
        @Override protected List<Rule> prepare(ResourceManager manager, ProfilerFiller profiler) {
            var loaded = new ArrayList<Rule>();
            manager.listResources("parry", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
                try (var reader = resource.openAsReader()) {
                    var rule = parse(id, ParryProfile.object(JsonParser.parseReader(reader), id.toString()));
                    loaded.add(new Rule(rule.id(), rule.targets(), rule.priority(), rule.settings(), resource.sourcePackId()));
                } catch (Exception error) {
                    LoggerFactory.getLogger(ParryMod.MODID).error("Invalid parry rule {} from {}: {}", id, resource.sourcePackId(), error.getMessage());
                }
            });
            loaded.sort(Comparator.comparing(rule -> rule.id.toString()));
            return List.copyOf(loaded);
        }
        @Override protected void apply(List<Rule> loaded, ResourceManager manager, ProfilerFiller profiler) {
            rules = loaded;
            LoggerFactory.getLogger(ParryMod.MODID).info("Loaded {} per-item parry rules", loaded.size());
        }
    }
}
