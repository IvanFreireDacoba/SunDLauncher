package es.sund.launcher.action;

import es.sund.launcher.config.AppPaths;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.service.InstanceInstallStatus;
import es.sund.launcher.ui.InstancePanel;
import es.sund.launcher.util.DownloadUtil;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * Borra del disco todo lo instalado de una instancia (versión de Minecraft,
 * librerías, assets, Fabric, config, mods, resourcepacks...), tras pedir
 * confirmación explícita. Solo está visible cuando la instancia ya está
 * instalada (ver InstancePanel.showInstalled()) y corre en su propio hilo,
 * igual que instalar, para no bloquear el resto del launcher.
 */
public class UninstallAction implements ActionListener {

    private final InstancePanel panel;
    private final GameInstance instance;

    public UninstallAction(InstancePanel panel, GameInstance instance) {
        this.panel = panel;
        this.instance = instance;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int choice = JOptionPane.showConfirmDialog(
                panel,
                "Se borrarán todos los archivos instalados de " + instance.name
                        + ".\n¿Seguro que quieres desinstalarla?",
                "Desinstalar " + instance.name,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        panel.showProgress("Desinstalando " + instance.name + "...", 0, 0);
        new Thread(this::performUninstall, "instance-uninstall-worker-" + instance.id).start();
    }

    private void performUninstall() {
        try {
            DownloadUtil.deleteRecursive(AppPaths.forInstance(instance).root.toPath());
        } catch (IOException ignored) {
            // DownloadUtil.deleteRecursive ya ignora fallos por-fichero (p.ej. un
            // .jar bloqueado por el antivirus); esto es solo una red de seguridad
            // adicional para no matar el hilo si algo más se escapa.
        }
        InstanceInstallStatus.refreshPanel(panel, instance);
    }
}
