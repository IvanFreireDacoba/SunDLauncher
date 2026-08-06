package es.sund.launcher.controller;

import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.exception.ApiTimeoutException;
import es.sund.launcher.model.GameCatalogResponse;
import es.sund.launcher.model.GameInstance;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pide en segundo plano el catálogo de instancias (GET /APIs/GameCatalog) para
 * InstanceSelectionFrame. Cualquier fallo de red o una respuesta de error del
 * backend se resuelve como onFailure, nunca lanzando una excepción sin capturar.
 */
public class InstanceCatalogController {

    private final SunDApiService apiService;

    public InstanceCatalogController(SunDApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * @param onLoaded  se ejecuta en el EDT con la lista de instancias si la consulta fue correcta
     * @param onFailure se ejecuta en el EDT con un mensaje legible si algo falló
     */
    public void load(Consumer<List<GameInstance>> onLoaded, Consumer<String> onFailure) {
        new Thread(() -> fetch(onLoaded, onFailure), "instance-catalog-loader").start();
    }

    private void fetch(Consumer<List<GameInstance>> onLoaded, Consumer<String> onFailure) {
        GameCatalogResponse response;
        try {
            response = apiService.fetchGameCatalog();
        } catch (ApiTimeoutException ex) {
            fail(onFailure, "El servidor tardó demasiado en responder al pedir la lista de juegos.");
            return;
        } catch (ApiConnectionException ex) {
            fail(onFailure, "No se pudo conectar con el servidor para obtener la lista de juegos.");
            return;
        }

        if (response == null || !response.success || response.instances == null) {
            String message = response != null && response.message != null
                    ? response.message
                    : "No se pudo obtener la lista de juegos.";
            fail(onFailure, message);
            return;
        }

        List<GameInstance> instances = response.instances;
        SwingUtilities.invokeLater(() -> onLoaded.accept(instances));
    }

    private void fail(Consumer<String> onFailure, String message) {
        SwingUtilities.invokeLater(() -> onFailure.accept(message));
    }
}
