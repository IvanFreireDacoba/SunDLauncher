package es.sund.launcher.config;

import es.sund.launcher.model.GameInstance;

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
 * separadas por instancia bajo INSTANCES_DIR: ver {@link #forInstance(GameInstance)}.
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
            // Ojo: NO crear las carpetas aquí. Este constructor se ejecuta en cada
            // AppPaths.forInstance(instance) -incluida cada comprobación de estado, p.ej.
            // InstanceInstallStatus.isInstalled()-, así que si recreara el árbol de
            // carpetas aquí, el refresco de estado que sigue a UninstallAction
            // volvería a levantar el esqueleto vacío justo después de borrarlo.
            // Cada instalador (MinecraftInstaller/FabricInstaller/
            // InstanceContentInstaller/DownloadUtil.downloadFile) ya crea sus
            // propias carpetas justo antes de escribir en ellas.
        }
    }

    /**
     * Rutas aisladas para la instancia dada (no crea ninguna carpeta: solo calcula las
     * rutas, cada escritura real crea lo que necesite en el momento). Independiente entre
     * instancias. Carpeta "<folder>_instance" (p.ej. "SunDOrigins_instance") en vez del
     * antiguo id numérico ("2"/"3") — mucho más legible si el jugador entra a la carpeta
     * de datos del launcher a mano (ver el botón "Ficheros locales" del perfil).
     *
     * SEGURIDAD (auditoría 2026-08-14): instance.folder viene tal cual del JSON de
     * /APIs/GameCatalog. Sin validar, un backend comprometido con un folder tipo
     * "../../../algo" habría hecho que TODO lo que escribe dentro de InstancePaths
     * (instaladores, GameLauncher) y sobre todo UninstallAction.deleteRecursive()
     * -un borrado recursivo real- operase fuera de INSTANCES_DIR. requireSafeFolderName()
     * exige un identificador simple (letras/dígitos/guion/guion bajo), igual de estricto
     * que DownloadUtil.resolveChild() pero para un único segmento en vez de una ruta.
     */
    public static InstancePaths forInstance(GameInstance instance) {
        File folder = new File(INSTANCES_DIR, requireSafeFolderName(instance.folder) + "_instance");
        if (!folder.exists()) {
            migrateLegacyFolder(instance.id, folder);
        }
        return new InstancePaths(folder);
    }

    private static String requireSafeFolderName(String folder) {
        if (folder == null || !folder.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalStateException(
                    "Nombre de carpeta de instancia inválido recibido del backend: \"" + folder + "\"");
        }
        return folder;
    }

    /**
     * Compatibilidad con instalaciones ya existentes de antes del 2026-08-14, cuando las
     * carpetas de instancia se nombraban por id numérico en vez de "<folder>_instance". Si
     * la carpeta nueva no existe pero la vieja sí, se renombra en vez de tratarla como una
     * instalación nueva -evita duplicar la descarga completa y dejar la carpeta vieja
     * huérfana ocupando espacio en disco a cualquiera que ya tuviera la instancia
     * instalada-. Best-effort: si el rename falla por lo que sea, forInstance() sigue
     * devolviendo la ruta nueva y el instalador la trata como instancia nueva, sin más.
     */
    private static void migrateLegacyFolder(int legacyId, File newFolder) {
        File legacyFolder = new File(INSTANCES_DIR, String.valueOf(legacyId));
        if (legacyFolder.isDirectory()) {
            legacyFolder.renameTo(newFolder);
        }
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
