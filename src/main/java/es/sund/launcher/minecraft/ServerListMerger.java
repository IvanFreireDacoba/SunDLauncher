package es.sund.launcher.minecraft;

import es.sund.launcher.minecraft.NbtServersFile.NbtList;
import es.sund.launcher.minecraft.NbtServersFile.Tag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fusiona el servers.dat que trae el instance-pack (con la dirección de SunD
 * ya pineada con su fingerprint de AutoModpack, ver GameCatalog.php) con el
 * servers.dat real del jugador, preservando cualquier servidor que este haya
 * añadido por su cuenta -igual que ya se hace con los resourcepacks en
 * options.txt (ver InstanceContentInstaller.applyResourcepackOrder), en vez
 * de pisar la lista entera cada vez que cambia el instance-pack.
 *
 * El servidor gestionado por SunD se identifica por su "name": si el jugador
 * ya tenía una entrada con ese nombre se actualiza en el mismo sitio (no se
 * reordena su lista), si no, se añade al final.
 *
 * Si algo falla leyendo el servers.dat existente (formato inesperado,
 * corrupto, versión de Minecraft con campos que este lector simplificado no
 * entiende...) se cae de vuelta a usar tal cual el servers.dat del
 * instance-pack, igual que se hacía antes de tener esta fusión: nunca se deja
 * la instalación a medias por esto.
 */
public final class ServerListMerger {

    private static final String SERVERS_KEY = "servers";
    private static final String NAME_KEY = "name";

    public static void mergeInto(Path incomingServersDat, Path targetServersDat) throws IOException {
        if (!Files.exists(targetServersDat)) {
            Files.copy(incomingServersDat, targetServersDat, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        try {
            Map<String, Tag> incomingRoot = NbtServersFile.readCompoundFile(incomingServersDat);
            Map<String, Tag> existingRoot = NbtServersFile.readCompoundFile(targetServersDat);

            List<Object> merged = mergeEntries(extractServerList(existingRoot), extractServerList(incomingRoot));

            Map<String, Tag> mergedRoot = new LinkedHashMap<>(existingRoot);
            mergedRoot.put(SERVERS_KEY, new Tag(NbtServersFile.TAG_LIST, new NbtList(NbtServersFile.TAG_COMPOUND, merged)));

            NbtServersFile.writeCompoundFile(targetServersDat, mergedRoot);
        } catch (RuntimeException | IOException unexpected) {
            Files.copy(incomingServersDat, targetServersDat, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<Object> extractServerList(Map<String, Tag> root) {
        Tag serversTag = root.get(SERVERS_KEY);
        if (serversTag == null || !(serversTag.value() instanceof NbtList list)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.elements());
    }

    private static List<Object> mergeEntries(List<Object> existing, List<Object> incoming) {
        List<Object> result = new ArrayList<>(existing);

        for (Object incomingEntryObj : incoming) {
            @SuppressWarnings("unchecked")
            Map<String, Tag> incomingEntry = (Map<String, Tag>) incomingEntryObj;
            String incomingName = nameOf(incomingEntry);

            int existingIndex = indexOfByName(result, incomingName);
            if (existingIndex >= 0) {
                result.set(existingIndex, incomingEntry);
            } else {
                result.add(incomingEntry);
            }
        }
        return result;
    }

    private static int indexOfByName(List<Object> entries, String name) {
        if (name == null) {
            return -1;
        }
        for (int i = 0; i < entries.size(); i++) {
            @SuppressWarnings("unchecked")
            Map<String, Tag> entry = (Map<String, Tag>) entries.get(i);
            if (name.equals(nameOf(entry))) {
                return i;
            }
        }
        return -1;
    }

    private static String nameOf(Map<String, Tag> entry) {
        Tag nameTag = entry.get(NAME_KEY);
        return nameTag != null && nameTag.value() instanceof String s ? s : null;
    }

    private ServerListMerger() {}
}
