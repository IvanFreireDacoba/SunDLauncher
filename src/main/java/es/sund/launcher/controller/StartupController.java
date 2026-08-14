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
 *
 * El auto-login (equivalente a pulsar "Entrar" solo, ver
 * MainFrame.prefillCredentialsAndAutoLogin) solo se dispara cuando AMBOS
 * hilos han terminado Y confirman que es seguro: hay credenciales guardadas
 * Y no hace falta actualizar el launcher primero. Si solo se mirara el
 * estado de "hay credenciales", una carrera entre los dos hilos podría
 * loguear automáticamente al jugador antes de que la comprobación de
 * actualización terminara de confirmar que sí hacía falta actualizar -
 * exactamente el caso que LauncherBootstrapper ya había intentado evitar al
 * mandar aquí (login manual) en vez de saltarse la pantalla del todo-.
 */
public class StartupController {

    private final MainFrame mainFrame;
    private final CredentialStore credentialStore;
    private final LauncherUpdateService updateService;

    private final Object lock = new Object();
    private boolean credentialsChecked = false;
    private boolean updateCheckDone = false;
    private boolean updateAvailable = false;
    private StoredCredentials storedCredentials;

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

    /**
     * Precarga las credenciales guardadas (si las hay) de inmediato, para que el jugador
     * no vea los campos vacíos mientras se resuelve la comprobación de actualización en el
     * otro hilo. Solo rellena: el auto-login en sí se decide en maybeAutoLogin(), una vez
     * confirmado también el estado de la actualización.
     */
    private void loadStoredCredentials() {
        if (!credentialStore.hasStoredCredentials()) {
            markCredentialsChecked(null);
            return;
        }
        try {
            StoredCredentials stored = credentialStore.load();
            if (stored != null) {
                mainFrame.prefillCredentials(stored.getUsername(), stored.getPassword());
            }
            markCredentialsChecked(stored);
        } catch (CredentialStorageException ex) {
            // Si las credenciales guardadas están corruptas o no se pueden leer,
            // simplemente no se precargan; el usuario puede iniciar sesión a mano.
            System.err.println("Aviso: no se pudieron cargar las credenciales guardadas: " + ex.getMessage());
            markCredentialsChecked(null);
        }
    }

    private void checkForUpdate() {
        try {
            VersionCheckResponse remote = updateService.checkRemoteVersion();
            boolean available = updateService.isUpdateAvailable(remote);
            mainFrame.setUpdateButtonEnabled(available);
            markUpdateChecked(available);
        } catch (ApiConnectionException ex) {
            // Sin conexión o timeout: el botón se queda deshabilitado, sin interrumpir el arranque.
            mainFrame.setUpdateButtonEnabled(false);
            markUpdateChecked(false);
        }
    }

    private void markCredentialsChecked(StoredCredentials stored) {
        synchronized (lock) {
            this.storedCredentials = stored;
            this.credentialsChecked = true;
        }
        maybeAutoLogin();
    }

    private void markUpdateChecked(boolean available) {
        synchronized (lock) {
            this.updateAvailable = available;
            this.updateCheckDone = true;
        }
        maybeAutoLogin();
    }

    private void maybeAutoLogin() {
        StoredCredentials stored;
        synchronized (lock) {
            if (!credentialsChecked || !updateCheckDone || updateAvailable || storedCredentials == null) {
                return;
            }
            stored = storedCredentials;
        }
        mainFrame.prefillCredentialsAndAutoLogin(stored.getUsername(), stored.getPassword());
    }
}
