package es.sund.launcher.minecraft;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.InstallationException;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.model.ModpackDefinition;
import es.sund.launcher.model.ModrinthVersionResponse;
import es.sund.launcher.model.ResourcepackDefinition;
import es.sund.launcher.util.DownloadUtil;
import es.sund.launcher.util.DownloadUtil.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Instala el contenido propio de una instancia que no viene de Mojang/Fabric:
 *
 * 1) instancePackUrl: zip con config/resources/... de la instancia, se extrae
 *    íntegro sobre su carpeta. Idempotente si el backend manda instancePackSha1
 *    (se guarda en un fichero marcador y no se vuelve a aplicar si no cambia).
 * 2) modpackJsonUrl / resourcepackJsonUrl: listas de mods/resourcepacks que
 *    referencian una versión de Modrinth (no se redistribuyen los .jar/.zip
 *    directamente), resueltas contra la API pública de Modrinth y descargadas
 *    en mods/ y resourcepacks/ respectivamente.
 *
 * Todo lo de aquí es idempotente y seguro de repetir en cada lanzamiento.
 */
public class InstanceContentInstaller {

    private static final String INSTANCE_PACK_MARKER_FILE = ".instance_pack.sha1";
    private static final String MANAGED_MODS_MARKER_FILE = ".managed_mods.json";
    private static final String MODPACK_JSON_MARKER_FILE = ".modpack_json.sha1";
    private static final String RESOURCEPACK_JSON_MARKER_FILE = ".resourcepack_json.sha1";

    private final AppPaths.InstancePaths paths;
    private final ProgressListener listener;
    private final ModrinthClient modrinthClient = new ModrinthClient();
    private final Gson gson = new Gson();

    public InstanceContentInstaller(AppPaths.InstancePaths paths, ProgressListener listener) {
        this.paths = paths;
        this.listener = listener;
    }

    /**
     * Hash del instance-pack que ya está aplicado en disco, o null si nunca se aplicó
     * ninguno todavía. Usado también por InstanceInstallStatus para decidir, sin tocar
     * la red, si el botón debe ofrecer "Actualizar" (el marcador no coincide con el
     * instancePackSha1 recién obtenido de GameCatalog).
     */
    public static String readAppliedInstancePackSha1(AppPaths.InstancePaths paths) {
        return readMarker(paths, INSTANCE_PACK_MARKER_FILE);
    }

    /** Mismo propósito que readAppliedInstancePackSha1 pero para modpack.json (lista de mods). */
    public static String readAppliedModpackJsonSha1(AppPaths.InstancePaths paths) {
        return readMarker(paths, MODPACK_JSON_MARKER_FILE);
    }

    /** Mismo propósito que readAppliedInstancePackSha1 pero para la lista de resourcepacks. */
    public static String readAppliedResourcepackJsonSha1(AppPaths.InstancePaths paths) {
        return readMarker(paths, RESOURCEPACK_JSON_MARKER_FILE);
    }

