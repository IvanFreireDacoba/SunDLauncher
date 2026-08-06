package es.sund.launcher.ui;

import java.awt.Color;

/**
 * Paleta compartida "SunDStudios" (piedra + oro envejecido), la misma
 * identidad visual que ya usa MainFrame para el login. Centralizada aquí
 * para que InstanceSelectionFrame/InstancePanel/InstanceListItem no
 * dupliquen los mismos valores mágicos en cada clase.
 */
final class Theme {

    static final Color GOLD_TEXT = new Color(242, 219, 165);
    static final Color GOLD_TEXT_MUTED = new Color(196, 176, 140);
    static final Color GOLD_ACCENT = new Color(224, 178, 96);

    static final Color STONE_PANEL_FILL = new Color(18, 13, 10, 190);
    static final Color STONE_BUTTON = new Color(58, 50, 42);
    static final Color STONE_BUTTON_BORDER = new Color(201, 154, 68, 130);

    static final Color DANGER_TEXT = new Color(224, 130, 118);
    static final Color DANGER_BORDER = new Color(150, 78, 68);

    static final Color SIDEBAR_BACKGROUND = new Color(20, 16, 13);
    static final Color SIDEBAR_DIVIDER = new Color(70, 60, 50);
    static final Color SIDEBAR_SELECTED = new Color(224, 178, 96, 35);

    /** Alto compartido entre la sección de perfil (sidebar) y la barra inferior de InstancePanel, para que ambas se alineen visualmente. */
    static final int PROFILE_SECTION_HEIGHT = 72;

    private Theme() {}
}
