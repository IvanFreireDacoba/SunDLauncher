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
 *
 * type: discriminador añadido para instancias que no son Minecraft (p.ej.
 * "pocketcrossing"). null o "minecraft" siguen significando lo mismo que
 * antes de que este campo existiera (pipeline Minecraft/Fabric de siempre,
 * ver GameSessionStarter/InstanceInstallStatus) — un backend que todavía no
 * mande este campo no rompe nada. Cualquier otro valor usa en su lugar el
 * pipeline genérico de NativeGameInstaller/NativeGameLauncher, que solo
 * necesita instancePackUrl/instancePackSha1 (el paquete completo del
 * cliente) y opcionalmente connectUrl.
 *
 * connectUrl: solo relevante para instancias no-Minecraft — dirección
 * (host:puerto o URL) del servidor de juego al que debe conectarse el
 * cliente nativo, para no tener que hardcodearla en NativeGameLauncher.
 */
public class GameInstance {
    public int id;
    public String name;
    // Carpeta local de la instancia (ver AppPaths.forInstance()): "<folder>_instance",
    // p.ej. "SunDOrigins_instance". Reemplaza al antiguo nombrado por id numérico
    // ("2"/"3"), ilegible para el jugador si mira la carpeta de datos del launcher.
    public String folder;
    public String type;
    public String mcVersion;
    public String fabricLoaderVersion;
    public String instancePackUrl;
    public String instancePackSha1;
    public String modpackJsonUrl;
    public String modpackJsonSha1;
    public String resourcepackJsonUrl;
    public String resourcepackJsonSha1;
    public String connectUrl;

    /** false para null/"minecraft" (pipeline de siempre), true para cualquier otro tipo. */
    public boolean isNative() {
        return type != null && !type.equals("minecraft");
    }
}
