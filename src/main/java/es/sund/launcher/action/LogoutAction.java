package es.sund.launcher.action;

import es.sund.launcher.exception.CredentialStorageException;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.ui.InstanceSelectionFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Cierra la sesión actual: borra las credenciales guardadas (para que
 * LauncherBootstrapper no vuelva a entrar solo con ellas en el próximo
 * arranque) y vuelve a la pantalla de login. A diferencia de ExitAction, no
 * mata el proceso: el jugador puede entrar con otra cuenta sin reiniciar el
 * launcher.
 */
public class LogoutAction implements ActionListener {

    private final InstanceSelectionFrame frame;
    private final CredentialStore credentialStore;
    private final Runnable onLoggedOut;

    public LogoutAction(InstanceSelectionFrame frame, CredentialStore credentialStore, Runnable onLoggedOut) {
        this.frame = frame;
        this.credentialStore = credentialStore;
        this.onLoggedOut = onLoggedOut;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int choice = JOptionPane.showConfirmDialog(
                frame,
                "¿Seguro que quieres cerrar sesión?",
                "Cerrar sesión",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            credentialStore.clear();
        } catch (CredentialStorageException ex) {
            // No bloqueante: aunque falle borrar el fichero cifrado, seguimos
            // cerrando sesión en la UI. En el peor caso, el próximo arranque
            // reintenta el auto-login con una credencial que StartupController
            // validará igual contra la API (y la borrará si ya no es válida).
            System.err.println("Aviso: no se pudieron borrar las credenciales guardadas: " + ex.getMessage());
        }

        frame.dispose();
        onLoggedOut.run();
    }
}
