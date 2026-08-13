package es.sund.launcher.security;

import es.sund.launcher.config.AppPaths;
import es.sund.launcher.util.OwnerOnlyFiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Escribe el token de sesión de juego de un solo uso que recoge el mod
 * sundauth del cliente de Minecraft nada más arrancar (y borra en cuanto lo
 * lee, ver datos extra/Documentacion/sundauth-mod/README.md). Vive en la
 * raíz de la instancia, junto a los demás marcadores internos del pipeline
 * de instalación (.instance_pack.sha1, .managed_mods.json), con permisos
 * restringidos al propietario -mismo patrón que EncryptedFileCredentialStore,
 * vía OwnerOnlyFiles-.
 *
 * El contenido es texto plano (no cifrado, a diferencia de credentials.dat):
 * el token vive segundos, no meses, y solo lo puede usar quien ya tenga
 * acceso de lectura a la carpeta de la instancia -mismo nivel de exposición
 * que cualquier otro fichero de configuración de ahí-, así que cifrarlo no
 * añadiría protección real.
 */
public final class GameSessionTokenFile {

    public static final String FILE_NAME = ".sund_session_token";

    private GameSessionTokenFile() {}

    public static void write(AppPaths.InstancePaths instancePaths, String token, String minecraftUsername)
            throws IOException {
        File file = new File(instancePaths.root, FILE_NAME);
        String content = token + "\n" + minecraftUsername + "\n";
        OwnerOnlyFiles.writeOwnerOnly(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Borra el token si sigue en disco. El mod cliente lo borra en cuanto lo lee, pero eso
     * solo ocurre si el jugador llega a conectarse a un servidor con sundauth -si juega en
     * un solo jugador, el servidor está caído, o Minecraft se cierra antes de conectar, el
     * token de la partida anterior se quedaría en disco en texto plano hasta el siguiente
     * "Jugar"-. Se llama tanto antes de pedir uno nuevo como al terminar el proceso del juego.
     */
    public static void deleteIfExists(AppPaths.InstancePaths instancePaths) {
        try {
            Files.deleteIfExists(new File(instancePaths.root, FILE_NAME).toPath());
        } catch (IOException ignored) {
            // Best-effort de limpieza: un fallo aquí no debe impedir lanzar/cerrar el juego.
        }
    }
}
