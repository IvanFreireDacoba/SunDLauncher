package es.sund.launcher.config;

/**
 * Todos los valores "hardcodeados" de la aplicación viven aquí, y solo aquí.
 * Ninguna otra clase debería tener una URL, versión o número mágico escrito
 * directamente en su código: siempre referencian esta interfaz.
 */
public interface AppConstants {

    // ---- Identidad de la aplicación ----
    String LAUNCHER_NAME = "SunDLauncher";
    String CURRENT_LAUNCHER_VERSION = "1.2";

    // ---- Tu API (SunD.es) ----
    String API_BASE_URL = "https://sund.es/APIs";
    String API_CHECK_ACCOUNT_ENDPOINT = API_BASE_URL + "/CheckServerAccount";
    String API_CHECK_LAUNCHER_VERSION_ENDPOINT = API_BASE_URL + "/LauncherVersion";
    String API_GAME_CATALOG_ENDPOINT = API_BASE_URL + "/GameCatalog";
    // Token de sesión de juego de un solo uso (mod sundauth, ver
    // datos extra/Documentacion/sundauth-mod/README.md): se pide justo antes
    // de cada "Jugar", no solo al iniciar sesión en el launcher.
    String API_GAME_SESSION_TOKEN_ENDPOINT = API_BASE_URL + "/GameSessionToken";

    // ---- Autoactualización del propio launcher (ver SelfUpdateService) ----
    // Mismos nombres fijos que ya usa SunD/views/downloads/index.php: GitHub resuelve
    // "latest" contra la release marcada como tal en cada momento, así que publicar una
    // versión nueva nunca requiere cambiar esta URL.
    String GITHUB_RELEASE_LATEST_BASE_URL =
            "https://github.com/IvanFreireDacoba/SunDLauncher/releases/latest/download";
    String SELF_UPDATE_ASSET_WINDOWS = "SunDLauncher-windows-x64.zip";
    String SELF_UPDATE_ASSET_LINUX = "SunDLauncher-linux-x64.zip";
    String SELF_UPDATE_ASSET_MACOS = "SunDLauncher-macos.zip";

    // ---- APIs públicas de terceros (no requieren cuenta) ----
    String MOJANG_VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    String MOJANG_RESOURCES_BASE_URL = "https://resources.download.minecraft.net";
    String FABRIC_META_BASE_URL = "https://meta.fabricmc.net/v2/versions/loader";
    String FABRIC_MAVEN_BASE_URL = "https://maven.fabricmc.net/";
    // Mods/resourcepacks no se redistribuyen: se resuelven en el momento de instalar contra esta API.
    String MODRINTH_API_BASE_URL = "https://api.modrinth.com/v2";

    // ---- Red ----
    int CONNECT_TIMEOUT_SECONDS = 8;
    int REQUEST_TIMEOUT_SECONDS = 10;

    // ---- Interfaz gráfica ----
    double MAIN_WINDOW_SCREEN_RATIO = 0.60; // tamaño inicial: 60% de la pantalla (la ventana es redimensionable, esto no es un límite)
    String BACKGROUND_IMAGE_RESOURCE = "/images/background.png";
    // Icono de la ventana/barra de tareas. Placeholder generado, sustituible por el mismo nombre. Ver IcoImageLoader.
    String WINDOW_ICON_RESOURCE = "/images/SunDLauncher.ico";

    // ---- Selección de instancias (Step 2) ----
    // %d se sustituye por el id de la instancia (el mismo data_val del catálogo).
    String INSTANCE_BACKGROUND_RESOURCE_PATTERN = "/images/instances/%d/background.png";
    // Miniatura para la columna vertical de instancias. Si todavía no existe un
    // logo.png dedicado para una instancia, InstanceIcons cae a background.png
    // escalado en vez de dejar la fila sin icono.
    String INSTANCE_LOGO_RESOURCE_PATTERN = "/images/instances/%d/logo.png";
    // Mismo par de imágenes (solo icono, sin texto) reutilizado en todas las instancias.
    String BUTTON_INSTALL_IMAGE_RESOURCE = "/images/buttons/install.png";
    String BUTTON_PLAY_IMAGE_RESOURCE = "/images/buttons/play.png";

    // ---- JVM para lanzar Minecraft ----
    String DEFAULT_JVM_MAX_MEMORY = "3G";
}
