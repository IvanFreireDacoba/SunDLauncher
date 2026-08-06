package es.sund.launcher.minecraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import es.sund.launcher.config.AppConstants;
import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.InstallationException;
import es.sund.launcher.util.DownloadUtil;
import es.sund.launcher.util.DownloadUtil.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Instala Fabric Loader sobre una versión vanilla ya instalada, usando meta.fabricmc.net. */
public class FabricInstaller {

    private final AppPaths.InstancePaths paths;
    private final ProgressListener listener;

    public FabricInstaller(AppPaths.InstancePaths paths, ProgressListener listener) {
        this.paths = paths;
        this.listener = listener;
    }

    public static String fabricVersionId(String mcVersion, String loaderVersion) {
        return "fabric-loader-" + loaderVersion + "-" + mcVersion;
    }

    public static boolean isInstalled(AppPaths.InstancePaths paths, String mcVersion, String loaderVersion) {
        String id = fabricVersionId(mcVersion, loaderVersion);
        File jsonFile = new File(paths.versionsDir, id + "/" + id + ".json");
        return jsonFile.exists();
    }

    public JsonObject install(String mcVersion, String loaderVersion) throws InstallationException {
        try {
            String profileUrl = AppConstants.FABRIC_META_BASE_URL + "/" + mcVersion + "/" + loaderVersion + "/profile/json";
            String raw = DownloadUtil.getString(profileUrl);
            JsonObject profile = JsonParser.parseString(raw).getAsJsonObject();

            String id = fabricVersionId(mcVersion, loaderVersion);
            File dir = new File(paths.versionsDir, id);
            dir.mkdirs();
            Files.writeString(new File(dir, id + ".json").toPath(), raw);

            for (JsonElement el : profile.getAsJsonArray("libraries")) {
                JsonObject lib = el.getAsJsonObject();
                String name = lib.get("name").getAsString();
                String baseUrl = lib.has("url") ? lib.get("url").getAsString() : AppConstants.FABRIC_MAVEN_BASE_URL;
                String path = mavenNameToPath(name);
                String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + path;
                Path dest = new File(paths.librariesDir, path).toPath();
                DownloadUtil.downloadFile(url, dest, null, "Fabric lib " + name, listener);
            }

            return profile;

        } catch (IOException | InterruptedException e) {
            throw new InstallationException("No se pudo instalar Fabric " + loaderVersion + ": " + e.getMessage(), e);
        }
    }

    private static String mavenNameToPath(String mavenName) {
        String[] parts = mavenName.split(":");
        String group = parts[0].replace(".", "/");
        String artifact = parts[1];
        String version = parts[2];
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
    }
}
