package es.sund.launcher.service;

import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.config.LauncherSettings;
import es.sund.launcher.exception.InstallationException;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.ui.InstancePanel;
import es.sund.launcher.ui.InstanceSelectionFrame;

import javax.swing.SwingUtilities;
import java.awt.Frame;
import java.util.function.Consumer;

/**
 * Encapsula el flujo de "instalar lo que falte y lanzar el juego" para una
 * instancia concreta. El progreso y los cambios de estado se pintan
 * directamente en el InstancePanel de esa instancia -nunca en una ventana
 * aparte-, así que el jugador puede seguir viendo o usando el resto del
 * launcher, incluida cualquier otra instancia instalando/jugando en paralelo
 * (cada una en su propio hilo, ver PlayOrInstallAction), mientras esto ocurre.
 *
 * El launcher YA NO se cierra solo cuando el jugador cierra Minecraft: se
 * queda abierto (para poder seguir usándolo, instalando otras instancias,
 * etc). En su lugar, si el ajuste "Minimizar launcher durante el juego" está
 * activo (ver LauncherSettings), la ventana se minimiza mientras haya alguna
 * partida abierta y se restaura cuando la última termina -coordinado entre
 * varias instancias jugando a la vez con LaunchActivityTracker, para no
 * restaurar la ventana mientras otra instancia sigue jugando.
 */
public class GameLaunchCoordinator {

	public void launch(String username, GameInstance instance, InstancePanel panel, InstanceSelectionFrame frame,
			SunDApiService apiService, CredentialStore credentialStore, Consumer<String> onFailure) {
		GameSessionStarter sessionStarter =
				new GameSessionStarter(instance, panel::showProgress, apiService, credentialStore);

		// Si la instancia no está instalada o tiene contenido pendiente de actualizar, este
		// clic es un "Instalar"/"Actualizar": se instala y se para ahí, sin lanzar Minecraft.
		// Necesario para poder poner a instalar varias instancias a la vez sin que cada una,
		// al terminar, abra el juego por su cuenta -el jugador pulsa "Jugar" aparte cuando
		// quiera arrancar de verdad-. Solo una instancia ya lista de verdad (instalada y al
		// día) llega más abajo a lanzar el proceso.
		if (!InstanceInstallStatus.isReadyToPlay(instance)) {
			try {
				sessionStarter.ensureInstalled();
				InstanceInstallStatus.refreshPanel(panel, instance);
			} catch (InstallationException ex) {
				SwingUtilities.invokeLater(() -> onFailure.accept("No se pudo instalar " + instance.name + ": " + ex.getMessage()));
			} catch (RuntimeException ex) {
				SwingUtilities.invokeLater(() -> onFailure.accept("No se pudo instalar " + instance.name + " (error inesperado): " + ex.getMessage()));
			}
			return;
		}

		try {
			Process process = sessionStarter.start(username);
			panel.showPlaying();

			// El contador solo abraza el tiempo que el proceso de Minecraft está
			// realmente abierto (no la instalación previa): así "la primera partida
			// que arranca" y "la última que termina" se calculan sobre partidas de
			// verdad, sin carreras con otras instancias que solo estén instalando.
			boolean wasFirst = LaunchActivityTracker.beginAndWasFirst();
			if (wasFirst && LauncherSettings.isMinimizeDuringGameEnabled()) {
				SwingUtilities.invokeLater(() -> frame.setState(Frame.ICONIFIED));
			}
			try {
				process.waitFor();
			} finally {
				if (LaunchActivityTracker.endAndWasLast()) {
					SwingUtilities.invokeLater(() -> frame.setState(Frame.NORMAL));
				}
			}

			panel.showInstalled();
		} catch (InstallationException ex) {
			SwingUtilities.invokeLater(() -> onFailure.accept("No se pudo iniciar " + instance.name + ": " + ex.getMessage()));
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} catch (RuntimeException ex) {
			// Red de seguridad: este método corre en un hilo propio por instancia
			// ("instance-launch-worker-<id>") sin manejador de excepciones no
			// capturadas. Sin este catch, cualquier RuntimeException inesperada
			// (p.ej. una URL mal formada) mataría ese hilo en silencio y la
			// instancia se quedaría colgada en su tarjeta de progreso para siempre.
			SwingUtilities.invokeLater(() -> onFailure.accept("No se pudo iniciar " + instance.name + " (error inesperado): " + ex.getMessage()));
		}
	}
}
