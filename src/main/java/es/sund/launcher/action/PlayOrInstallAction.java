package es.sund.launcher.action;

import es.sund.launcher.model.GameInstance;
import es.sund.launcher.service.GameLaunchCoordinator;
import es.sund.launcher.service.InstanceInstallStatus;
import es.sund.launcher.ui.InstancePanel;
import es.sund.launcher.ui.InstanceSelectionFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Botón único por instancia que sirve tanto para "Instalar" como para "Jugar":
 * el texto cambia según InstanceInstallStatus, pero pulsarlo siempre hace lo
 * mismo (instalar lo que falte y lanzar). Cada instancia instala/juega en su
 * propio hilo, con su propio InstancePanel para pintar el progreso, así que
 * pulsar el botón de una instancia nunca bloquea, oculta ni cierra el resto
 * del launcher: se puede seguir navegando o lanzar otra instancia en paralelo.
 */
public class PlayOrInstallAction implements ActionListener {

    private final InstanceSelectionFrame frame;
    private final InstancePanel panel;
    private final GameInstance instance;
    private final String username;
    private final GameLaunchCoordinator gameLaunchCoordinator = new GameLaunchCoordinator();

    public PlayOrInstallAction(InstanceSelectionFrame frame, InstancePanel panel, GameInstance instance, String username) {
        this.frame = frame;
        this.panel = panel;
        this.instance = instance;
        this.username = username;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Se cambia a la tarjeta de progreso aquí mismo, en el EDT y antes de
        // arrancar el hilo: evita que un doble click alcance a lanzar dos
        // instalaciones/lanzamientos en paralelo de la misma instancia.
        panel.showProgress("Preparando " + instance.name + "...", 0, 0);
        new Thread(this::performLaunch, "instance-launch-worker-" + instance.id).start();
    }

    private void performLaunch() {
        gameLaunchCoordinator.launch(username, instance, panel, frame, failureMessage -> {
            restoreIdleState();
            frame.setStatus(failureMessage);
        });
    }

    /** Tras un fallo, vuelve al estado correcto según lo que de verdad haya en disco (no lo que hubiera antes de intentarlo). */
    private void restoreIdleState() {
        if (InstanceInstallStatus.isInstalled(instance)) {
            panel.showInstalled();
        } else {
            panel.showNotInstalled();
        }
    }
}
