package es.sund.launcher.ui;

import es.sund.launcher.config.AppConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Carga la miniatura (logo) de una instancia para la columna vertical de
 * InstanceSelectionFrame. Usa /images/instances/{id}/logo.png si existe; si
 * no (todavía no hay arte dedicado a miniatura para esa instancia), reutiliza
 * el mismo background.png de la pantalla de detalle escalado, en vez de dejar
 * la fila sin icono. Si tampoco hay background, devuelve null (fallback
 * seguro: la fila se queda sin icono, igual que InstancePanel pinta blanco
 * cuando no encuentra su imagen de fondo).
 */
final class InstanceIcons {

    static ImageIcon loadThumbnail(int instanceId, int size) {
        BufferedImage image = readImage(String.format(AppConstants.INSTANCE_LOGO_RESOURCE_PATTERN, instanceId));
        if (image == null) {
            image = readImage(String.format(AppConstants.INSTANCE_BACKGROUND_RESOURCE_PATTERN, instanceId));
        }
        if (image == null) {
            return null;
        }
        Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static BufferedImage readImage(String resourcePath) {
        try (InputStream in = InstanceIcons.class.getResourceAsStream(resourcePath)) {
            return in != null ? ImageIO.read(in) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private InstanceIcons() {}
}
