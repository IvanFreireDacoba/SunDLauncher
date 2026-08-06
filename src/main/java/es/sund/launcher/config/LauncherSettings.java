package es.sund.launcher.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Ajustes del launcher que persisten entre arranques (a diferencia de
 * AppConstants, que son fijos). Fichero de propiedades simple bajo
 * AppPaths.ROOT_DIR, sin dependencias nuevas. Si el fichero no existe o está
 * corrupto, se usan los valores por defecto en vez de fallar: un ajuste de
 * preferencias nunca debe impedir que el launcher arranque.
 */
public final class LauncherSettings {

    private static final Path FILE = new File(AppPaths.ROOT_DIR, "settings.properties").toPath();
    private static final String KEY_MINIMIZE_DURING_GAME = "minimizeDuringGame";
    private static final boolean DEFAULT_MINIMIZE_DURING_GAME = true;

    private static volatile Boolean minimizeDuringGameCache;

    public static boolean isMinimizeDuringGameEnabled() {
        if (minimizeDuringGameCache == null) {
            minimizeDuringGameCache = loadBoolean(KEY_MINIMIZE_DURING_GAME, DEFAULT_MINIMIZE_DURING_GAME);
        }
        return minimizeDuringGameCache;
    }

    public static void setMinimizeDuringGameEnabled(boolean enabled) {
        minimizeDuringGameCache = enabled;
        saveBoolean(KEY_MINIMIZE_DURING_GAME, enabled);
    }

    private static boolean loadBoolean(String key, boolean fallback) {
        String value = readAll().getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : fallback;
    }

    private static void saveBoolean(String key, boolean value) {
        Properties props = readAll();
        props.setProperty(key, String.valueOf(value));
        writeAll(props);
    }

    private static Properties readAll() {
        Properties props = new Properties();
        if (Files.exists(FILE)) {
            try (var in = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                props.load(in);
            } catch (IOException ignored) {
                // Fichero corrupto o ilegible: se usan los valores por defecto.
            }
        }
        return props;
    }

    private static void writeAll(Properties props) {
        try (var out = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
            props.store(out, "SunDLauncher settings");
        } catch (IOException ignored) {
            // Best-effort: si no se puede guardar, el ajuste vuelve al valor por defecto en el próximo arranque.
        }
    }

    private LauncherSettings() {}
}
