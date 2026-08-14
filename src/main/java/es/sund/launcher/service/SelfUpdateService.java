package es.sund.launcher.service;

import es.sund.launcher.config.AppConstants;
import es.sund.launcher.exception.LauncherUpdateException;
import es.sund.launcher.util.DownloadUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Descarga la release "Latest" del propio launcher directamente desde GitHub y la aplica
 * sobre la instalación en marcha, sin abrir ningún navegador ni depender de la web. Un
 * mecanismo distinto por sistema operativo, porque cada uno empaqueta de forma distinta
 * (ver packaging/build-{linux,windows,macos}.*):
 *
 * - Linux (AppImage): el fichero se puede sustituir en sitio mientras el proceso sigue
 *   corriendo (Linux no bloquea un ejecutable en uso), así que el reemplazo y el relanzamiento
 *   pasan enteros por este mismo proceso Java, sin ayudante externo.
 * - Windows/macOS: el ejecutable/`.app` en marcha SÍ está bloqueado mientras corre, así que
 *   hace falta un script ayudante desacoplado que espere a que este proceso termine (PID),
 *   aplique la actualización (instalador `/quiet` en Windows, copia de la `.app` en macOS) y
 *   relance la app — este proceso solo lo lanza y sale.
 *
 * Las tres instalaciones son "por usuario, sin admin" (ver --win-per-user-install y el cambio
 * de macOS a --type app-image, ambos del 2026-08-14): sin eso, relanzar el instalador/copiar
 * la app habría disparado un permiso de administrador del sistema en mitad de la actualización.
 *
 * Si el proceso actual no parece una instalación empaquetada real (p.ej. se ejecuta desde un
 * IDE o `java -jar` suelto, caso de desarrollo), no se hace nada destructivo: se devuelve
 * NOT_APPLICABLE para que el llamador caiga al enlace de descarga manual de siempre.
 */
public class SelfUpdateService {

    public enum Result {
        /** La nueva versión ya se descargó y se lanzó (o se dejó en marcha un ayudante que la aplicará); este proceso debe salir ya. */
        APPLIED_WILL_RESTART,
        /** No se detectó una instalación empaquetada real (modo desarrollo): nada que autoactualizar. */
        NOT_APPLICABLE
    }

