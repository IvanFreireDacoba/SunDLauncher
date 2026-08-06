package es.sund.launcher.minecraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import es.sund.launcher.config.AppConstants;
import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.InstallationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Une la versión vanilla + el profile de Fabric (si lo hay) y lanza el proceso Java.
 * Login offline: el uuid lo decide OfflineUUID, no hay accessToken real de Mojang.
 */
public class GameLauncher {

    public Process launch(AppPaths.InstancePaths paths, String versionId, JsonObject vanillaJson, JsonObject fabricJson,
                           String username, String uuid) throws InstallationException {
        try {
            List<String> classpath = new ArrayList<>();
            collectLibraryPaths(paths, vanillaJson, classpath);
            if (fabricJson != null) {
                collectLibraryPaths(paths, fabricJson, classpath);
            }
            File clientJar = new File(paths.versionsDir, versionId + "/" + versionId + ".jar");
            classpath.add(clientJar.getAbsolutePath());

            String mainClass = fabricJson != null
                    ? fabricJson.get("mainClass").getAsString()
                    : vanillaJson.get("mainClass").getAsString();

            String assetIndexId = vanillaJson.getAsJsonObject("assetIndex").get("id").getAsString();
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-Djava.library.path=" + paths.nativesDir.getAbsolutePath());
            command.add("-Dminecraft.launcher.brand=" + AppConstants.LAUNCHER_NAME);
            command.add("-Dminecraft.launcher.version=" + AppConstants.CURRENT_LAUNCHER_VERSION);
            command.add("-Xmx" + AppConstants.DEFAULT_JVM_MAX_MEMORY);
            command.add("-cp");
            command.add(String.join(File.pathSeparator, classpath));
            command.add(mainClass);

            command.add("--username"); command.add(username);
            command.add("--version"); command.add(versionId);
            command.add("--gameDir"); command.add(paths.root.getAbsolutePath());
            command.add("--assetsDir"); command.add(paths.assetsDir.getAbsolutePath());
            command.add("--assetIndex"); command.add(assetIndexId);
            command.add("--uuid"); command.add(uuid);
            command.add("--accessToken"); command.add("0");
            command.add("--userType"); command.add("legacy");
            command.add("--versionType"); command.add("release");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(paths.root);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            return pb.start();

        } catch (IOException e) {
            throw new InstallationException("No se pudo lanzar Minecraft: " + e.getMessage(), e);
        }
    }

    private void collectLibraryPaths(AppPaths.InstancePaths paths, JsonObject versionJson, List<String> out) {
        if (!versionJson.has("libraries")) return;
        for (JsonElement el : versionJson.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            if (lib.has("downloads") && lib.getAsJsonObject("downloads").has("artifact")) {
                String path = lib.getAsJsonObject("downloads").getAsJsonObject("artifact").get("path").getAsString();
                out.add(new File(paths.librariesDir, path).getAbsolutePath());
            } else if (lib.has("name")) {
                String[] parts = lib.get("name").getAsString().split(":");
                String group = parts[0].replace(".", "/");
                String artifact = parts[1];
                String version = parts[2];
                String path = group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
                out.add(new File(paths.librariesDir, path).getAbsolutePath());
            }
        }
    }
}
