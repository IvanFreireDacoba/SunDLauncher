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

/**
 * Descarga e instala una versión vanilla de Minecraft usando las APIs públicas
 * de Mojang (piston-meta). No requieren autenticación: son las mismas que usan
 * TLauncher, HMCL o Prism. La autenticación de CUENTA es un tema aparte y no
 * interviene aquí.
 */
public class MinecraftInstaller {

    private final AppPaths.InstancePaths paths;
    private final ProgressListener listener;

    public MinecraftInstaller(AppPaths.InstancePaths paths, ProgressListener listener) {
        this.paths = paths;
        this.listener = listener;
    }

    public static boolean isInstalled(AppPaths.InstancePaths paths, String versionId) {
        File versionJar = new File(paths.versionsDir, versionId + "/" + versionId + ".jar");
        File versionJson = new File(paths.versionsDir, versionId + "/" + versionId + ".json");
        return versionJar.exists() && versionJson.exists();
    }

    /** Devuelve el JsonObject del version.json ya instalado (o recién descargado). Idempotente. */
    public JsonObject install(String versionId) throws InstallationException {
        try {
            String manifestRaw = DownloadUtil.getString(AppConstants.MOJANG_VERSION_MANIFEST_URL);
            JsonObject manifest = JsonParser.parseString(manifestRaw).getAsJsonObject();

            String versionJsonUrl = null;
            for (JsonElement el : manifest.getAsJsonArray("versions")) {
                JsonObject v = el.getAsJsonObject();
                if (v.get("id").getAsString().equals(versionId)) {
                    versionJsonUrl = v.get("url").getAsString();
                    break;
                }
            }
            if (versionJsonUrl == null) {
                throw new InstallationException("Versión " + versionId + " no encontrada en el manifest de Mojang");
            }

            String versionJsonRaw = DownloadUtil.getString(versionJsonUrl);
            JsonObject versionJson = JsonParser.parseString(versionJsonRaw).getAsJsonObject();

            File versionDir = new File(paths.versionsDir, versionId);
            versionDir.mkdirs();
            Files.writeString(new File(versionDir, versionId + ".json").toPath(), versionJsonRaw);

            JsonObject clientDl = versionJson.getAsJsonObject("downloads").getAsJsonObject("client");
            String clientUrl = clientDl.get("url").getAsString();
            String clientSha1 = clientDl.get("sha1").getAsString();
            Path clientJar = new File(versionDir, versionId + ".jar").toPath();
            DownloadUtil.downloadFile(clientUrl, clientJar, clientSha1, "Cliente " + versionId, listener);

            installLibraries(versionJson);
            installAssets(versionJson);

            return versionJson;

        } catch (IOException | InterruptedException e) {
            throw new InstallationException("No se pudo instalar Minecraft " + versionId + ": " + e.getMessage(), e);
        }
    }

    private void installLibraries(JsonObject versionJson) throws IOException, InterruptedException {
        for (JsonElement el : versionJson.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            if (!ruleAllows(lib)) continue;
            if (!lib.has("downloads")) continue;
            JsonObject downloads = lib.getAsJsonObject("downloads");

            if (downloads.has("artifact")) {
                downloadLibraryArtifact(downloads.getAsJsonObject("artifact"));
            }
            if (downloads.has("classifiers") && lib.has("natives")) {
                JsonObject natives = lib.getAsJsonObject("natives");
                String osKey = currentOsKey();
                if (natives.has(osKey)) {
                    String classifierKey = natives.get(osKey).getAsString();
                    JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                    if (classifiers.has(classifierKey)) {
                        Path nativeJar = downloadLibraryArtifact(classifiers.getAsJsonObject(classifierKey));
                        extractNatives(nativeJar);
                    }
                }
            }
        }
    }

    private Path downloadLibraryArtifact(JsonObject artifact) throws IOException, InterruptedException {
        String path = artifact.get("path").getAsString();
        String url = artifact.get("url").getAsString();
        String sha1 = artifact.has("sha1") ? artifact.get("sha1").getAsString() : null;
        Path dest = new File(paths.librariesDir, path).toPath();
        DownloadUtil.downloadFile(url, dest, sha1, "Librería " + dest.getFileName(), listener);

        if (path.contains("natives")) {
            extractNatives(dest);
        }
        return dest;
    }

    private void extractNatives(Path jarPath) throws IOException {
        paths.nativesDir.mkdirs();
        try (var zip = new java.util.zip.ZipFile(jarPath.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("META-INF")) continue;
                File out = new File(paths.nativesDir, new File(entry.getName()).getName());
                try (var in = zip.getInputStream(entry); var os = Files.newOutputStream(out.toPath())) {
                    in.transferTo(os);
                }
            }
        }
    }

    private void installAssets(JsonObject versionJson) throws IOException, InterruptedException {
        JsonObject assetIndexInfo = versionJson.getAsJsonObject("assetIndex");
        String indexId = assetIndexInfo.get("id").getAsString();
        String indexUrl = assetIndexInfo.get("url").getAsString();

        File indexesDir = new File(paths.assetsDir, "indexes");
        indexesDir.mkdirs();
        String indexRaw = DownloadUtil.getString(indexUrl);
        Files.writeString(new File(indexesDir, indexId + ".json").toPath(), indexRaw);

        JsonObject index = JsonParser.parseString(indexRaw).getAsJsonObject();
        JsonObject objects = index.getAsJsonObject("objects");

        File objectsDir = new File(paths.assetsDir, "objects");
        for (String key : objects.keySet()) {
            JsonObject obj = objects.getAsJsonObject(key);
            String hash = obj.get("hash").getAsString();
            String sub = hash.substring(0, 2);
            String url = AppConstants.MOJANG_RESOURCES_BASE_URL + "/" + sub + "/" + hash;
            Path dest = new File(objectsDir, sub + "/" + hash).toPath();
            DownloadUtil.downloadFile(url, dest, hash, "Asset " + key, listener);
        }
    }

    private boolean ruleAllows(JsonObject lib) {
        if (!lib.has("rules")) return true;
        boolean allowed = false;
        for (JsonElement r : lib.getAsJsonArray("rules")) {
            JsonObject rule = r.getAsJsonObject();
            String action = rule.get("action").getAsString();
            boolean matches = true;
            if (rule.has("os")) {
                String osName = rule.getAsJsonObject("os").has("name")
                        ? rule.getAsJsonObject("os").get("name").getAsString() : null;
                matches = osName == null || osName.equals(currentOsKey());
            }
            if (matches) allowed = action.equals("allow");
        }
        return allowed;
    }

    private static String currentOsKey() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }
}
