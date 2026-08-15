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
            String raw = fetchProfileJson(mcVersion, loaderVersion);
            JsonObject profile = JsonParser.parseString(raw).getAsJsonObject();

            String id = fabricVersionId(mcVersion, loaderVersion);
            File dir = new File(paths.versionsDir, id);
            dir.mkdirs();
            Files.writeString(new File(dir, id + ".json").toPath(), raw);

            for (JsonElement el : profile.getAsJsonArray("libraries")) {
                JsonObject lib = el.getAsJsonObject();
                String name = lib.get("name").getAsString();
                boolean fabricMaven = !lib.has("url");
                String baseUrl = fabricMaven ? AppConstants.FABRIC_MAVEN_BASE_URL : lib.get("url").getAsString();
                String path = mavenNameToPath(name);
                String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + path;
                Path dest = new File(paths.librariesDir, path).toPath();
                downloadLibrary(url, dest, name, fabricMaven, path);
            }

            return profile;

        } catch (IOException | InterruptedException e) {
            throw new InstallationException("No se pudo instalar Fabric " + loaderVersion + ": " + e.getMessage(), e);
        }
    }

    /** Pide el profile JSON a meta.fabricmc.net, con reintento contra el mirror meta2 si el
     *  primero no responde (fallo real visto: rutas de ISP que bloquean el rango de Cloudflare
     *  donde cae meta.fabricmc.net, mientras que meta2 resuelve a una IP distinta y sí conecta). */
    private String fetchProfileJson(String mcVersion, String loaderVersion) throws IOException, InterruptedException {
        String suffix = "/" + mcVersion + "/" + loaderVersion + "/profile/json";
        try {
            return DownloadUtil.getString(AppConstants.FABRIC_META_BASE_URL + suffix);
        } catch (IOException primaryFailure) {
            return DownloadUtil.getString(AppConstants.FABRIC_META_BASE_URL_FALLBACK + suffix);
        }
    }

    /** Igual que fetchProfileJson pero para una librería del Maven de Fabric: solo tiene sentido
     *  reintentar contra maven2 cuando la URL venía del propio FABRIC_MAVEN_BASE_URL (fabricMaven),
     *  no cuando el profile apunta a un repositorio Maven de terceros. */
    private void downloadLibrary(String url, Path dest, String name, boolean fabricMaven, String path)
            throws IOException, InterruptedException {
        try {
            DownloadUtil.downloadFile(url, dest, null, "Fabric lib " + name, listener);
        } catch (IOException primaryFailure) {
            if (!fabricMaven) throw primaryFailure;
            String fallbackUrl = AppConstants.FABRIC_MAVEN_BASE_URL_FALLBACK + path;
            DownloadUtil.downloadFile(fallbackUrl, dest, null, "Fabric lib " + name, listener);
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
