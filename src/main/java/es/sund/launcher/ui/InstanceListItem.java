package es.sund.launcher.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Fila de la columna vertical de instancias (logo + nombre), dentro de
 * InstanceSelectionFrame. Es un JButton sin borde/relleno propio (mismo
 * patrón que el botón de Instalar/Jugar de InstancePanel: todo el "look" lo
 * da el contenido, no el JButton), para poder engancharle un ActionListener
 * normal y corriente al hacer click y así cambiar qué InstancePanel se ve.
 *
 * Constructor sin argumentos por WindowBuilder; el logo y el nombre se
 * asignan después con setters, igual que el resto de paneles de esta
 * pantalla.
 */
public class InstanceListItem extends JButton {

    private static final long serialVersionUID = 1L;

    private final JLabel logoLabel = new JLabel();
    private final JLabel nameLabel = new JLabel();
    private boolean selected;

    public InstanceListItem() {
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        setBorderPainted(false);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.LEFT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        logoLabel.setPreferredSize(new Dimension(40, 40));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        nameLabel.setForeground(Theme.GOLD_TEXT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));

        add(logoLabel, BorderLayout.WEST);
        add(nameLabel, BorderLayout.CENTER);
    }

    public void setLogoIcon(Icon icon) {
        logoLabel.setIcon(icon);
    }

    public void setInstanceName(String name) {
        nameLabel.setText(name);
    }

    /** Resalta la fila cuando su instancia es la que se ve en el panel de detalle. */
    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (selected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Theme.SIDEBAR_SELECTED);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.GOLD_ACCENT);
            g2.fillRect(0, 0, 3, getHeight());
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
