package es.sund.launcher.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Franja de una instancia dentro de la columna izquierda (sustituye a la
 * antigua fila logo+nombre de InstanceListItem, y a una versión anterior de
 * esta misma clase que la dibujaba como un cuadro cuadrado): la imagen de
 * fondo de la instancia recortada ("cover", sin deformar) para llenar todo
 * el ancho de la columna, alto fijo, con el nombre en una franja inferior
 * semitransparente. Al pulsarla selecciona esa instancia en el panel de
 * detalle, igual que antes hacía InstanceListItem.
 *
 * Alto fijo (ver InstanceSelectionFrame.INSTANCE_TILE_HEIGHT); el ancho lo
 * estira el BoxLayout.Y_AXIS del contenedor (columna izquierda) hasta
 * ocupar todo el ancho disponible -de lado a lado, como un rectángulo, no
 * un cuadro con hueco vacío al lado- gracias a getMaximumSize() devolviendo
 * un ancho prácticamente ilimitado.
 *
 * Constructor sin argumentos por WindowBuilder; imagen y nombre se asignan
 * después con setters.
 */
public class InstanceGridTile extends JButton {

    private static final long serialVersionUID = 1L;

    private BufferedImage backgroundImage;
    private final JLabel nameLabel = new JLabel();
    private boolean selected;

    public InstanceGridTile() {
        setLayout(new BorderLayout());
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));

        JPanel nameBacking = new JPanel(new BorderLayout());
        nameBacking.setOpaque(true);
        nameBacking.setBackground(new Color(0, 0, 0, 155));
        nameBacking.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        nameBacking.add(nameLabel, BorderLayout.CENTER);

        add(nameBacking, BorderLayout.SOUTH);
    }

    public void setInstanceName(String name) {
        nameLabel.setText(name != null ? name : " ");
    }

    /** Carga la imagen de fondo desde el classpath; si no existe, se pinta un gris de relleno (fallback seguro). */
    public void setBackgroundImageResource(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            backgroundImage = in != null ? ImageIO.read(in) : null;
        } catch (IOException e) {
            backgroundImage = null;
        }
        repaint();
    }

    /** Resalta la franja cuando su instancia es la que se ve en el panel de detalle. */
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, InstanceSelectionFrame.INSTANCE_TILE_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(80, InstanceSelectionFrame.INSTANCE_TILE_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        // Ancho prácticamente ilimitado (alto fijo): es lo que hace que
        // BoxLayout.Y_AXIS estire esta franja a todo el ancho de la columna
        // en vez de dejarla en su tamaño preferido con hueco vacío al lado.
        return new Dimension(Integer.MAX_VALUE, InstanceSelectionFrame.INSTANCE_TILE_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (backgroundImage != null && w > 0 && h > 0) {
            // Recorte centrado ("cover"): la imagen llena el rectángulo entero sin
            // deformarse, recortando el sobrante por los lados o por arriba/abajo
            // según haga falta -no siempre un cuadrado, como antes-, según la
            // proporción real de la franja (que cambia con el ancho de la ventana).
            int imgW = backgroundImage.getWidth();
            int imgH = backgroundImage.getHeight();
            double targetAspect = (double) w / h;
            double sourceAspect = (double) imgW / imgH;
            int sx, sy, sw, sh;
            if (sourceAspect > targetAspect) {
                sh = imgH;
                sw = (int) Math.round(imgH * targetAspect);
                sx = (imgW - sw) / 2;
                sy = 0;
            } else {
                sw = imgW;
                sh = (int) Math.round(imgW / targetAspect);
                sx = 0;
                sy = (imgH - sh) / 2;
            }
            g2.drawImage(backgroundImage, 0, 0, w, h, sx, sy, sx + sw, sy + sh, this);
        } else {
            g2.setColor(Theme.STONE_BUTTON);
            g2.fillRect(0, 0, w, h);
        }

        if (selected) {
            g2.setColor(Theme.GOLD_ACCENT);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRect(1, 1, w - 3, h - 3);
        } else {
            g2.setColor(Theme.SIDEBAR_DIVIDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(0, 0, w - 1, h - 1);
        }
        g2.dispose();

        super.paintComponent(g);
    }
}
