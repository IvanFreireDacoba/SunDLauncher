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

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

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

    /**
     * Instala lo que falte (vanilla, Fabric, contenido propio) SIN lanzar el juego. Usado
     * cuando el botón pulsado era "Instalar"/"Actualizar" (ver GameLaunchCoordinator): el
     * jugador puede poner a instalar varias instancias a la vez, y no debe encontrarse con
     * que Minecraft se abre solo en cuanto cada una termina.
     */
    public void ensureInstalled() throws InstallationException {
        minecraftInstaller.install(instance.mcVersion); // idempotente
        fabricInstaller.install(instance.mcVersion, instance.fabricLoaderVersion); // idempotente
        if (!InstanceInstallStatus.isInstalled(instance) || InstanceInstallStatus.isUpdateAvailable(instance)) {
            contentInstaller.install(instance);
        }
    }

    /** Instala lo que falte y lanza Minecraft con una cuenta offline para el username dado. */
    public Process start(String username) throws InstallationException {
        // Limpia un token de una partida anterior que el mod nunca llegó a leer (crash, sin
        // conexión al servidor, un solo jugador) antes de pedir uno nuevo.
        GameSessionTokenFile.deleteIfExists(instancePaths);

        JsonObject vanillaJson = minecraftInstaller.install(instance.mcVersion); // idempotente
        JsonObject fabricJson = fabricInstaller.install(instance.mcVersion, instance.fabricLoaderVersion); // idempotente

        // El contenido propio de la instancia (instance-pack, mods y resourcepacks vía
        // Modrinth/CurseForge) solo se resuelve si hace falta de verdad: la instancia no está
        // instalada todavía, o algo cambió desde la última vez (ver
        // InstanceInstallStatus.isUpdateAvailable, que compara hashes sin tocar la red). En
        // cualquier otro "Jugar" de una instancia ya al día este paso se salta entero -antes se
        // llamaba en cada partida, y volvía a resolver cada mod contra Modrinth aunque nada
        // hubiera cambiado, solo para acabar sin hacer ninguna descarga real-.
        if (!InstanceInstallStatus.isInstalled(instance) || InstanceInstallStatus.isUpdateAvailable(instance)) {
            contentInstaller.install(instance);
        }

        attemptWriteSessionToken(username);

        String uuid = OfflineUUID.generate(username).toString();

        Process process = gameLauncher.launch(instancePaths, instance.mcVersion, vanillaJson, fabricJson, username, uuid);
        // Mismo motivo que el borrado de arriba: si esta partida tampoco llega a conectarse a un
        // servidor con el mod, el token no debe sobrevivir en disco más allá de la propia partida.
        process.onExit().thenRun(() -> {
            GameSessionTokenFile.deleteIfExists(instancePaths);
            pruneOldLogs();
        });
        return process;
    }

    /**
     * Minecraft rota logs/latest.log a un fichero nuevo (logs/AAAA-MM-DD-N.log.gz) en cada
     * partida, sin borrar nunca los anteriores — con el tiempo, basura acumulada sin límite en
     * cada instancia. Tras cada partida, conserva solo los MAX_LOG_FILES_KEPT más recientes
     * (por fecha de modificación) y borra el resto. Best-effort a propósito: un fallo aquí
     * (permisos, fichero bloqueado) no debe afectar a nada más del launcher.
     */
    private static final int MAX_LOG_FILES_KEPT = 2;

    private void pruneOldLogs() {
        File logsDir = new File(instancePaths.root, "logs");
        File[] logFiles = logsDir.listFiles(f ->
                f.isFile() && (f.getName().endsWith(".log") || f.getName().endsWith(".log.gz")));
        if (logFiles == null || logFiles.length <= MAX_LOG_FILES_KEPT) {
            return;
        }
        Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = MAX_LOG_FILES_KEPT; i < logFiles.length; i++) {
            logFiles[i].delete();
        }
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
