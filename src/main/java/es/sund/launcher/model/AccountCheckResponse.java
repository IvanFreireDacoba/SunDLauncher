package es.sund.launcher.model;

/**
 * Respuesta esperada de POST /APIs/CheckServerAccount.
 * Ajusta los nombres de campo si tu backend usa otros distintos (Gson mapea por nombre).
 */
public class AccountCheckResponse {
    public boolean success;
    public String message;      // motivo de error si success = false
    public String displayName;  // nombre "oficial" del jugador en tu sistema (opcional)
}
