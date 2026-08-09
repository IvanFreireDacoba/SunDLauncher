package es.sund.launcher;

import es.sund.launcher.action.ExitAction;
import es.sund.launcher.action.LoginAction;
import es.sund.launcher.action.LogoutAction;
import es.sund.launcher.action.PlayOrInstallAction;
import es.sund.launcher.action.UninstallAction;
import es.sund.launcher.action.UpdateLauncherAction;
import es.sund.launcher.api.HttpSunDApiService;
import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.controller.InstanceCatalogController;
import es.sund.launcher.controller.LauncherBootstrapper;
import es.sund.launcher.controller.StartupController;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.security.EncryptedFileCredentialStore;
import es.sund.launcher.service.InstanceInstallStatus;
import es.sund.launcher.service.LauncherUpdateService;
import es.sund.launcher.ui.InstancePanel;
import es.sund.launcher.ui.InstanceSelectionFrame;
import es.sund.launcher.ui.MainFrame;

import javax.swing.*;
import java.util.List;

/**
 * Punto de entrada y composition root: es el único sitio de toda la aplicación
 * donde se instancian las implementaciones concretas (HttpSunDApiService,
 * EncryptedFileCredentialStore...) y se decide qué pantalla mostrar primero.
 * El resto de clases solo conocen interfaces.
 *
 * Flujo (Step 1 + Step 2):
 *   start() -> LauncherBootstrapper comprueba en segundo plano si el launcher
 *   está actualizado Y hay credenciales guardadas válidas.
 *     - Si sí -> showInstanceSelection(username): se salta el login y se va
 *       directo a elegir instancia.
 *     - Si no (por lo que sea: sin credenciales, credenciales inválidas, hay que
 *       actualizar, o no hay conexión para comprobarlo) -> showLoginScreen():
 *       el login manual de toda la vida, con las credenciales precargadas si las hay.
 *   En ambos casos, tras autenticarse se muestra InstanceSelectionFrame (Step 2)
 *   en vez de lanzar el juego directamente: el jugador elige qué instancia
 *   instalar/jugar (SunD Origins, CobbleSpain, ...).
 */
public class Main {

    // Dependencias compartidas durante toda la vida del proceso (composition root).
    private static SunDApiService apiService;
    private static CredentialStore credentialStore;
    private static LauncherUpdateService updateService;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        apiService = new HttpSunDApiService();
        credentialStore = new EncryptedFileCredentialStore();
        updateService = new LauncherUpdateService(apiService);

        new LauncherBootstrapper(credentialStore, apiService, updateService)
                .run(Main::showLoginScreen, Main::showInstanceSelection);
    }

    /** Pantalla de login manual, con las credenciales guardadas precargadas si las hay. */
    private static void showLoginScreen() {
        showLoginScreen(null);
    }

    /** Igual que showLoginScreen(), pero mostrando de entrada un mensaje de estado (por ejemplo, un error). */
    private static void showLoginScreen(String initialStatusMessage) {
        MainFrame mainFrame = new MainFrame();

        mainFrame.getLoginButton().addActionListener(new LoginAction(mainFrame, apiService, credentialStore, Main::onLoginSuccess));
        mainFrame.getExitButton().addActionListener(new ExitAction());
        mainFrame.getUpdateButton().addActionListener(new UpdateLauncherAction(mainFrame, updateService));

        new StartupController(mainFrame, credentialStore, apiService, updateService).onStartup();

        if (initialStatusMessage != null) {
            mainFrame.setStatus(initialStatusMessage);
        }
        mainFrame.setVisible(true);
    }

    /** Login manual correcto: LoginAction llama desde un hilo de fondo, por eso se fuerza el EDT aquí. */
    private static void onLoginSuccess(String username) {
        SwingUtilities.invokeLater(() -> showInstanceSelection(username));
    }

    /** Se salta el login (bootstrapper) o viene de un login manual recién hecho: toca elegir instancia. */
    private static void showInstanceSelection(String username) {
        InstanceSelectionFrame frame = new InstanceSelectionFrame();
        frame.getProfileScreen().getLogoutButton()
                .addActionListener(new LogoutAction(frame, credentialStore, Main::showLoginScreen));
        frame.setVisible(true);
        frame.setStatus("Cargando juegos disponibles...");

        new InstanceCatalogController(apiService).load(
                instances -> bindInstances(frame, username, instances),
                frame::setStatus
        );
    }

    private static void bindInstances(InstanceSelectionFrame frame, String username, List<GameInstance> instances) {
        frame.setStatus(" ");
        frame.setUsername(username);
        frame.showInstances(instances);
        for (GameInstance instance : instances) {
            InstancePanel panel = frame.getInstancePanel(instance.id);
            applyInstallState(panel, instance);
            panel.getActionButton().addActionListener(new PlayOrInstallAction(frame, panel, instance, username));
            panel.getUninstallButton().addActionListener(new UninstallAction(panel, instance));
        }
    }

    private static void applyInstallState(InstancePanel panel, GameInstance instance) {
        boolean installed = InstanceInstallStatus.isInstalled(instance);
        boolean updateAvailable = installed && InstanceInstallStatus.isUpdateAvailable(instance);

        StringBuilder details = new StringBuilder("Minecraft ").append(instance.mcVersion);
        if (instance.fabricLoaderVersion != null && !instance.fabricLoaderVersion.isBlank()) {
            details.append(" · Fabric ").append(instance.fabricLoaderVersion);
        }
        details.append(" · ").append(!installed ? "No instalado" : updateAvailable ? "Actualización disponible" : "Instalado");
        panel.setInstanceInfo(instance.name, details.toString());

        if (updateAvailable) {
            panel.showUpdateAvailable();
        } else if (installed) {
            panel.showInstalled();
        } else {
            panel.showNotInstalled();
        }
    }
}
