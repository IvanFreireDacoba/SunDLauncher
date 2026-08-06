package es.sund.launcher.model;

import java.util.List;

/**
 * Respuesta esperada de GET {GameInstance.modpackJsonUrl} (un JSON estático,
 * no un endpoint de tu API). Formato inspirado en la lista de ficheros de
 * AutoModpack: en vez de distribuir los .jar de los mods directamente, cada
 * entrada apunta a dónde resolverlo en el momento de instalar.
 *
 * directUrl tiene prioridad si está presente: se usa para mods que no están
 * en Modrinth (p.ej. FTB, distribuido vía CurseForge) y cuyo enlace de
 * descarga real se resolvió en el momento de generar este JSON (en el
 * backend, contra la API de CurseForge con su fingerprint/murmur) — nunca
 * apunta a un fichero alojado en sund.es, solo al CDN original del mod.
 * Si directUrl es null, se resuelve modrinthVersionId contra Modrinth.
 */
public class ModpackDefinition {
    public String modpackName;
    public List<ModEntry> mods;

    public static class ModEntry {
        public String fileName;           // nombre de fichero destino en mods/ (si es null, se usa el resuelto)
        public String modrinthProjectId;  // informativo, no se usa para descargar
        public String modrinthVersionId;  // se resuelve contra la API de Modrinth si directUrl es null
        public String directUrl;          // enlace de descarga ya resuelto (p.ej. CDN de CurseForge), opcional
        public String sha1;               // verificación de la descarga (obligatoria en la práctica si hay directUrl)
        public boolean required = true;
    }
}