    private static String readMarker(AppPaths.InstancePaths paths, String markerFileName) {
        Path marker = new File(paths.root, markerFileName).toPath();
        if (!Files.exists(marker)) {
            return null;
        }
        try {
            return Files.readString(marker, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return null;
        }
    }

    public void install(GameInstance instance) throws InstallationException {
        try {
            installInstancePack(instance);
            installMods(instance);
            installResourcepacks(instance);
        } catch (IOException | InterruptedException e) {
            throw new InstallationException("No se pudo instalar el contenido de " + instance.name + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            // Cualquier error inesperado (p.ej. una URL mal formada que llega del backend)
            // debe llegar a la UI como fallo de instalación en vez de matar en silencio el
            // hilo "instance-launch-worker" y dejar el launcher colgado sin ningún aviso.
            throw new InstallationException("No se pudo instalar el contenido de " + instance.name + ": " + e.getMessage(), e);
        }
    }

    private void installInstancePack(GameInstance instance) throws IOException, InterruptedException {
        if (instance.instancePackUrl == null) {
            return;
        }

        Path marker = new File(paths.root, INSTANCE_PACK_MARKER_FILE).toPath();
        String appliedSha1 = readAppliedInstancePackSha1(paths);
        if (instance.instancePackSha1 != null && instance.instancePackSha1.equalsIgnoreCase(appliedSha1)) {
            return; // ya aplicado, nada que hacer
        }

        Path zipTarget = new File(paths.root, "instance-pack.zip").toPath();
        DownloadUtil.downloadFile(instance.instancePackUrl, zipTarget, instance.instancePackSha1,
                "Contenido de " + instance.name, listener);

        // Se extrae primero a una carpeta temporal (en vez de directamente sobre
        // paths.root) para poder tratar servers.dat de forma especial: todo lo
        // demás (config/mods/...) se sigue sobrescribiendo tal cual como antes,
        // pero servers.dat se fusiona con el del jugador en vez de pisárselo
        // entero (ver applyInstancePackContents/ServerListMerger).
        File extractionTmp = new File(paths.root, ".instance_pack_tmp");
        DownloadUtil.deleteRecursive(extractionTmp.toPath());
        DownloadUtil.unzip(zipTarget.toFile(), extractionTmp);
        applyInstancePackContents(extractionTmp);
        DownloadUtil.deleteRecursive(extractionTmp.toPath());

        Files.deleteIfExists(zipTarget);

        if (instance.instancePackSha1 != null) {
            Files.writeString(marker, instance.instancePackSha1, StandardCharsets.UTF_8);
        }
    }

    /**
     * Copia el contenido ya extraído del instance-pack sobre la carpeta de la
     * instancia. Todo se sobrescribe tal cual (mismo comportamiento de
     * siempre) salvo servers.dat, que en vez de pisar la lista de servidores
     * del jugador se fusiona con ella (ver ServerListMerger): se actualiza en
     * el sitio la entrada que gestiona SunD (identificada por su "name") y se
     * preserva cualquier servidor que el jugador haya añadido por su cuenta.
     */
    private void applyInstancePackContents(File extractionRoot) throws IOException {
        Path extractionRootPath = extractionRoot.toPath();
        Path targetRootPath = paths.root.toPath();

        Files.walkFileTree(extractionRootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path source, BasicFileAttributes attrs) throws IOException {
                Path relative = extractionRootPath.relativize(source);
                Path target = targetRootPath.resolve(relative);
                Files.createDirectories(target.getParent());

                if (relative.toString().equals("servers.dat")) {
                    ServerListMerger.mergeInto(source, target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void installMods(GameInstance instance) throws IOException, InterruptedException, InstallationException {
        if (instance.modpackJsonUrl == null) {
            return;
        }
        Path modpackMarker = new File(paths.root, MODPACK_JSON_MARKER_FILE).toPath();
        if (instance.modpackJsonSha1 != null
                && instance.modpackJsonSha1.equalsIgnoreCase(readMarker(paths, MODPACK_JSON_MARKER_FILE))) {
            return; // la lista de mods no cambió desde la última instalación, nada que resolver
        }

        String raw = DownloadUtil.getString(instance.modpackJsonUrl);
        ModpackDefinition modpack = gson.fromJson(raw, ModpackDefinition.class);
        if (modpack == null || modpack.mods == null) {
            return;
        }

        Path modsRoot = paths.modsDir.toPath().normalize();
        List<String> managedFileNames = new ArrayList<>();
        for (ModpackDefinition.ModEntry mod : modpack.mods) {
            String fileName;
            if (mod.directUrl != null) {
                // Mods fuera de Modrinth (p.ej. FTB, vía CurseForge): el backend ya resolvió
                // el enlace real de descarga al generar modpack.json, aquí solo se descarga.
                fileName = mod.fileName != null ? mod.fileName : new File(mod.directUrl).getName();
                Path dest = DownloadUtil.resolveChild(modsRoot, fileName);
                DownloadUtil.downloadFile(mod.directUrl, dest, mod.sha1, "Mod " + fileName, listener);
            } else {
                ModrinthVersionResponse.ModrinthFile file = modrinthClient.resolvePrimaryFile(mod.modrinthVersionId);
                fileName = mod.fileName != null ? mod.fileName : file.filename;
                String sha1 = mod.sha1 != null ? mod.sha1 : (file.hashes != null ? file.hashes.sha1 : null);
                Path dest = DownloadUtil.resolveChild(modsRoot, fileName);
                DownloadUtil.downloadFile(file.url, dest, sha1, "Mod " + fileName, listener);
            }
            managedFileNames.add(fileName);
        }

        removeUnmanagedMods(managedFileNames);

        if (instance.modpackJsonSha1 != null) {
            Files.writeString(modpackMarker, instance.modpackJsonSha1, StandardCharsets.UTF_8);
        }
    }

    /**
     * Borra de mods/ los ficheros que una instalación anterior de este mismo modpack.json
     * gestionaba pero que ya no aparecen en la lista actual (p.ej. un admin quitó un mod
     * del catálogo). Se distingue de "cualquier archivo suelto en mods/" con un marcador
     * propio (MANAGED_MODS_MARKER_FILE) para no tocar nunca automodpack.jar (viene del
     * instance-pack, no de aquí) ni ningún mod que el jugador haya añadido por su cuenta.
     */
    private void removeUnmanagedMods(List<String> currentFileNames) throws IOException {
        Path marker = new File(paths.root, MANAGED_MODS_MARKER_FILE).toPath();
        if (Files.exists(marker)) {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> previousFileNames = gson.fromJson(
                    Files.readString(marker, StandardCharsets.UTF_8), listType);
            if (previousFileNames != null) {
                Path modsRoot = paths.modsDir.toPath().normalize();
                for (String fileName : previousFileNames) {
                    if (!currentFileNames.contains(fileName)) {
                        Files.deleteIfExists(DownloadUtil.resolveChild(modsRoot, fileName));
                    }
                }
            }
        }
        Files.writeString(marker, gson.toJson(currentFileNames), StandardCharsets.UTF_8);
    }

    private void installResourcepacks(GameInstance instance) throws IOException, InterruptedException, InstallationException {
        if (instance.resourcepackJsonUrl == null) {
            return;
        }
        Path resourcepackMarker = new File(paths.root, RESOURCEPACK_JSON_MARKER_FILE).toPath();
        if (instance.resourcepackJsonSha1 != null
                && instance.resourcepackJsonSha1.equalsIgnoreCase(readMarker(paths, RESOURCEPACK_JSON_MARKER_FILE))) {
            return; // la lista de resourcepacks no cambió desde la última instalación, nada que resolver
        }

        String raw = DownloadUtil.getString(instance.resourcepackJsonUrl);
        ResourcepackDefinition resourcepacks = gson.fromJson(raw, ResourcepackDefinition.class);
        if (resourcepacks == null || resourcepacks.resourcePacks == null) {
            return;
        }

        Path resourcepacksRoot = paths.resourcepacksDir.toPath().normalize();
        List<String> orderedFileNames = new ArrayList<>();
        for (ResourcepackDefinition.ResourcepackEntry pack : resourcepacks.resourcePacks) {
            ModrinthVersionResponse.ModrinthFile file =
                    modrinthClient.resolveResourcepackFile(pack.source, pack.version, instance.mcVersion);
            String sha1 = file.hashes != null ? file.hashes.sha1 : null;
            Path dest = DownloadUtil.resolveChild(resourcepacksRoot, file.filename);
            DownloadUtil.downloadFile(file.url, dest, sha1, "Resourcepack " + pack.name, listener);
            orderedFileNames.add(file.filename);
        }

        applyResourcepackOrder(orderedFileNames);

        if (instance.resourcepackJsonSha1 != null) {
            Files.writeString(resourcepackMarker, instance.resourcepackJsonSha1, StandardCharsets.UTF_8);
        }
    }

    /**
     * Deja los resourcepacks de la instancia activos y en el orden dado (el orden importa:
     * cada entrada posterior se aplica por encima/sobrescribe a las anteriores, igual que
     * en el menú de resourcepacks de Minecraft). Se escribe directamente en options.txt en
     * vez de depender de que el jugador los active a mano.
     *
     * No se pisa la lista entera: se preserva cualquier resourcepack que el jugador haya
     * añadido por su cuenta, solo se reordenan/insertan los que gestiona esta instancia.
     */
    private void applyResourcepackOrder(List<String> fileNames) throws IOException {
        File optionsFile = new File(paths.root, "options.txt");
        List<String> lines = optionsFile.exists()
                ? new ArrayList<>(Files.readAllLines(optionsFile.toPath(), StandardCharsets.UTF_8))
                : new ArrayList<>();

        List<String> currentPacks = readResourcePacksList(lines);

        List<String> managedEntries = new ArrayList<>();
        for (String fileName : fileNames) {
            managedEntries.add("file/" + fileName);
        }
        currentPacks.removeAll(managedEntries);
        currentPacks.addAll(managedEntries);
        if (!currentPacks.contains("vanilla")) {
            currentPacks.add(0, "vanilla");
        }

        String newLine = "resourcePacks:" + gson.toJson(currentPacks);

        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("resourcePacks:")) {
                lines.set(i, newLine);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lines.add(newLine);
        }

        Files.write(optionsFile.toPath(), lines, StandardCharsets.UTF_8);
    }

    private List<String> readResourcePacksList(List<String> optionsLines) {
        for (String line : optionsLines) {
            if (line.startsWith("resourcePacks:")) {
                try {
                    Type listType = new TypeToken<List<String>>() {}.getType();
                    List<String> parsed = gson.fromJson(line.substring("resourcePacks:".length()), listType);
                    if (parsed != null) {
                        return new ArrayList<>(parsed);
                    }
                } catch (Exception malformed) {
                    // Línea corrupta o con un formato inesperado: se reconstruye desde cero.
                }
            }
        }
        List<String> defaultList = new ArrayList<>();
        defaultList.add("vanilla");
        return defaultList;
    }
}
