package es.sund.launcher.service;

import es.sund.launcher.config.AppPaths;
import es.sund.launcher.minecraft.FabricInstaller;
import es.sund.launcher.minecraft.InstanceContentInstaller;
import es.sund.launcher.minecraft.MinecraftInstaller;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.nativegame.NativeGameInstaller;
import es.sund.launcher.ui.InstancePanel;

/** Comprueba en disco si una instancia (Minecraft o nativa, ver GameInstance.isNative()) ya está instalada, sin tocar la red. */
public final class InstanceInstallStatus {

    public static boolean isInstalled(GameInstance instance) {
        AppPaths.InstancePaths paths = AppPaths.forInstance(instance);
        if (instance.isNative()) {
            return NativeGameInstaller.isInstalled(paths);
        }
        return MinecraftInstaller.isInstalled(paths, instance.mcVersion)
                && FabricInstaller.isInstalled(paths, instance.mcVersion, instance.fabricLoaderVersion);
    }

    /** Instalada Y con el contenido al día: es el único caso en el que pulsar el botón debe lanzar el juego directamente. */
    public static boolean isReadyToPlay(GameInstance instance) {
        return isInstalled(instance) && !isUpdateAvailable(instance);
    }

    /**
     * Solo tiene sentido si la instancia ya está instalada. Compara, sin red, los hashes del
     * instance-pack/modpack.json/lista de resourcepacks (recién obtenidos de GameCatalog)
     * contra los marcadores locales que dejó la última vez que se aplicó cada uno (ver
     * InstanceContentInstaller). Si alguno no coincide, el contenido de la instancia está
     * desactualizado y el botón debe ofrecer "Actualizar" en vez de "Jugar" — y
     * GameSessionStarter usa este mismo resultado para decidir si hace falta volver a
     * instalar contenido en absoluto antes de lanzar, o si puede ir directo al "Jugar" sin
     * tocar la red (ver GameSessionStarter.start()).
     */
    public static boolean isUpdateAvailable(GameInstance instance) {
        AppPaths.InstancePaths paths = AppPaths.forInstance(instance);
        return isStale(instance.instancePackUrl, instance.instancePackSha256, instance.instancePackSha1,
                    InstanceContentInstaller.readAppliedInstancePackSha1(paths))
                || isStale(instance.modpackJsonUrl, instance.modpackJsonSha256, instance.modpackJsonSha1,
                    InstanceContentInstaller.readAppliedModpackJsonSha1(paths))
                || isStale(instance.resourcepackJsonUrl, instance.resourcepackJsonSha256, instance.resourcepackJsonSha1,
                    InstanceContentInstaller.readAppliedResourcepackJsonSha1(paths));
    }

    /**
     * Compara, sin red, el hash del catálogo contra el marcador en disco (ver
     * InstanceContentInstaller.markerMatches, que tolera que el marcador sea SHA-1 —launcher
     * viejo— o SHA-256 —launcher nuevo— mientras el catálogo sirva ambos durante la transición).
     * Si el backend no manda NINGÚN hash (ni sha256 ni sha1) para un contenido que sí existe, no
     * se puede confirmar que siga igual, así que se trata como "desactualizado" (conservador) en
     * vez de asumir que no cambió nada.
     */
    private static boolean isStale(String url, String remoteSha256, String remoteSha1, String appliedMarker) {
        if (url == null) {
            return false;
        }
        if ((remoteSha256 == null || remoteSha256.isBlank()) && (remoteSha1 == null || remoteSha1.isBlank())) {
            return true;
        }
        return !InstanceContentInstaller.markerMatches(appliedMarker, remoteSha256, remoteSha1);
    }

    /**
     * Recalcula el estado en disco y refleja en el panel tanto el texto ("No instalado" /
     * "Actualización disponible" / "Instalado") como el botón correspondiente. Único punto
     * donde se construye ese texto: usado tanto al construir la pantalla de instancias
     * (Main.bindInstances) como después de instalar/actualizar o de un fallo
     * (GameLaunchCoordinator/PlayOrInstallAction), para que el estado mostrado sea siempre
     * el que hay de verdad en disco, no el que había antes de la acción.
     */
    public static void refreshPanel(InstancePanel panel, GameInstance instance) {
        boolean installed = isInstalled(instance);
        boolean updateAvailable = installed && isUpdateAvailable(instance);

        StringBuilder details = new StringBuilder();
        if (instance.isNative()) {
            details.append(instance.name);
        } else {
            details.append("Minecraft ").append(instance.mcVersion);
            if (instance.fabricLoaderVersion != null && !instance.fabricLoaderVersion.isBlank()) {
                details.append(" · Fabric ").append(instance.fabricLoaderVersion);
            }
        }
        details.append(" · ").append(!installed ? "No instalado" : updateAvailable ? "Actualización disponible" : "Instalado");
        panel.setInstanceInfo(instance.name, details.toString());

        if (updateAvailable) {
            panel.showUpdateAvailable();
        } else if (installed) {
            panel.showInstalled();
        } else {
            panel.showNotInstalled();
        }
    }

    private InstanceInstallStatus() {}
}
