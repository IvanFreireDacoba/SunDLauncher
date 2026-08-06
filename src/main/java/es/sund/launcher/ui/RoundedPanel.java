package es.sund.launcher.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Panel con fondo de piedra semitransparente y borde dorado, esquinas
 * redondeadas: el mismo "look" que ya usaba MainFrame para el formBacking
 * detrás del formulario de login, pero como componente reutilizable en vez
 * de una clase anónima duplicada en cada pantalla.
 */
class RoundedPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int radius;

    RoundedPanel(int radius) {
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.STONE_PANEL_FILL);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.setColor(Theme.STONE_BUTTON_BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}
