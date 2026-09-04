package es.sund.launcher.action;

import es.sund.launcher.api.SunDApiService;
import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.model.GameCatalogResponse;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.model.VersionCheckResponse;
import es.sund.launcher.security.CredentialStore;
import es.sund.launcher.service.GameLaunchCoordinator;
import es.sund.launcher.service.InstanceInstallStatus;
import es.sund.launcher.service.LauncherUpdateService;
import es.sund.launcher.ui.InstancePanel;
import es.sund.launcher.ui.InstanceSelectionFrame;

import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 *
 * Antes de decidir instalar/jugar, refresca en caliente (refreshRemoteState())
 * tanto la versión del launcher como los hashes del catálogo de ESTA
 * instancia: StartupController/InstanceCatalogController solo consultan esto
 * una vez, al arrancar o al entrar a la pantalla de instancias, así que si el
 * jugador se deja el launcher abierto y sale una actualización mientras
 * tanto, pulsar "Jugar" entraba con datos obsoletos (bug real reportado:
 * "puedo entrar a la instancia sin dicha update"). No dispara la
 * autoactualización completa del launcher aquí (eso reiniciaría el proceso
 * entero y mataría cualquier otra instancia instalando/jugando en paralelo,
 * ver comentario de clase) — si hay actualización de LAUNCHER, se bloquea
 * este lanzamiento y se pide reiniciar el launcher; si es solo el contenido
 * de la instancia el que cambió, los hashes frescos hacen que
 * InstanceInstallStatus ya vea "actualización disponible" de verdad y
 * GameLaunchCoordinator se comporte como si se hubiera pulsado recién
 * "Actualizar", sin cambiar esa lógica en absoluto.
 */
public class PlayOrInstallAction implements ActionListener {

    private static final Logger LOG = Logger.getLogger(PlayOrInstallAction.class.getName());

    private final InstanceSelectionFrame frame;
    private final InstancePanel panel;
    private final GameInstance instance;
    private final String username;
    private final SunDApiService apiService;
    private final CredentialStore credentialStore;
    private final GameLaunchCoordinator gameLaunchCoordinator = new GameLaunchCoordinator();
    private final LauncherUpdateService launcherUpdateService;

    public PlayOrInstallAction(InstanceSelectionFrame frame, InstancePanel panel, GameInstance instance,
            String username, SunDApiService apiService, CredentialStore credentialStore) {
        this.frame = frame;
        this.panel = panel;
        this.instance = instance;
        this.username = username;
        this.apiService = apiService;
        this.credentialStore = credentialStore;
        this.launcherUpdateService = new LauncherUpdateService(apiService);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Se cambia a la tarjeta de progreso aquí mismo, en el EDT y antes de
        // arrancar el hilo: evita que un doble click alcance a lanzar dos
        // instalaciones/lanzamientos en paralelo de la misma instancia.
        panel.showProgress("Comprobando actualizaciones...", 0, 0);
        new Thread(this::performLaunch, "instance-launch-worker-" + instance.id).start();
    }

    private void performLaunch() {
        if (!refreshRemoteState()) {
            return; // ya se avisó al jugador (actualización de launcher pendiente); no se lanza nada.
        }
        SwingUtilities.invokeLater(() -> panel.showProgress("Preparando " + instance.name + "...", 0, 0));
        gameLaunchCoordinator.launch(username, instance, panel, frame, apiService, credentialStore, failureMessage -> {
            restoreIdleState();
            frame.setStatus(failureMessage);
        });
    }

    /**
     * Vuelve a consultar la versión del launcher y el catálogo de instancias antes de decidir
     * nada. Devuelve false si hay que abortar el lanzamiento (actualización de launcher
     * pendiente). Un fallo de red en cualquiera de las dos consultas NO bloquea: se sigue con
     * los datos ya cargados (mejor dejar jugar con datos algo desfasados que impedir jugar por
     * un simple hipo de conexión al comprobar).
     */
    private boolean refreshRemoteState() {
        try {
            VersionCheckResponse remoteVersion = launcherUpdateService.checkRemoteVersion();
            if (launcherUpdateService.isUpdateAvailable(remoteVersion)) {
                restoreIdleState();
                frame.setStatus("Hay una actualización del launcher disponible. Cierra y vuelve a abrir "
                        + "el launcher para instalarla antes de jugar.");
                return false;
            }
        } catch (ApiConnectionException ex) {
            LOG.log(Level.WARNING, "No se pudo comprobar la versión del launcher antes de jugar, se continúa con los datos ya cargados", ex);
        }

        try {
            GameCatalogResponse catalog = apiService.fetchGameCatalog();
            if (catalog != null && catalog.success && catalog.instances != null) {
                for (GameInstance fresh : catalog.instances) {
                    if (fresh.id == instance.id) {
                        applyFreshMetadata(fresh);
                        break;
                    }
                }
            }
        } catch (ApiConnectionException ex) {
            LOG.log(Level.WARNING, "No se pudo refrescar el catálogo de instancias antes de jugar, se continúa con los datos ya cargados", ex);
        }
        return true;
    }

    /** Copia solo los campos que InstanceInstallStatus usa para detectar contenido desactualizado. */
    private void applyFreshMetadata(GameInstance fresh) {
        instance.instancePackUrl = fresh.instancePackUrl;
        instance.instancePackSha1 = fresh.instancePackSha1;
        instance.modpackJsonUrl = fresh.modpackJsonUrl;
        instance.modpackJsonSha1 = fresh.modpackJsonSha1;
        instance.resourcepackJsonUrl = fresh.resourcepackJsonUrl;
        instance.resourcepackJsonSha1 = fresh.resourcepackJsonSha1;
    }

    /** Tras un fallo, vuelve al estado correcto según lo que de verdad haya en disco (no lo que hubiera antes de intentarlo). */
    private void restoreIdleState() {
        InstanceInstallStatus.refreshPanel(panel, instance);
    }
}
