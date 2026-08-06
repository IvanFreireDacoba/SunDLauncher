package es.sund.launcher.ui;

import javax.swing.*;
import java.awt.*;

/**
 * JPanel normal y corriente salvo por una cosa: implementa Scrollable para
 * que, dentro de un JScrollPane, su ANCHO se fije siempre al del viewport
 * (getScrollableTracksViewportWidth() = true) en vez de al ancho preferido
 * de sus hijos.
 *
 * Sin esto, un BoxLayout.Y_AXIS con hijos de ancho "estirable" (maximumSize
 * = Integer.MAX_VALUE, como InstanceGridTile) no tiene ningún efecto dentro
 * de un JScrollPane: el viewport le daría a este panel su propio ancho
 * preferido (el máximo de sus hijos) en vez del ancho real disponible, y
 * los hijos nunca llegarían a estirarse de lado a lado de la columna. El
 * alto, en cambio, NO se fuerza al del viewport (false): tiene que poder
 * crecer más allá de la altura visible para que aparezca la barra de
 * scroll vertical si hay muchas instancias.
 */
class ScrollableStackPanel extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 120;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
