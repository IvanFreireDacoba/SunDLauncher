package es.sund.launcher.model;

import java.util.List;

/**
 * Respuesta esperada de GET {GameInstance.resourcepackJsonUrl} (JSON estático).
 * A diferencia de ModpackDefinition, aquí no se resuelve nada de antemano en
 * el backend: cada entrada solo dice qué proyecto de Modrinth y qué versión
 * (por su version_number, tal cual aparece en Modrinth) usar. El propio
 * launcher resuelve el fichero real en el momento de instalar
 * (ver {@link es.sund.launcher.minecraft.ModrinthClient}), así que nunca se
 * aloja el .zip del resourcepack en el servidor.
 */
public class ResourcepackDefinition {
    public List<ResourcepackEntry> resourcePacks;

    public static class ResourcepackEntry {
        public String name;     // informativo (progreso, mensajes de error)
        public String source;   // URL de Modrinth al proyecto, p.ej. https://modrinth.com/resourcepack/avalon-32x
        public String version;  // version_number de Modrinth a instalar
    }
}
