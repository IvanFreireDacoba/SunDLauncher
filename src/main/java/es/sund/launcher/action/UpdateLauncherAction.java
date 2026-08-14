package es.sund.launcher.action;

import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.exception.ApiTimeoutException;
import es.sund.launcher.exception.LauncherUpdateException;
import es.sund.launcher.model.VersionCheckResponse;
import es.sund.launcher.service.LauncherUpdateService;
import es.sund.launcher.service.SelfUpdateService;
import es.sund.launcher.ui.MainFrame;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

/**
 * Encapsula la actualización del launcher. Solo debería poder pulsarse cuando
 * StartupController ya ha confirmado que hay conexión Y que la versión remota
 * difiere de AppConstants.CURRENT_LAUNCHER_VERSION (el botón permanece
 * deshabilitado en cualquier otro caso).
 *
 * Desde la versión 1.2: ya no se limita a abrir la página de descargas y dejar
 * que el jugador instale a mano — descarga la nueva versión directamente de
 * GitHub y se aplica sola (SelfUpdateService), con una barra de progreso en
 * esta misma ventana. Al terminar, el proceso se relanza entero (nuevo
 * arranque limpio: LauncherBootstrapper decide otra vez si toca login manual
 * o se salta a la selección de instancias) y este se cierra. El enlace de
 * descarga manual se conserva como último recurso si SelfUpdateService no
 * puede aplicarse (p.ej. modo desarrollo, sin instalación empaquetada real)
 * o si algo falla a mitad de camino.
 */
public class UpdateLauncherAction implements ActionListener {

    private final MainFrame mainFrame;
    private final LauncherUpdateService updateService;
    private final SelfUpdateService selfUpdateService;

    public UpdateLauncherAction(MainFrame mainFrame, LauncherUpdateService updateService) {
        this(mainFrame, updateService, new SelfUpdateService());
    }

    public UpdateLauncherAction(MainFrame mainFrame, LauncherUpdateService updateService, SelfUpdateService selfUpdateService) {
        this.mainFrame = mainFrame;
        this.updateService = updateService;
        this.selfUpdateService = selfUpdateService;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        mainFrame.getUpdateButton().setEnabled(false);
        mainFrame.setStatus("Comprobando actualización...");
        new Thread(this::performUpdate, "launcher-update-worker").start();
    }

    private void performUpdate() {
        VersionCheckResponse remote;
        try {
            remote = updateService.checkRemoteVersion();
        } catch (ApiTimeoutException ex) {
            fail("El servidor tardó demasiado en responder. Inténtalo de nuevo más tarde.");
            return;
        } catch (ApiConnectionException ex) {
            fail("No se pudo conectar con el servidor para comprobar la actualización.");
            return;
        }

        if (!updateService.isUpdateAvailable(remote)) {
            mainFrame.setStatus("Ya tienes la última versión.");
            return;
        }

        try {
            updateService.applyUpdate(remote);
        } catch (LauncherUpdateException ex) {
            fail("Fallo al aplicar la actualización: " + ex.getMessage());
            return;
        }

        SelfUpdateService.Result result;
        try {
            result = selfUpdateService.performSelfUpdate(mainFrame::showSelfUpdateProgress);
        } catch (LauncherUpdateException ex) {
            mainFrame.hideSelfUpdateProgress();
            fail("No se pudo autoactualizar: " + ex.getMessage() + fallbackHint(remote));
            return;
        }

        if (result == SelfUpdateService.Result.APPLIED_WILL_RESTART) {
            // La nueva versión ya se descargó y quedó lanzada (Linux) o en marcha un
            // ayudante que la aplicará en cuanto este proceso termine (Windows/macOS,
            // ver SelfUpdateService). Nada más que hacer aquí salvo salir.
            System.exit(0);
            return;
        }

        // NOT_APPLICABLE: no se detectó una instalación empaquetada real (modo
        // desarrollo). Único caso en el que se cae al flujo antiguo de siempre.
        mainFrame.hideSelfUpdateProgress();
        if (remote.launcherDownloadUrl != null) {
            openInBrowser(remote.launcherDownloadUrl);
            mainFrame.setStatus("Se ha abierto la página de descarga. Instala la nueva versión y reinicia el launcher.");
        } else {
            fail("No se pudo autoactualizar y no hay enlace de descarga disponible.");
        }
    }

    private String fallbackHint(VersionCheckResponse remote) {
        return remote.launcherDownloadUrl != null
                ? " Descárgala manualmente desde " + remote.launcherDownloadUrl
                : "";
    }

    private void openInBrowser(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            // launcherDownloadUrl viene del backend (VersionCheckResponse); si alguna vez se
            // pudiera manipular esa respuesta, un esquema como "file:" haría que Desktop.browse
            // delegue en el manejador local de ficheros (xdg-open/ShellExecute) en vez de un
            // navegador, con riesgo de ejecutar algo local. Solo se permite http/https.
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                mainFrame.setStatus("Descarga disponible en: " + url);
                return;
            }
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (Exception ex) {
            // No es crítico: si no se puede abrir el navegador, el usuario aún puede
            // ver la URL en el mensaje de estado y visitarla manualmente.
            mainFrame.setStatus("Descarga disponible en: " + url);
        }
    }

    private void fail(String message) {
        SwingUtilities.invokeLater(() -> {
            mainFrame.setStatus(message);
            mainFrame.getUpdateButton().setEnabled(true);
        });
    }
}
