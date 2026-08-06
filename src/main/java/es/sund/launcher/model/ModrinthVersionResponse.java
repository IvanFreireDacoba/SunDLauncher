package es.sund.launcher.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Subconjunto de la respuesta de GET https://api.modrinth.com/v2/version/{id}
 * (o de un elemento del array que devuelve GET .../project/{slug}/version)
 * que necesitamos para descargar el fichero real de un mod/resourcepack.
 */
public class ModrinthVersionResponse {
    @SerializedName("version_number")
    public String versionNumber;

    @SerializedName("game_versions")
    public List<String> gameVersions;

    public List<ModrinthFile> files;

    public static class ModrinthFile {
        public String url;
        public String filename;
        public boolean primary;
        public ModrinthHashes hashes;
    }

    public static class ModrinthHashes {
        public String sha1;
        public String sha512;
    }
}
