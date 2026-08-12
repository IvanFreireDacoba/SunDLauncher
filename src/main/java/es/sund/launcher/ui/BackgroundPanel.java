package es.sund.launcher.ui;

import es.sund.launcher.config.AppConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * JPanel que pinta una imagen de fondo escalada para llenar todo el panel.
 * Si no encuentra la imagen en el classpath, pinta simplemente blanco (fallback seguro).
 * Tiene constructor sin argumentos para que WindowBuilder pueda instanciarlo en modo Design.
 */
public class BackgroundPanel extends JPanel {

    private static final long serialVersionUID = 3466875985565258907L;
	private BufferedImage backgroundImage;

    public BackgroundPanel() {
        setLayout(null); // los componentes hijos se posicionan con setBounds desde MainFrame
        loadBackgroundImage();
    }

    private void loadBackgroundImage() {
        try (InputStream in = getClass().getResourceAsStream(AppConstants.BACKGROUND_IMAGE_RESOURCE)) {
            if (in != null) {
                backgroundImage = ImageIO.read(in);
            }
        } catch (IOException e) {
            backgroundImage = null; // si falla la carga, simplemente pintamos blanco
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        if (backgroundImage != null) {
            // Recorte centrado ("cover"): antes se estiraba a getWidth()/getHeight()
            // exactos, deformando la imagen cada vez que la ventana cambiaba de
            // proporción (ver ImageScaling, mismo fix que InstancePanel/InstanceGridTile).
            ImageScaling.drawCover(g2, backgroundImage, getWidth(), getHeight());
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
    }
}
