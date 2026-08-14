package es.sund.launcher.action;

import es.sund.launcher.config.AppPaths;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/** Abre AppPaths.ROOT_DIR (carpeta de datos del launcher: instancias, credenciales, config) en el explorador de archivos del sistema. */
public class OpenLocalFilesAction implements ActionListener {

    private final Component parent;

    public OpenLocalFilesAction(Component parent) {
        this.parent = parent;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            showError();
            return;
        }
        try {
            Desktop.getDesktop().open(AppPaths.ROOT_DIR);
        } catch (IOException ex) {
            showError();
        }
    }

    private void showError() {
        JOptionPane.showMessageDialog(
                parent,
                "No se ha podido abrir la carpeta de ficheros locales.\nRuta: " + AppPaths.ROOT_DIR.getAbsolutePath(),
                "Ficheros locales",
                JOptionPane.WARNING_MESSAGE);
    }
}
