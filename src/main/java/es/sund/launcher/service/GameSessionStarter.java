package es.sund.launcher.service;

import com.google.gson.JsonObject;
import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.InstallationException;
import es.sund.launcher.minecraft.FabricInstaller;
import es.sund.launcher.minecraft.GameLauncher;
import es.sund.launcher.minecraft.InstanceContentInstaller;
import es.sund.launcher.minecraft.MinecraftInstaller;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.util.DownloadUtil.ProgressListener;
import es.sund.launcher.util.OfflineUUID;

/**
 * Encapsula el flujo completo de "asegurar que el juego está instalado y lanzarlo"
 * para una instancia concreta (SunD Origins, CobbleSpain, ...), para que las clases
 * de Action de la UI no tengan que conocer los detalles de
 * MinecraftInstaller/FabricInstaller/GameLauncher.
 */
public class GameSessionStarter {

    private final GameInstance instance;
    private final AppPaths.InstancePaths instancePaths;
    private final MinecraftInstaller minecraftInstaller;
    private final FabricInstaller fabricInstaller;
    private final InstanceContentInstaller contentInstaller;
    private final GameLauncher gameLauncher = new GameLauncher();

    public GameSessionStarter(GameInstance instance, ProgressListener progressListener) {
        this.instance = instance;
        this.instancePaths = AppPaths.forInstance(instance.id);
        this.minecraftInstaller = new MinecraftInstaller(instancePaths, progressListener);
        this.fabricInstaller = new FabricInstaller(instancePaths, progressListener);
        this.contentInstaller = new InstanceContentInstaller(instancePaths, progressListener);
    }

    /** Instala lo que falte y lanza Minecraft con una cuenta offline para el username dado. */
    public Process start(String username) throws InstallationException {
        JsonObject vanillaJson = minecraftInstaller.install(instance.mcVersion); // idempotente
        JsonObject fabricJson = fabricInstaller.install(instance.mcVersion, instance.fabricLoaderVersion); // idempotente
        contentInstaller.install(instance); // config/resources propios + mods/resourcepacks vía Modrinth, idempotente

        String uuid = OfflineUUID.generate(username).toString();

        return gameLauncher.launch(instancePaths, instance.mcVersion, vanillaJson, fabricJson, username, uuid);
    }
}
