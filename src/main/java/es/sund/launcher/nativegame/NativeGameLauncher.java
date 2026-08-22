package es.sund.launcher.nativegame;

import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.InstallationException;
import es.sund.launcher.model.GameInstance;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lanza el proceso de un cliente nativo (no Minecraft) ya instalado por NativeGameInstaller:
 * `java -jar client.jar`, sin manifiestos de versión ni classpath que montar (a diferencia de
 * GameLauncher, que sí necesita todo eso para Minecraft/Fabric).
 *
 * El token de sesión de juego (GameSessionTokenFile) NUNCA se pasa aquí como argumento del
 * proceso -se vería en `ps`/Task Manager de cualquier otro usuario de la máquina-: ya lo dejó
 * escrito GameSessionStarter en <paths.root>/.sund_session_token antes de llamar a launch(), y
 * es el propio cliente quien debe leerlo y borrarlo al conectar.
 */
public class NativeGameLauncher {

    public Process launch(AppPaths.InstancePaths paths, GameInstance instance, String username)
            throws InstallationException {
        try {
            File clientJar = new File(paths.root, NativeGameInstaller.CLIENT_JAR_NAME);
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-jar");
            command.add(clientJar.getAbsolutePath());
            command.add("--username");
            command.add(username);
            if (instance.connectUrl != null && !instance.connectUrl.isBlank()) {
                command.add("--server");
                command.add(instance.connectUrl);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(paths.root);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            return pb.start();

        } catch (IOException e) {
            throw new InstallationException("No se pudo lanzar " + instance.name + ": " + e.getMessage(), e);
        }
    }
}
