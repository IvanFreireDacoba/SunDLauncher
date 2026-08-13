package es.sund.launcher.action;

import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.exception.ApiTimeoutException;
import es.sund.launcher.exception.CredentialStorageException;
import es.sund.launcher.model.AccountCheckResponse;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.ui.MainFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Encapsula todo lo que ocurre al pulsar "Entrar": 1) verifica la cuenta contra
 * tu API 2) si es válida, guarda las credenciales de forma cifrada para el
 * próximo arranque 3) delega en onLoginSuccess, que lleva al jugador a la
 * pantalla de selección de instancias (no lanza Minecraft directamente: eso
 * ahora depende de qué instancia elija después).
 *
 * No conoce los detalles de red: delega en SunDApiService. Su única
 * responsabilidad es orquestar el login y actualizar la UI.
 */
public class LoginAction implements ActionListener {

	private final MainFrame mainFrame;
	private final SunDApiService apiService;
	private final CredentialStore credentialStore;
	private final Consumer<String> onLoginSuccess;

	public LoginAction(MainFrame mainFrame, SunDApiService apiService, CredentialStore credentialStore,
			Consumer<String> onLoginSuccess) {
		this.mainFrame = mainFrame;
		this.apiService = apiService;
		this.credentialStore = credentialStore;
		this.onLoginSuccess = onLoginSuccess;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String username = mainFrame.getUsernameField().getText().trim();
		char[] password = mainFrame.getPasswordField().getPassword();

		mainFrame.setFormEnabled(false);
		mainFrame.setStatus("Verificando cuenta...");

		new Thread(() -> performLogin(username, password), "login-worker").start();
	}

	private void performLogin(String username, char[] password) {
		AccountCheckResponse response;

		try {
			response = apiService.checkAccount(username, password);
		} catch (ApiTimeoutException ex) {
			fail("El servidor tardó demasiado en responder. Inténtalo de nuevo.");
			return;
		} catch (ApiConnectionException ex) {
			fail("No se pudo conectar con el servidor. Comprueba tu conexión.");
			return;
		}

		if (!response.success) {
			failWithAccountHint(response.message != null ? response.message : "Usuario o contraseña incorrectos");
			return;
		}

		try {
			credentialStore.save(username, password);
		} catch (CredentialStorageException ex) {
			// No es un error bloqueante: el login fue correcto, simplemente no se
			// recordará la sesión la próxima vez. Avisamos pero seguimos adelante.
			System.err.println("Aviso: no se pudieron guardar las credenciales: " + ex.getMessage());
		}

		SwingUtilities.invokeLater(() -> mainFrame.setVisible(false));
		onLoginSuccess.accept(username);
	}

	private void fail(String message) {
		mainFrame.setStatus(message);
		mainFrame.setFormEnabled(true);
	}

	/** Igual que fail(), pero para credenciales inválidas: ver MainFrame.setStatusWithAccountHint(). */
	private void failWithAccountHint(String message) {
		mainFrame.setStatusWithAccountHint(message);
		mainFrame.setFormEnabled(true);
	}
}
