package es.sund.launcher.model;

/**
 * Respuesta esperada de GET /APIs/CheckLauncherVersion.
 * Ajusta los nombres de campo si tu backend usa otros distintos (Gson mapea por nombre).
 */
public class VersionCheckResponse {
    public String latestVersion;       // ej. "1.1"
    public boolean forceConfigUpdate;  // si true, se borran y re-descargan config/assets/resourcepacks
    public String configPackUrl;       // URL de un .zip con config/assets/resourcepacks actualizados
    public String configPackSha1;      // hash del configPackUrl; sin él, LauncherUpdateService rechaza aplicarlo
    public String launcherDownloadUrl; // URL del nuevo instalador/jar del launcher
}
