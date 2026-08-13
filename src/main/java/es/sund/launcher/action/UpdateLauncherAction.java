package es.sund.launcher.action;

import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.exception.ApiTimeoutException;
import es.sund.launcher.exception.LauncherUpdateException;
import es.sund.launcher.model.VersionCheckResponse;
import es.sund.launcher.service.LauncherUpdateService;
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
 */
public class UpdateLauncherAction implements ActionListener {

    private final MainFrame mainFrame;
    private final LauncherUpdateService updateService;

    public UpdateLauncherAction(MainFrame mainFrame, LauncherUpdateService updateService) {
        this.mainFrame = mainFrame;
        this.updateService = updateService;
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

        mainFrame.setStatus("Descargando actualización...");
        try {
            updateService.applyUpdate(remote);
        } catch (LauncherUpdateException ex) {
            fail("Fallo al aplicar la actualización: " + ex.getMessage());
            return;
        }

        if (remote.launcherDownloadUrl != null) {
            openInBrowser(remote.launcherDownloadUrl);
            mainFrame.setStatus("Se ha abierto la página de descarga. Instala la nueva versión y reinicia el launcher.");
        } else {
            mainFrame.setStatus("Configuración actualizada correctamente.");
        }
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
