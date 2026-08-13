package es.sund.launcher.service;

import es.sund.launcher.config.AppPaths;
import es.sund.launcher.minecraft.FabricInstaller;
import es.sund.launcher.minecraft.InstanceContentInstaller;
import es.sund.launcher.minecraft.MinecraftInstaller;
import es.sund.launcher.model.GameInstance;

/** Comprueba en disco si una instancia (vanilla + Fabric) ya está instalada, sin tocar la red. */
public final class InstanceInstallStatus {

    public static boolean isInstalled(GameInstance instance) {
        AppPaths.InstancePaths paths = AppPaths.forInstance(instance.id);
        return MinecraftInstaller.isInstalled(paths, instance.mcVersion)
                && FabricInstaller.isInstalled(paths, instance.mcVersion, instance.fabricLoaderVersion);
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
        AppPaths.InstancePaths paths = AppPaths.forInstance(instance.id);
        return isStale(instance.instancePackUrl, instance.instancePackSha1,
                    InstanceContentInstaller.readAppliedInstancePackSha1(paths))
                || isStale(instance.modpackJsonUrl, instance.modpackJsonSha1,
                    InstanceContentInstaller.readAppliedModpackJsonSha1(paths))
                || isStale(instance.resourcepackJsonUrl, instance.resourcepackJsonSha1,
                    InstanceContentInstaller.readAppliedResourcepackJsonSha1(paths));
    }

    /**
     * remoteSha1 == null significa que el backend no manda hash para este contenido: no se
     * puede confirmar que siga igual, así que se trata como "desactualizado" (conservador) en
     * vez de asumir que no cambió nada.
     */
    private static boolean isStale(String url, String remoteSha1, String appliedSha1) {
        if (url == null) {
            return false;
        }
        if (remoteSha1 == null) {
            return true;
        }
        return !remoteSha1.equalsIgnoreCase(appliedSha1);
    }

    private InstanceInstallStatus() {}
}
