package es.sund.launcher.service;

import es.sund.launcher.config.AppConstants;
import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.exception.ApiTimeoutException;
import es.sund.launcher.exception.LauncherUpdateException;
import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.model.VersionCheckResponse;
import es.sund.launcher.util.DownloadUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class LauncherUpdateService {

    private final SunDApiService apiService;

    public LauncherUpdateService(SunDApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Consulta la versión remota. No lanza excepción si simplemente no hay actualización
     * disponible: solo la lanza si falla la propia consulta (timeout / sin conexión).
     */
    public VersionCheckResponse checkRemoteVersion() throws ApiTimeoutException, ApiConnectionException {
        return apiService.checkLauncherVersion();
    }

    /** Compara la versión remota con la versión actual embebida en AppConstants. */
    public boolean isUpdateAvailable(VersionCheckResponse remote) {
        return remote != null
                && remote.latestVersion != null
                && !remote.latestVersion.equals(AppConstants.CURRENT_LAUNCHER_VERSION);
    }

    /**
     * Aplica la actualización: si el backend indica que hay que refrescar la configuración,
     * borra config/resourcepacks locales y descarga el paquete nuevo indicado por la API.
     */
    public void applyUpdate(VersionCheckResponse remote) throws LauncherUpdateException {
        if (remote.configPackUrl == null || !remote.forceConfigUpdate) {
            return; // nada que actualizar a nivel de ficheros, solo cambia el número de versión mostrado
        }
        // SEGURIDAD (auditoría 2026-08-13): antes se descargaba y extraía este zip sin
        // verificar ningún hash (DownloadUtil.downloadFile(..., null, ...) desactiva la
        // comprobación por completo) — un backend comprometido o una respuesta manipulada
        // podía hacer aplicar contenido arbitrario sobre config/resourcepacks sin ninguna
        // garantía de integridad. Ahora, sin configPackSha1 no se aplica nada: es mejor no
        // actualizar la config que aplicar un zip sin verificar su procedencia.
        if (remote.configPackSha1 == null || remote.configPackSha1.isBlank()) {
            throw new LauncherUpdateException(
                    "El servidor no proporcionó un hash de verificación (configPackSha1) para el paquete de configuración.");
        }
        try {
            DownloadUtil.deleteRecursive(AppPaths.CONFIG_DIR.toPath());
            DownloadUtil.deleteRecursive(AppPaths.RESOURCEPACKS_DIR.toPath());

            Path zipTarget = new File(AppPaths.ROOT_DIR, "update-pack.zip").toPath();
            DownloadUtil.downloadFile(remote.configPackUrl, zipTarget, remote.configPackSha1, "Paquete de actualización", null);
            DownloadUtil.unzip(zipTarget.toFile(), AppPaths.ROOT_DIR);
            zipTarget.toFile().delete();

        } catch (IOException | InterruptedException e) {
            throw new LauncherUpdateException("No se pudo aplicar la actualización: " + e.getMessage(), e);
        }
    }
}
