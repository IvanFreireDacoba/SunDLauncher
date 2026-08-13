package es.sund.launcher.action;

import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.service.GameLaunchCoordinator;
import es.sund.launcher.service.InstanceInstallStatus;
import es.sund.launcher.ui.InstancePanel;
import es.sund.launcher.ui.InstanceSelectionFrame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Botón único por instancia que sirve tanto para "Instalar"/"Actualizar" como
 * para "Jugar": el texto cambia según InstanceInstallStatus, pero pulsarlo
 * siempre delega en GameLaunchCoordinator, que decide qué hacer. Instalar/
 * actualizar NUNCA lanza el juego automáticamente al terminar -para poder
 * poner a instalar varias instancias a la vez sin que cada una abra Minecraft
 * por su cuenta-, solo "Jugar" en una instancia ya lista lo hace. Cada
 * instancia instala/juega en su propio hilo, con su propio InstancePanel para
 * pintar el progreso, así que pulsar el botón de una instancia nunca bloquea,
 * oculta ni cierra el resto del launcher: se puede seguir navegando o lanzar
 * otra instancia en paralelo.
 */
public class PlayOrInstallAction implements ActionListener {

    private final InstanceSelectionFrame frame;
    private final InstancePanel panel;
    private final GameInstance instance;
    private final String username;
    private final SunDApiService apiService;
    private final CredentialStore credentialStore;
    private final GameLaunchCoordinator gameLaunchCoordinator = new GameLaunchCoordinator();

    public PlayOrInstallAction(InstanceSelectionFrame frame, InstancePanel panel, GameInstance instance,
            String username, SunDApiService apiService, CredentialStore credentialStore) {
        this.frame = frame;
        this.panel = panel;
        this.instance = instance;
        this.username = username;
        this.apiService = apiService;
        this.credentialStore = credentialStore;
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
        gameLaunchCoordinator.launch(username, instance, panel, frame, apiService, credentialStore, failureMessage -> {
            restoreIdleState();
            frame.setStatus(failureMessage);
        });
    }

    /** Tras un fallo, vuelve al estado correcto según lo que de verdad haya en disco (no lo que hubiera antes de intentarlo). */
    private void restoreIdleState() {
        InstanceInstallStatus.refreshPanel(panel, instance);
    }
}
