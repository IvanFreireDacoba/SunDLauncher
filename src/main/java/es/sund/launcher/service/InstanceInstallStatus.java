package es.sund.launcher.service;

import es.sund.launcher.config.AppPaths;
import es.sund.launcher.minecraft.FabricInstaller;
import es.sund.launcher.minecraft.MinecraftInstaller;
import es.sund.launcher.model.GameInstance;

/** Comprueba en disco si una instancia (vanilla + Fabric) ya está instalada, sin tocar la red. */
public final class InstanceInstallStatus {

    public static boolean isInstalled(GameInstance instance) {
        AppPaths.InstancePaths paths = AppPaths.forInstance(instance.id);
        return MinecraftInstaller.isInstalled(paths, instance.mcVersion)
                && FabricInstaller.isInstalled(paths, instance.mcVersion, instance.fabricLoaderVersion);
    }

    private InstanceInstallStatus() {}
}
