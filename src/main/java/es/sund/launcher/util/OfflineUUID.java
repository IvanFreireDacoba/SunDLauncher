package es.sund.launcher.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Genera un UUID offline exactamente igual que lo hace el cliente vanilla de Minecraft
 * cuando el servidor está en online-mode=false, y como hacen TLauncher/SKLauncher.
 * Es determinista: el mismo username siempre da el mismo UUID.
 */
public final class OfflineUUID {

    public static UUID generate(String username) {
        String source = "OfflinePlayer:" + username;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private OfflineUUID() {}
}
