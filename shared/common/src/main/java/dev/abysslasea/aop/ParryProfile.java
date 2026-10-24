package dev.abysslasea.aop;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Immutable settings captured when a parry starts. Missing JSON fields inherit independently. */
public record ParryProfile(boolean enabled, int window, Cooldown cooldown, boolean refund,
                           boolean reflect, double reflectMultiplier, String activationSound, String successSound) {
    public record Cooldown(String mode, int ticks, double percent, int fallbackTicks) {
        public int resolve(boolean shield, Integer recordedDuration) {
            if (!shield || mode.equals("fixed")) return ticks;
            double base = recordedDuration == null ? fallbackTicks : recordedDuration;
            return (int) Math.max(0, Math.min(72000, Math.floor(base * percent / 100.0)));
        }
    }

    public static JsonObject defaults() {
        var json = new JsonObject();
        json.addProperty("enabled", true);
        json.addProperty("parry_window", ParryConfig.PARRY_WINDOW.get());
        json.addProperty("refund_parry_cooldown", ParryConfig.REFUND_PARRY_COOLDOWN.get());
        json.addProperty("reflect_damage", ParryConfig.REFLECT_DAMAGE.get());
        json.addProperty("reflect_multiplier", ParryConfig.REFLECT_MULTIPLIER.get());
        var cooldown = new JsonObject();
        cooldown.addProperty("mode", "shield_cooldown");
        cooldown.addProperty("ticks", ParryConfig.PARRY_COOLDOWN.get());
        cooldown.addProperty("percent", ParryConfig.SHIELD_COOLDOWN_PERCENT.get());
        cooldown.addProperty("fallback_ticks", ParryConfig.PARRY_COOLDOWN.get());
        json.add("cooldown", cooldown);
        var sounds = new JsonObject();
        sounds.addProperty("activation", "artofparrying:block");
        sounds.addProperty("success", "artofparrying:parry_sound");
        json.add("sounds", sounds);
        return json;
    }

    public static ParryProfile read(JsonObject json) {
        var c = json.getAsJsonObject("cooldown");
        var s = json.getAsJsonObject("sounds");
        return new ParryProfile(json.get("enabled").getAsBoolean(), json.get("parry_window").getAsInt(),
                new Cooldown(c.get("mode").getAsString(), c.get("ticks").getAsInt(),
                        c.get("percent").getAsDouble(), c.get("fallback_ticks").getAsInt()),
                json.get("refund_parry_cooldown").getAsBoolean(), json.get("reflect_damage").getAsBoolean(),
                json.get("reflect_multiplier").getAsDouble(), s.get("activation").getAsString(), s.get("success").getAsString());
    }

    public static void merge(JsonObject destination, JsonObject patch) {
        for (var entry : patch.entrySet()) {
            if (entry.getValue().isJsonObject() && destination.has(entry.getKey()) && destination.get(entry.getKey()).isJsonObject()) {
                merge(destination.getAsJsonObject(entry.getKey()), entry.getValue().getAsJsonObject());
            } else destination.add(entry.getKey(), entry.getValue().deepCopy());
        }
    }

    public static void validate(JsonObject patch) {
        keys(patch, Set.of("enabled", "parry_window", "refund_parry_cooldown", "reflect_damage", "reflect_multiplier", "cooldown", "sounds"));
        bool(patch, "enabled");
        bool(patch, "refund_parry_cooldown");
        bool(patch, "reflect_damage");
        number(patch, "parry_window", 1, 72000);
        number(patch, "reflect_multiplier", 0, 1000);
        if (patch.has("cooldown")) {
            var c = object(patch.get("cooldown"), "cooldown");
            keys(c, Set.of("mode", "ticks", "percent", "fallback_ticks"));
            if (c.has("mode") && !Set.of("fixed", "shield_cooldown").contains(string(c.get("mode"), "cooldown.mode"))) {
                throw new JsonParseException("cooldown.mode must be fixed or shield_cooldown");
            }
            number(c, "ticks", 0, 72000);
            number(c, "percent", 0, 10000);
            number(c, "fallback_ticks", 0, 72000);
        }
        if (patch.has("sounds")) {
            var s = object(patch.get("sounds"), "sounds");
            keys(s, Set.of("activation", "success"));
            for (var entry : s.entrySet()) {
                String id = string(entry.getValue(), "sounds." + entry.getKey());
                if (!id.isEmpty() && ResourceLocation.tryParse(id) == null) throw new JsonParseException("Invalid sound ID: " + id);
            }
        }
    }

    public static JsonObject object(JsonElement value, String name) {
        if (value == null || !value.isJsonObject()) throw new JsonParseException(name + " must be an object");
        return value.getAsJsonObject();
    }

    public static String string(JsonElement value, String name) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new JsonParseException(name + " must be a string");
        return value.getAsString();
    }

    private static void bool(JsonObject json, String key) {
        if (json.has(key) && (!json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isBoolean())) throw new JsonParseException(key + " must be a boolean");
    }

    public static void number(JsonObject json, String key, double min, double max) {
        if (!json.has(key)) return;
        var value = json.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new JsonParseException(key + " must be a number");
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number < min || number > max) throw new JsonParseException(key + " must be between " + min + " and " + max);
    }

    public static void keys(JsonObject json, Set<String> allowed) {
        for (String key : json.keySet()) if (!allowed.contains(key)) throw new JsonParseException("Unknown field: " + key);
    }
}
