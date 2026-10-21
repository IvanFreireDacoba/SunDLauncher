package es.sund.launcher.model;

/**
 * Respuesta esperada de GET /APIs/CheckLauncherVersion.
 * Ajusta los nombres de campo si tu backend usa otros distintos (Gson mapea por nombre).
 */
public class VersionCheckResponse {
    public String latestVersion;       // ej. "1.1"
    public boolean forceConfigUpdate;  // si true, se borran y re-descargan config/assets/resourcepacks
    public String configPackUrl;       // URL de un .zip con config/assets/resourcepacks actualizados
    public String configPackSha1;      // hash SHA-1 del configPackUrl (nuestro artefacto)
    // SHA-256 en paralelo a configPackSha1 (transición SHA-1 -> SHA-256 de nuestros
    // artefactos). Aditivo/opcional: LauncherUpdateService.applyUpdate() verifica con
    // SHA-256 si viene, y cae a configPackSha1 si no (retrocompatible con un backend
    // viejo). Sigue siendo fail-closed: sin ninguno de los dos, no aplica el paquete.
    public String configPackSha256;
    public String launcherDownloadUrl; // URL del nuevo instalador/jar del launcher
}
