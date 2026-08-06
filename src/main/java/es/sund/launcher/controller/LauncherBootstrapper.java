package es.sund.launcher.controller;

import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.exception.CredentialStorageException;
import es.sund.launcher.model.AccountCheckResponse;
import es.sund.launcher.model.VersionCheckResponse;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.security.StoredCredentials;
import es.sund.launcher.service.LauncherUpdateService;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

/**
 * Se ejecuta una única vez al arrancar la aplicación, ANTES de decidir qué
 * ventana mostrar. Comprueba, en este orden:
 *
 * 1) Si hay credenciales guardadas (si no las hay, no hay nada que "saltar").
 * 2) Si el launcher está actualizado (comparando con AppConstants.CURRENT_LAUNCHER_VERSION
 *    a través de LauncherUpdateService). Si hay una actualización pendiente, se
 *    exige login manual para que el jugador vea el botón "Actualizar lanzador".
 * 3) Si las credenciales guardadas siguen siendo válidas contra tu API (por si
 *    la contraseña cambió, la cuenta fue baneada, etc). Si ya no son válidas,
 *    se borran del almacenamiento local y se pide login manual.
 *
 * Solo si las tres comprobaciones pasan se salta la pantalla de login, igual
 * que hacen launchers como el de Riot o Steam con la sesión recordada.
 *
 * Cualquier fallo de red (timeout o sin conexión) en cualquiera de los pasos
 * hace que, por seguridad, se caiga al login manual en vez de asumir que todo
 * está bien.
 */
public class LauncherBootstrapper {

	private final CredentialStore credentialStore;
	private final SunDApiService apiService;
	private final LauncherUpdateService updateService;

	public LauncherBootstrapper(CredentialStore credentialStore, SunDApiService apiService,
			LauncherUpdateService updateService) {
		this.credentialStore = credentialStore;
		this.apiService = apiService;
		this.updateService = updateService;
	}

	/**
	 * @param onManualLoginRequired se ejecuta en el EDT si hay que mostrar la pantalla de login de toda la vida
	 * @param onAutoLoginSuccess    se ejecuta en el EDT con el username ya autenticado, sin mostrar login
	 */
	public void run(Runnable onManualLoginRequired, Consumer<String> onAutoLoginSuccess) {
		new Thread(() -> attempt(onManualLoginRequired, onAutoLoginSuccess), "launcher-bootstrap").start();
	}

	private void attempt(Runnable onManualLoginRequired, Consumer<String> onAutoLoginSuccess) {
		if (!credentialStore.hasStoredCredentials()) {
			requireManualLogin(onManualLoginRequired);
			return;
		}

		VersionCheckResponse remoteVersion;
		try {
			remoteVersion = updateService.checkRemoteVersion();
		} catch (ApiConnectionException ex) {
			// Cubre timeout y fallo de conexión: si no podemos confirmar que el
			// launcher está al día, no arriesgamos, y pedimos login manual.
			requireManualLogin(onManualLoginRequired);
			return;
		}

		if (updateService.isUpdateAvailable(remoteVersion)) {
			requireManualLogin(onManualLoginRequired);
			return;
		}

		StoredCredentials stored;
		try {
			stored = credentialStore.load();
		} catch (CredentialStorageException ex) {
			requireManualLogin(onManualLoginRequired);
			return;
		}
		if (stored == null) {
			requireManualLogin(onManualLoginRequired);
			return;
		}

		AccountCheckResponse accountResponse;
		try {
			accountResponse = apiService.checkAccount(stored.getUsername(), stored.getPassword());
		} catch (ApiConnectionException ex) {
			requireManualLogin(onManualLoginRequired);
			return;
		}

		if (!accountResponse.success) {
			// La contraseña guardada ya no es válida (cambió, cuenta baneada, etc):
			// no tiene sentido seguir intentando el auto-login con ella.
			clearInvalidCredentials();
			requireManualLogin(onManualLoginRequired);
			return;
		}

		String username = stored.getUsername();
		SwingUtilities.invokeLater(() -> onAutoLoginSuccess.accept(username));
	}

	private void clearInvalidCredentials() {
		try {
			credentialStore.clear();
		} catch (CredentialStorageException ex) {
			System.err.println("Aviso: no se pudieron limpiar credenciales inválidas: " + ex.getMessage());
		}
	}

	private void requireManualLogin(Runnable onManualLoginRequired) {
		SwingUtilities.invokeLater(onManualLoginRequired);
	}
}
