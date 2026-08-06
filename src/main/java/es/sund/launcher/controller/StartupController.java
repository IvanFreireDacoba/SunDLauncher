package es.sund.launcher.controller;

import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.exception.ApiConnectionException;
//import es.sund.launcher.exception.ApiTimeoutException;
import es.sund.launcher.exception.CredentialStorageException;
import es.sund.launcher.model.VersionCheckResponse;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.security.StoredCredentials;
import es.sund.launcher.service.LauncherUpdateService;
import es.sund.launcher.ui.MainFrame;

/**
 * Se ejecuta una vez al arrancar la aplicación, antes de mostrar la ventana
 * (o justo después). Hace dos cosas independientes, cada una en su propio
 * hilo para no bloquear la UI ni depender la una de la otra:
 *
 * 1) Si hay credenciales guardadas, las precarga en el formulario.
 * 2) Consulta si hay una actualización del launcher disponible y habilita
 *    el botón "Actualizar lanzador" solo si hay conexión Y la versión difiere.
 */
public class StartupController {

    private final MainFrame mainFrame;
    private final CredentialStore credentialStore;
    private final LauncherUpdateService updateService;

    public StartupController(MainFrame mainFrame, CredentialStore credentialStore,
                              SunDApiService apiService, LauncherUpdateService updateService) {
        this.mainFrame = mainFrame;
        this.credentialStore = credentialStore;
        this.updateService = updateService;
    }

    public void onStartup() {
        new Thread(this::loadStoredCredentials, "credentials-loader").start();
        new Thread(this::checkForUpdate, "update-checker").start();
    }

    private void loadStoredCredentials() {
        if (!credentialStore.hasStoredCredentials()) {
            return;
        }
        try {
            StoredCredentials stored = credentialStore.load();
            if (stored != null) {
                // Si llegamos a ver esta pantalla con credenciales guardadas es porque
                // LauncherBootstrapper no pudo saltársela del todo (p.ej. un hipo de
                // red al comprobar actualizaciones), pero las credenciales en sí pueden
                // seguir siendo válidas: en vez de dejarlas ahí precargadas esperando un
                // click, se validan solas para no hacer esperar al jugador sin motivo.
                mainFrame.prefillCredentialsAndAutoLogin(stored.getUsername(), stored.getPassword());
            }
        } catch (CredentialStorageException ex) {
            // Si las credenciales guardadas están corruptas o no se pueden leer,
            // simplemente no se precargan; el usuario puede iniciar sesión a mano.
            System.err.println("Aviso: no se pudieron cargar las credenciales guardadas: " + ex.getMessage());
        }
    }

    private void checkForUpdate() {
        try {
            VersionCheckResponse remote = updateService.checkRemoteVersion();
            boolean updateAvailable = updateService.isUpdateAvailable(remote);
            mainFrame.setUpdateButtonEnabled(updateAvailable);
        } catch (ApiConnectionException ex) {
            // Sin conexión o timeout: el botón se queda deshabilitado, sin interrumpir el arranque.
            mainFrame.setUpdateButtonEnabled(false);
        }
    }
}
