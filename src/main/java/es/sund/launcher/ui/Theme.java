package es.sund.launcher.ui;

import es.sund.launcher.config.LauncherSettings;

import java.awt.Color;

/**
 * Paleta de color del launcher. Tres temas seleccionables desde ProfileScreen
 * (mismo concepto que el selector de /perfil en la web, ver
 * Documentacion/web/README.md#temas-visuales): Clásico, SunDStudios y Arcano
 * (MMORPG). Los campos siguen llamándose GOLD_... / STONE_... por continuidad
 * con el resto del código (así no hace falta tocar cada punto donde se usan),
 * aunque ahora mismo no siempre contengan un color "dorado" o "piedra": lo
 * que importa es el rol (texto principal, texto muted, acento...), no el
 * nombre literal.
 *
 * Igual que la web decide su hoja de estilos al cargar la página (no repinta
 * en caliente si cambias la cookie a mitad de sesión), aquí la paleta se fija
 * una vez al arrancar el proceso, leyendo LauncherSettings: cambiar el tema
 * desde ProfileScreen guarda la preferencia pero se aplica en el próximo
 * arranque del launcher, no reconstruye en caliente las ventanas ya creadas.
 */
final class Theme {

    /** Los tres temas visuales del launcher, en paralelo con los de la web (classic/alternative/mmorpg). */
    enum LauncherPalette {
        CLASSIC("classic", "Clásico"),
        SUNDSTUDIOS("sundstudios", "SunDStudios"),
        MMORPG("mmorpg", "Arcano (MMORPG)");

        final String settingsId;
        final String displayName;

        LauncherPalette(String settingsId, String displayName) {
            this.settingsId = settingsId;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

        static LauncherPalette fromSettingsId(String id) {
            for (LauncherPalette palette : values()) {
                if (palette.settingsId.equals(id)) {
                    return palette;
                }
            }
            return SUNDSTUDIOS;
        }
    }

    static Color GOLD_TEXT;
    static Color GOLD_TEXT_MUTED;
    static Color GOLD_ACCENT;

    static Color STONE_PANEL_FILL;
    static Color STONE_BUTTON;
    static Color STONE_BUTTON_BORDER;

    static Color DANGER_TEXT;
    static Color DANGER_BORDER;

    static Color SIDEBAR_BACKGROUND;
    static Color SIDEBAR_DIVIDER;
    static Color SIDEBAR_SELECTED;

    /** Alto compartido entre la sección de perfil (sidebar) y la barra inferior de InstancePanel, para que ambas se alineen visualmente. */
    static final int PROFILE_SECTION_HEIGHT = 72;

    static {
        apply(LauncherPalette.fromSettingsId(LauncherSettings.getLauncherTheme()));
    }

    static void apply(LauncherPalette palette) {
        switch (palette) {
            case CLASSIC:
                GOLD_TEXT = new Color(255, 255, 255);
                GOLD_TEXT_MUTED = new Color(170, 170, 170);
                GOLD_ACCENT = new Color(0, 255, 204);
                STONE_PANEL_FILL = new Color(20, 20, 20, 190);
                STONE_BUTTON = new Color(37, 37, 37);
                STONE_BUTTON_BORDER = new Color(0, 255, 204, 130);
                DANGER_TEXT = new Color(224, 130, 118);
                DANGER_BORDER = new Color(150, 78, 68);
                SIDEBAR_BACKGROUND = new Color(26, 26, 26);
                SIDEBAR_DIVIDER = new Color(60, 60, 60);
                SIDEBAR_SELECTED = new Color(0, 255, 204, 35);
                break;
            case MMORPG:
                GOLD_TEXT = new Color(241, 236, 255);
                GOLD_TEXT_MUTED = new Color(144, 137, 184);
                GOLD_ACCENT = new Color(181, 140, 255);
                STONE_PANEL_FILL = new Color(16, 12, 27, 190);
                STONE_BUTTON = new Color(35, 27, 58);
                STONE_BUTTON_BORDER = new Color(181, 140, 255, 130);
                DANGER_TEXT = new Color(224, 130, 118);
                DANGER_BORDER = new Color(150, 78, 68);
                SIDEBAR_BACKGROUND = new Color(16, 12, 24);
                SIDEBAR_DIVIDER = new Color(70, 55, 110);
                SIDEBAR_SELECTED = new Color(181, 140, 255, 35);
                break;
            case SUNDSTUDIOS:
            default:
                GOLD_TEXT = new Color(242, 219, 165);
                GOLD_TEXT_MUTED = new Color(196, 176, 140);
                GOLD_ACCENT = new Color(224, 178, 96);
                STONE_PANEL_FILL = new Color(18, 13, 10, 190);
                STONE_BUTTON = new Color(58, 50, 42);
                STONE_BUTTON_BORDER = new Color(201, 154, 68, 130);
                DANGER_TEXT = new Color(224, 130, 118);
                DANGER_BORDER = new Color(150, 78, 68);
                SIDEBAR_BACKGROUND = new Color(20, 16, 13);
                SIDEBAR_DIVIDER = new Color(70, 60, 50);
                SIDEBAR_SELECTED = new Color(224, 178, 96, 35);
                break;
        }
    }

    private Theme() {}
}
