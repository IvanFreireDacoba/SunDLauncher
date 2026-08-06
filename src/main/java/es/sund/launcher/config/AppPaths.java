package es.sund.launcher.config;

import java.io.File;

/**
 * Rutas de ficheros/carpetas del launcher. A diferencia de AppConstants, esto
 * no son valores fijos "de negocio" sino rutas calculadas según el sistema
 * operativo en el que se ejecuta.
 *
 * CONFIG_DIR/RESOURCEPACKS_DIR son a nivel de launcher (el paquete de
 * configuración propio que gestiona LauncherUpdateService, independiente del
 * juego). Las carpetas de una instalación de Minecraft en sí (versions,
 * libraries, assets, natives, config, resourcepacks del propio juego) están
 * separadas por instancia bajo INSTANCES_DIR: ver {@link #forInstance(int)}.
 */
public final class AppPaths {

    public static final File ROOT_DIR = resolveRootDir();
    public static final File INSTANCES_DIR = new File(ROOT_DIR, "instances");
    public static final File CONFIG_DIR = new File(ROOT_DIR, "config");
    public static final File RESOURCEPACKS_DIR = new File(ROOT_DIR, "resourcepacks");
    public static final File CREDENTIALS_FILE = new File(ROOT_DIR, "credentials.dat");
    public static final File CREDENTIALS_KEY_FILE = new File(ROOT_DIR, ".credkey");

    /** Carpetas de una instalación de Minecraft para una instancia concreta (SunD Origins, CobbleSpain, ...). */
    public static final class InstancePaths {
        public final File root;
        public final File versionsDir;
        public final File librariesDir;
        public final File assetsDir;
        public final File nativesDir;
        public final File configDir;
        public final File resourcepacksDir;
        public final File modsDir;

        private InstancePaths(File root) {
            this.root = root;
            this.versionsDir = new File(root, "versions");
            this.librariesDir = new File(root, "libraries");
            this.assetsDir = new File(root, "assets");
            this.nativesDir = new File(root, "natives");
            this.configDir = new File(root, "config");
            this.resourcepacksDir = new File(root, "resourcepacks");
            this.modsDir = new File(root, "mods");
            for (File dir : new File[] { root, versionsDir, librariesDir, assetsDir, nativesDir, configDir, resourcepacksDir, modsDir }) {
                dir.mkdirs();
            }
        }
    }

    /** Rutas aisladas para la instancia dada, creando las carpetas si no existen. Independiente entre instancias. */
    public static InstancePaths forInstance(int instanceId) {
        return new InstancePaths(new File(INSTANCES_DIR, String.valueOf(instanceId)));
    }

    private static File resolveRootDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        File base;
        if (os.contains("win")) {
            String appdata = System.getenv("APPDATA");
            base = new File(appdata != null ? appdata : home, AppConstants.LAUNCHER_NAME);
        } else if (os.contains("mac")) {
            base = new File(home, "Library/Application Support/" + AppConstants.LAUNCHER_NAME);
        } else {
            String xdgData = System.getenv("XDG_DATA_HOME");
            File dataHome = new File(xdgData != null ? xdgData : home + "/.local/share");
            base = new File(dataHome, AppConstants.LAUNCHER_NAME);
        }
        base.mkdirs();
        return base;
    }

    private AppPaths() {}
}
