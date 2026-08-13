package es.sund.launcher.service;

import com.google.gson.JsonObject;
import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.InstallationException;
import es.sund.launcher.minecraft.FabricInstaller;
import es.sund.launcher.minecraft.GameLauncher;
import es.sund.launcher.minecraft.InstanceContentInstaller;
import es.sund.launcher.minecraft.MinecraftInstaller;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.model.GameSessionTokenResponse;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.security.GameSessionTokenFile;
import es.sund.launcher.security.StoredCredentials;
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
    private final SunDApiService apiService;
    private final CredentialStore credentialStore;

    public GameSessionStarter(GameInstance instance, ProgressListener progressListener,
            SunDApiService apiService, CredentialStore credentialStore) {
        this.instance = instance;
        this.instancePaths = AppPaths.forInstance(instance.id);
        this.minecraftInstaller = new MinecraftInstaller(instancePaths, progressListener);
        this.fabricInstaller = new FabricInstaller(instancePaths, progressListener);
        this.contentInstaller = new InstanceContentInstaller(instancePaths, progressListener);
        this.apiService = apiService;
        this.credentialStore = credentialStore;
    }

    /** Instala lo que falte y lanza Minecraft con una cuenta offline para el username dado. */
    public Process start(String username) throws InstallationException {
        JsonObject vanillaJson = minecraftInstaller.install(instance.mcVersion); // idempotente
        JsonObject fabricJson = fabricInstaller.install(instance.mcVersion, instance.fabricLoaderVersion); // idempotente
        contentInstaller.install(instance); // config/resources propios + mods/resourcepacks vía Modrinth, idempotente

        attemptWriteSessionToken(username);

        String uuid = OfflineUUID.generate(username).toString();

        return gameLauncher.launch(instancePaths, instance.mcVersion, vanillaJson, fabricJson, username, uuid);
    }

    /**
     * Pide un token de sesión de juego de un solo uso (mod sundauth, ver
     * datos extra/Documentacion/sundauth-mod/README.md) y lo deja en un fichero
     * local para que lo recoja el mod cliente. Deliberadamente "best effort": un
     * fallo aquí (red caída, backend caído, credenciales no disponibles) NUNCA
     * debe impedir lanzar el juego -mientras dure el despliegue en paralelo con
     * EasyAuth (rollout elegido por el usuario), el mod es una comprobación
     * añadida, no todavía la única puerta de entrada-.
     */
    private void attemptWriteSessionToken(String username) {
        try {
            StoredCredentials credentials = credentialStore.load();
            if (credentials == null) {
                return;
            }
            GameSessionTokenResponse tokenResponse =
                    apiService.requestGameSessionToken(username, credentials.getPassword());
            if (tokenResponse.success) {
                GameSessionTokenFile.write(instancePaths, tokenResponse.token, tokenResponse.minecraftUsername);
            }
        } catch (Exception e) {
            // Ver comentario del método: nunca debe propagar y bloquear el lanzamiento.
        }
    }
}
