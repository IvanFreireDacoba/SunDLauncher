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
     * Solo tiene sentido si la instancia ya está instalada. Compara, sin red, el hash del
     * instance-pack (instancePackSha1, recién obtenido de GameCatalog) contra el marcador
     * local que dejó la última vez que se aplicó (ver InstanceContentInstaller). Si no
     * coincide, el contenido de la instancia (config/mods/servers.dat...) está desactualizado
     * y el botón debe ofrecer "Actualizar" en vez de "Jugar".
     */
    public static boolean isUpdateAvailable(GameInstance instance) {
        if (instance.instancePackUrl == null || instance.instancePackSha1 == null) {
            return false;
        }
        AppPaths.InstancePaths paths = AppPaths.forInstance(instance.id);
        String appliedSha1 = InstanceContentInstaller.readAppliedInstancePackSha1(paths);
        return !instance.instancePackSha1.equalsIgnoreCase(appliedSha1);
    }

    private InstanceInstallStatus() {}
}