    public Result performSelfUpdate(DownloadUtil.ProgressListener progress) throws LauncherUpdateException {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                return applyWindows(progress);
            } else if (os.contains("mac")) {
                return applyMacos(progress);
            } else {
                return applyLinux(progress);
            }
        } catch (IOException | InterruptedException e) {
            throw new LauncherUpdateException("No se pudo autoactualizar el launcher: " + e.getMessage(), e);
        }
    }

    private Result applyLinux(DownloadUtil.ProgressListener progress)
            throws IOException, InterruptedException, LauncherUpdateException {
        String appImagePathStr = System.getenv("APPIMAGE");
        if (appImagePathStr == null || appImagePathStr.isBlank()) {
            return Result.NOT_APPLICABLE;
        }
        Path currentAppImage = Path.of(appImagePathStr);

        Path workDir = Files.createTempDirectory("sundlauncher-selfupdate");
        Path zipPath = workDir.resolve("update.zip");
        DownloadUtil.downloadFile(
                AppConstants.GITHUB_RELEASE_LATEST_BASE_URL + "/" + AppConstants.SELF_UPDATE_ASSET_LINUX,
                zipPath, null, "Descargando SunD Launcher...", progress);

        Path extractDir = workDir.resolve("extracted");
        Files.createDirectories(extractDir);
        DownloadUtil.unzip(zipPath.toFile(), extractDir.toFile());

        Path newAppImage = findChildEndingWith(extractDir, ".appimage");
        if (newAppImage == null) {
            throw new LauncherUpdateException("El paquete descargado no contiene un AppImage válido.");
        }
        if (!newAppImage.toFile().setExecutable(true, false)) {
            throw new LauncherUpdateException("No se pudo marcar el AppImage descargado como ejecutable.");
        }

        // Reemplazo en dos pasos dentro del mismo directorio que el original (mismo
        // filesystem, para que el segundo movimiento sea un rename atómico de verdad):
        // Linux permite sustituir el fichero de un ejecutable en marcha sin problema, el
        // proceso actual sigue usando el inode antiguo hasta que termine.
        Path staged = currentAppImage.resolveSibling(currentAppImage.getFileName() + ".new");
        Files.move(newAppImage, staged, StandardCopyOption.REPLACE_EXISTING);
        Files.move(staged, currentAppImage, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        detachedProcess(currentAppImage.toString()).start();
        return Result.APPLIED_WILL_RESTART;
    }

    private Result applyWindows(DownloadUtil.ProgressListener progress)
            throws IOException, InterruptedException, LauncherUpdateException {
        String exePathStr = ProcessHandle.current().info().command().orElse(null);
        if (exePathStr == null || !exePathStr.toLowerCase().endsWith("sundlauncher.exe")) {
            return Result.NOT_APPLICABLE;
        }
        Path currentExe = Path.of(exePathStr);

        Path workDir = Files.createTempDirectory("sundlauncher-selfupdate");
        Path zipPath = workDir.resolve("update.zip");
        DownloadUtil.downloadFile(
                AppConstants.GITHUB_RELEASE_LATEST_BASE_URL + "/" + AppConstants.SELF_UPDATE_ASSET_WINDOWS,
                zipPath, null, "Descargando SunD Launcher...", progress);

        Path extractDir = workDir.resolve("extracted");
        Files.createDirectories(extractDir);
        DownloadUtil.unzip(zipPath.toFile(), extractDir.toFile());

        Path installerExe = findChildEndingWith(extractDir, ".exe");
        if (installerExe == null) {
            throw new LauncherUpdateException("El paquete descargado no contiene un instalador .exe válido.");
        }

        // Script ayudante: espera a que este proceso (PID) termine, relanza el instalador
        // en silencio (/quiet - instalación por usuario, sin UAC, ver build-windows.ps1) y
        // vuelve a abrir el launcher ya actualizado. No se puede hacer desde este mismo
        // proceso: el .exe en marcha tiene sus propios ficheros bloqueados hasta que termine.
        Path helperScript = workDir.resolve("apply-update.cmd");
        String script = String.join("\r\n",
                "@echo off",
                ":wait",
                "tasklist /FI \"PID eq %1\" 2>NUL | find /I \"%1\" >NUL",
                "if not errorlevel 1 (",
                "  ping -n 2 127.0.0.1 >NUL",
                "  goto wait",
                ")",
                "start /wait \"\" \"%~2\" /quiet",
                "start \"\" \"%~3\"",
                "del \"%~f0\"",
                "");
        Files.writeString(helperScript, script);

        long ourPid = ProcessHandle.current().pid();
        detachedProcess("cmd.exe", "/c", "start", "\"SunD Launcher - actualizando\"",
                helperScript.toString(), String.valueOf(ourPid), installerExe.toString(), currentExe.toString())
                .start();

        return Result.APPLIED_WILL_RESTART;
    }

    private Result applyMacos(DownloadUtil.ProgressListener progress)
            throws IOException, InterruptedException, LauncherUpdateException {
        String exePathStr = ProcessHandle.current().info().command().orElse(null);
        if (exePathStr == null || !exePathStr.contains(".app/Contents/MacOS/")) {
            return Result.NOT_APPLICABLE;
        }
        Path macosExe = Path.of(exePathStr);
        Path currentApp = macosExe.getParent() != null ? macosExe.getParent().getParent() : null; // MacOS -> Contents
        currentApp = currentApp != null ? currentApp.getParent() : null; // Contents -> SunDLauncher.app
        if (currentApp == null || !currentApp.getFileName().toString().endsWith(".app")) {
            return Result.NOT_APPLICABLE;
        }

        Path workDir = Files.createTempDirectory("sundlauncher-selfupdate");
        Path zipPath = workDir.resolve("update.zip");
        DownloadUtil.downloadFile(
                AppConstants.GITHUB_RELEASE_LATEST_BASE_URL + "/" + AppConstants.SELF_UPDATE_ASSET_MACOS,
                zipPath, null, "Descargando SunD Launcher...", progress);

        Path extractDir = workDir.resolve("extracted");
        Files.createDirectories(extractDir);
        DownloadUtil.unzip(zipPath.toFile(), extractDir.toFile());

        Path newApp = findChildEndingWith(extractDir, ".app");
        if (newApp == null) {
            throw new LauncherUpdateException("El paquete descargado no contiene una SunDLauncher.app válida.");
        }

        // Mismo motivo que en Windows: la .app en marcha no se puede sustituir desde dentro
        // de sí misma, hace falta un script ayudante que espere a que este proceso (PID)
        // termine antes de tocar sus ficheros.
        Path helperScript = workDir.resolve("apply-update.sh");
        String script = String.join("\n",
                "#!/bin/sh",
                "PID=\"$1\"",
                "NEW_APP=\"$2\"",
                "CURRENT_APP=\"$3\"",
                "while kill -0 \"$PID\" 2>/dev/null; do sleep 0.3; done",
                "OLD_APP=\"$CURRENT_APP.old.$$\"",
                "mv \"$CURRENT_APP\" \"$OLD_APP\" 2>/dev/null",
                "cp -R \"$NEW_APP\" \"$CURRENT_APP\"",
                "rm -rf \"$OLD_APP\"",
                "open -n \"$CURRENT_APP\"",
                "rm -f \"$0\"",
                "");
        Files.writeString(helperScript, script);
        if (!helperScript.toFile().setExecutable(true, false)) {
            throw new LauncherUpdateException("No se pudo preparar el script de actualización.");
        }

        long ourPid = ProcessHandle.current().pid();
        detachedProcess("/bin/sh", helperScript.toString(),
                String.valueOf(ourPid), newApp.toString(), currentApp.toString())
                .start();

        return Result.APPLIED_WILL_RESTART;
    }

    /** Busca el primer hijo directo de dir cuyo nombre termine en extension (sin distinguir mayúsculas). */
    private static Path findChildEndingWith(Path dir, String extension) throws IOException {
        try (Stream<Path> children = Files.list(dir)) {
            return children
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(extension))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Proceso desacoplado de este JVM (no hereda salida/error), para que sobreviva a
     * nuestro System.exit() sin quedarse colgado de una consola que va a desaparecer.
     * La entrada se deja como PIPE por defecto (no existe un Redirect.DISCARD válido
     * para lectura, solo para escritura): nadie escribe en ese pipe y se cierra solo
     * al salir este proceso, así que no bloquea nada.
     */
    private static ProcessBuilder detachedProcess(String... command) {
        return new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD);
    }
}
