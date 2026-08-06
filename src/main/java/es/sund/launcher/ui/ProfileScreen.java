package es.sund.launcher.ui;

import es.sund.launcher.config.LauncherSettings;

import javax.swing.*;
import java.awt.*;

/**
 * Pantalla de "tu perfil" en el área de detalle de InstanceSelectionFrame:
 * se muestra en vez de una instancia cuando el jugador hace click en la
 * sección de perfil (abajo de la columna izquierda). De momento solo
 * enseña el nombre de usuario y un desplegable de ajustes -en vez del
 * antiguo botón de rueda que abría un JPopupMenu- con la opción
 * "Minimizar launcher durante el juego" (ver LauncherSettings).
 *
 * Constructor sin argumentos por WindowBuilder; el nombre se asigna
 * después con setUsername(), igual que el resto de paneles de esta
 * pantalla.
 */
public class ProfileScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel usernameValueLabel = new JLabel(" ");
    private final JPanel settingsBody = new JPanel();
    private final JButton settingsToggle = new JButton();
    private boolean settingsExpanded = true;

    public ProfileScreen() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Theme.SIDEBAR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Tu perfil");
        title.setForeground(Theme.GOLD_TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel usernameCaption = new JLabel("USUARIO");
        usernameCaption.setForeground(Theme.GOLD_TEXT_MUTED);
        usernameCaption.setFont(usernameCaption.getFont().deriveFont(Font.PLAIN, 10.5f));
        usernameCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameCaption.setBorder(BorderFactory.createEmptyBorder(26, 0, 2, 0));

        usernameValueLabel.setForeground(Theme.GOLD_TEXT);
        usernameValueLabel.setFont(usernameValueLabel.getFont().deriveFont(Font.BOLD, 18f));
        usernameValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(title);
        content.add(usernameCaption);
        content.add(usernameValueLabel);
        content.add(buildSettingsSection());

        add(content, BorderLayout.NORTH);
    }

    /** Cabecera "▾ Ajustes" que expande/colapsa el contenido de ajustes debajo (desplegable, no un popup aparte). */
    private JComponent buildSettingsSection() {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        settingsToggle.setText(toggleLabel());
        settingsToggle.setHorizontalAlignment(SwingConstants.LEFT);
        settingsToggle.setFocusPainted(false);
        settingsToggle.setContentAreaFilled(false);
        settingsToggle.setBorderPainted(false);
        settingsToggle.setMargin(new Insets(0, 0, 0, 0));
        settingsToggle.setForeground(Theme.GOLD_TEXT);
        settingsToggle.setFont(settingsToggle.getFont().deriveFont(Font.BOLD, 14f));
        settingsToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsToggle.addActionListener(e -> toggleSettings());

        settingsBody.setOpaque(false);
        settingsBody.setLayout(new BoxLayout(settingsBody, BoxLayout.Y_AXIS));
        settingsBody.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsBody.setBorder(BorderFactory.createEmptyBorder(12, 4, 0, 0));

        JCheckBox minimizeCheckbox = new JCheckBox("Minimizar launcher durante el juego");
        minimizeCheckbox.setSelected(LauncherSettings.isMinimizeDuringGameEnabled());
        minimizeCheckbox.setOpaque(false);
        minimizeCheckbox.setForeground(Theme.GOLD_TEXT_MUTED);
        minimizeCheckbox.setFont(minimizeCheckbox.getFont().deriveFont(13f));
        minimizeCheckbox.setFocusPainted(false);
        minimizeCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        minimizeCheckbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minimizeCheckbox.addActionListener(
                e -> LauncherSettings.setMinimizeDuringGameEnabled(minimizeCheckbox.isSelected()));
        settingsBody.add(minimizeCheckbox);

        section.add(settingsToggle);
        section.add(settingsBody);
        return section;
    }

    private void toggleSettings() {
        settingsExpanded = !settingsExpanded;
        settingsBody.setVisible(settingsExpanded);
        settingsToggle.setText(toggleLabel());
        revalidate();
        repaint();
    }

    private String toggleLabel() {
        return (settingsExpanded ? "▾" : "▸") + "  Ajustes";
    }

    public void setUsername(String username) {
        usernameValueLabel.setText(username != null && !username.isBlank() ? username : " ");
    }
}
