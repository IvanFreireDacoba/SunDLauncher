package es.sund.launcher.minecraft;

import com.google.gson.Gson;
import es.sund.launcher.config.AppConstants;
import es.sund.launcher.exception.InstallationException;
import es.sund.launcher.model.ModrinthVersionResponse;
import es.sund.launcher.util.DownloadUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Resuelve mods/resourcepacks a su fichero descargable real contra la API
 * pública de Modrinth. Nada de esto se aloja en sund.es: solo se referencia
 * un identificador (hash de fichero, o proyecto+versión) y aquí se pregunta
 * a Modrinth por la URL y el hash reales en el momento de instalar.
 */
public class ModrinthClient {

    private final Gson gson = new Gson();

    /** Mods: modpack.json ya trae el modrinthVersionId resuelto de antemano (por sha1, al generar el modpack). */
    public ModrinthVersionResponse.ModrinthFile resolvePrimaryFile(String versionId)
            throws IOException, InterruptedException, InstallationException {
        String url = AppConstants.MODRINTH_API_BASE_URL + "/version/" + versionId;
        String raw = DownloadUtil.getString(url);
        ModrinthVersionResponse response = gson.fromJson(raw, ModrinthVersionResponse.class);
        return pickPrimaryFile(response, versionId);
    }

    /**
     * Resourcepacks: resourcepack.json solo trae la URL del proyecto de Modrinth y
     * la versión deseada (version_number) — nada resuelto de antemano. Se listan
     * todas las versiones del proyecto, se filtra primero por compatibilidad real
     * con mcVersion (algunos resourcepacks reutilizan el mismo version_number en
     * builds para otra versión de Minecraft), y solo dentro de esas se busca la
     * que coincida con version_number; si ninguna coincide, se usa la más
     * reciente que sí sea compatible.
     */
    public ModrinthVersionResponse.ModrinthFile resolveResourcepackFile(String modrinthProjectUrl, String targetVersion, String mcVersion)
            throws IOException, InterruptedException, InstallationException {
        String slug = extractSlug(modrinthProjectUrl);
        String url = AppConstants.MODRINTH_API_BASE_URL + "/project/" + slug + "/version";
        String raw = DownloadUtil.getString(url);
        ModrinthVersionResponse[] versions = gson.fromJson(raw, ModrinthVersionResponse[].class);

        if (versions == null || versions.length == 0) {
            throw new InstallationException("El proyecto de Modrinth " + slug + " no tiene versiones publicadas");
        }

        List<ModrinthVersionResponse> compatible = new ArrayList<>();
        for (ModrinthVersionResponse version : versions) {
            if (version.gameVersions != null && version.gameVersions.contains(mcVersion)) {
                compatible.add(version);
            }
        }
        if (compatible.isEmpty()) {
            throw new InstallationException("Ninguna versión de " + slug + " es compatible con Minecraft " + mcVersion);
        }

        ModrinthVersionResponse chosen = compatible.get(0); // más reciente compatible, por si no hay coincidencia exacta
        if (targetVersion != null) {
            for (ModrinthVersionResponse version : compatible) {
                if (targetVersion.equals(version.versionNumber)) {
                    chosen = version;
                    break;
                }
            }
        }

        return pickPrimaryFile(chosen, slug);
    }

    /** Segmentos de tipo de proyecto que puede llevar una URL de Modrinth justo antes del slug. */
    private static final List<String> MODRINTH_PROJECT_TYPE_SEGMENTS =
            List.of("resourcepack", "mod", "modpack", "plugin", "shader", "datapack", "project");

    /**
     * Extrae el slug de una URL de proyecto de Modrinth. No asume que el slug es el
     * último segmento: algunas entradas de resourcepack.json enlazan directamente a
     * una versión concreta (p.ej. ".../resourcepack/<slug>/version/1.20B"), y tomar
     * el último segmento a ciegas devolvería "1.20B" en vez del slug real, rompiendo
     * la resolución contra la API (404 silencioso). En su lugar se busca el segmento
     * de tipo de proyecto conocido y se toma el siguiente; si no aparece ninguno
     * (URL ya es solo el slug, o un formato no contemplado), se cae al último
     * segmento como antes.
     */
    private static String extractSlug(String modrinthProjectUrl) {
        String trimmed = modrinthProjectUrl.replaceAll("/+$", "");
        String[] segments = trimmed.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if (MODRINTH_PROJECT_TYPE_SEGMENTS.contains(segments[i])) {
                return segments[i + 1];
            }
        }
        int lastSlash = trimmed.lastIndexOf('/');
        return lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
    }

    private static ModrinthVersionResponse.ModrinthFile pickPrimaryFile(ModrinthVersionResponse response, String context)
            throws InstallationException {
        if (response == null || response.files == null || response.files.isEmpty()) {
            throw new InstallationException("La versión de Modrinth resuelta para " + context + " no tiene ficheros descargables");
        }
        for (ModrinthVersionResponse.ModrinthFile file : response.files) {
            if (file.primary) {
                return file;
            }
        }
        return response.files.get(0);
    }
}
