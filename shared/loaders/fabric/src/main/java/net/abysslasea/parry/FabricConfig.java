package net.abysslasea.parry;

import com.electronwill.nightconfig.core.file.FileConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;

public final class FabricConfig {
    private FabricConfig() {}
    public static <T> T value(FileConfig config, String key, T fallback) {
        if (!config.contains(key)) config.set(key, fallback);
        Object raw = config.get(key);
        if (fallback instanceof Double) {
            if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue())) return fallback;
            @SuppressWarnings("unchecked") T result = (T) Double.valueOf(number.doubleValue());
            return result;
        }
        if (!fallback.getClass().isInstance(raw)) return fallback;
        @SuppressWarnings("unchecked") T result = (T) raw;
        return result;
    }
    public static void load(Path path, Consumer<FileConfig> read) {
        try {
            Files.createDirectories(path.getParent());
            try (FileConfig config = FileConfig.of(path)) {
                config.load();
                read.accept(config);
                config.save();
            }
        } catch (IOException | RuntimeException error) {
            LoggerFactory.getLogger("parry").error("Unable to read Parry configuration {}", path, error);
        }
    }
    public static final class Value<T> {
        private T value;
        public Value(T initial) { value = initial; }
        public T get() { return value; }
        public void set(T value) { this.value = value; }
    }
    public static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
