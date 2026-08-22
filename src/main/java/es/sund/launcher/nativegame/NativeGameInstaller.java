package es.sund.launcher.nativegame;

import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.InstallationException;
import es.sund.launcher.minecraft.InstanceContentInstaller;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.util.DownloadUtil;
import es.sund.launcher.util.DownloadUtil.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Instala una instancia que NO es Minecraft (ver GameInstance.isNative()): descarga
 * instancePackUrl (el paquete completo del cliente, no solo config/resources como en
 * InstanceContentInstaller) y lo extrae íntegro sobre la carpeta de la instancia. Sin
 * vanilla/Fabric, sin mods/resourcepacks vía Modrinth — el paquete ya trae todo lo que
 * el cliente necesita para arrancar.
 *
 * Reutiliza el mismo fichero marcador que InstanceContentInstaller
 * (INSTANCE_PACK_MARKER_FILE) para que InstanceInstallStatus.isUpdateAvailable() no
 * necesite ninguna rama por tipo de instancia: ya compara instancePackUrl/Sha1 contra
 * ese marcador de forma genérica.
 */
public class NativeGameInstaller {

    /** Nombre de fichero que debe traer el paquete de cliente en su raíz para poder lanzarse. */
    public static final String CLIENT_JAR_NAME = "client.jar";

    private final AppPaths.InstancePaths paths;
    private final ProgressListener listener;

    public NativeGameInstaller(AppPaths.InstancePaths paths, ProgressListener listener) {
        this.paths = paths;
        this.listener = listener;
    }

    public static boolean isInstalled(AppPaths.InstancePaths paths) {
        return new File(paths.root, CLIENT_JAR_NAME).isFile();
    }

    public void install(GameInstance instance) throws InstallationException {
        if (instance.instancePackUrl == null) {
            throw new InstallationException(
                    "La instancia " + instance.name + " no tiene paquete de cliente configurado (instancePackUrl).");
        }
        try {
            String appliedSha1 = InstanceContentInstaller.readAppliedInstancePackSha1(paths);
            if (isInstalled(paths) && instance.instancePackSha1 != null
                    && instance.instancePackSha1.equalsIgnoreCase(appliedSha1)) {
                return; // ya aplicado, nada que hacer
            }

            Path zipTarget = new File(paths.root, "client-pack.zip").toPath();
            DownloadUtil.downloadFile(instance.instancePackUrl, zipTarget, instance.instancePackSha1,
                    "Cliente de " + instance.name, listener);
            DownloadUtil.unzip(zipTarget.toFile(), paths.root);
            Files.deleteIfExists(zipTarget);

            if (!isInstalled(paths)) {
                throw new IOException("El paquete de cliente de " + instance.name
                        + " no contiene " + CLIENT_JAR_NAME + " en su raíz.");
            }

            if (instance.instancePackSha1 != null) {
                Path marker = new File(paths.root, InstanceContentInstaller.INSTANCE_PACK_MARKER_FILE).toPath();
                Files.writeString(marker, instance.instancePackSha1, StandardCharsets.UTF_8);
            }
        } catch (IOException | InterruptedException e) {
            throw new InstallationException("No se pudo instalar " + instance.name + ": " + e.getMessage(), e);
        }
    }
}
