package es.sund.launcher.model;

/**
 * Respuesta de POST /APIs/GameSessionToken. Token de un solo uso, de vida
 * corta, que el launcher escribe en un fichero local para que lo recoja el
 * mod sundauth del cliente de Minecraft (ver
 * datos extra/Documentacion/sundauth-mod/README.md). Se pide de nuevo en
 * cada "Jugar", no solo al iniciar sesión.
 */
public class GameSessionTokenResponse {
    public boolean success;
    public String message;
    public boolean blocked;
    public Integer retryAfterSeconds;

    public String token;               // solo si success = true
    public String minecraftUsername;   // mismo string que se pasará como --username
    public Integer expiresInSeconds;
}
