package es.sund.launcher.model;

/**
 * Una instancia de juego del catálogo (SunD Origins, CobbleSpain, ...), tal y
 * como la devuelve GET /APIs/GameCatalog. El campo id es el mismo data_val
 * que ya usa el backend para identificar productos (p.ej. data_val=1 para el
 * propio launcher en CheckLauncherVersion).
 *
 * instancePackUrl/instancePackSha1: zip con el contenido estático de la
 * instancia (config, resources, ...) que se descarga y extrae íntegro sobre
 * la carpeta de la instancia. instancePackSha1 es opcional: si el backend no
 * lo manda, el pack se vuelve a aplicar en cada instalación (no idempotente).
 *
 * modpackJsonUrl/resourcepackJsonUrl: JSON estático con la lista de mods y
 * resourcepacks a resolver contra la API de Modrinth en el momento de
 * instalar (ver ModpackDefinition/ResourcepackDefinition). Pueden ser null
 * si esa instancia no tiene mods/resourcepacks adicionales.
 *
 * modpackJsonSha1/resourcepackJsonSha1: hash del JSON tal cual (no de los
 * mods que resuelve), igual que instancePackSha1 pero para estas dos listas.
 * Permite a InstanceInstallStatus/InstanceContentInstaller saber si algo
 * cambió sin tener que volver a resolver cada mod/resourcepack contra
 * Modrinth: ese trabajo de red solo se repite al instalar/actualizar, nunca
 * en cada "Jugar" de una instancia ya al día.
 */
public class GameInstance {
    public int id;
    public String name;
    public String mcVersion;
    public String fabricLoaderVersion;
    public String instancePackUrl;
    public String instancePackSha1;
    public String modpackJsonUrl;
    public String modpackJsonSha1;
    public String resourcepackJsonUrl;
    public String resourcepackJsonSha1;
}
