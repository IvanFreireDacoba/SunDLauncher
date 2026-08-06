package es.sund.launcher.model;

import java.util.List;

/**
 * Respuesta esperada de GET /APIs/GameCatalog.
 * Ajusta los nombres de campo si tu backend usa otros distintos (Gson mapea por nombre).
 */
public class GameCatalogResponse {
    public boolean success;
    public String message;      // motivo de error si success = false
    public List<GameInstance> instances;
}
