package es.sund.launcher.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Panel de detalle de una instancia dentro de InstanceSelectionFrame: la
 * imagen de fondo a pantalla completa, con algunos datos de la instancia
 * arriba a la izquierda (versión, estado) y los controles -Jugar/Instalar,
 * Desinstalar, o la barra de progreso mientras instala/desinstala- abajo a
 * la derecha, en tamaño de botón normal (no un icono a pantalla completa).
 *
 * Solo se ve un InstancePanel a la vez (ver InstanceSelectionFrame, que los
 * apila en un CardLayout), pero cada uno instala/juega de forma
 * independiente: el progreso se pinta aquí mismo, nunca en una ventana
 * aparte, así que el jugador puede cambiar de instancia y seguir viendo o
 * arrancando otras mientras esta sigue en marcha en su propio hilo (ver
 * PlayOrInstallAction/GameLaunchCoordinator).
 *
 * Tiene constructor sin argumentos para que WindowBuilder pueda instanciarlo
 * en modo Design; el fondo, los datos y el estado se asignan después con
 * setters.
 */
public class InstancePanel extends JPanel {

    private static final long serialVersionUID = 8823140766118437651L;

    private static final String CARD_CONTROLS = "controls";
    private static final String CARD_PROGRESS = "progress";

    private BufferedImage backgroundImage;

    private final JLabel nameLabel = new JLabel();
    private final JLabel detailsLabel = new JLabel();

    private final JButton actionButton = new JButton("Instalar");
    private final JButton uninstallButton = new JButton("Desinstalar");

    private final JLabel progressTaskLabel = new JLabel(" ");
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private final CardLayout bottomCards = new CardLayout();
    private final JPanel bottomContent = new JPanel(bottomCards);

    public InstancePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        add(buildInfoOverlay(), BorderLayout.NORTH);
        add(buildBottomBar(), BorderLayout.SOUTH);

        bottomCards.show(bottomContent, CARD_CONTROLS);
    }

    private JComponent buildInfoOverlay() {
        nameLabel.setForeground(Theme.GOLD_TEXT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 22f));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailsLabel.setForeground(Theme.GOLD_TEXT_MUTED);
        detailsLabel.setFont(detailsLabel.getFont().deriveFont(Font.PLAIN, 13f));
        detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        RoundedPanel infoPanel = new RoundedPanel(14);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 26));
        infoPanel.add(nameLabel);
        infoPanel.add(detailsLabel);

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 24));
        wrapper.setOpaque(false);
        wrapper.add(infoPanel);
        return wrapper;
    }

    /**
     * Franja horizontal de lado a lado, con la misma altura que la sección de
     * perfil del jugador (Theme.PROFILE_SECTION_HEIGHT) para que ambas se
     * vean como parte de la misma "barra de estado" del launcher. Dentro,
     * un CardLayout cambia entre los botones (alineados a la derecha) y la
     * barra de progreso (ocupando todo el ancho).
     */
    private JComponent buildBottomBar() {
        styleActionButton(actionButton);
        styleDangerButton(uninstallButton);
        uninstallButton.setVisible(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);
        controls.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        controls.add(uninstallButton);
        controls.add(actionButton);

        JComponent progressStrip = buildProgressStrip();

        bottomContent.setOpaque(false);
        bottomContent.add(controls, CARD_CONTROLS);
        bottomContent.add(progressStrip, CARD_PROGRESS);

        // GridBagLayout con fill=HORIZONTAL (no BOTH): bottomContent ocupa todo
        // el ancho de la franja pero conserva su alto preferido (el de un botón,
        // bastante menor que Theme.PROFILE_SECTION_HEIGHT), así que
        // GridBagLayout lo centra verticalmente en el espacio que sobra. Sin
        // este envoltorio, BorderLayout.CENTER estiraba bottomContent (y con
        // él, el FlowLayout de "controls") a los 72px enteros de la franja, y
        // FlowLayout NUNCA centra su fila verticalmente en el espacio extra:
        // la alinea arriba del todo, que es justo lo que se veía mal.
        JPanel bottomWrapper = new JPanel(new GridBagLayout());
        bottomWrapper.setOpaque(false);
        GridBagConstraints bottomGbc = new GridBagConstraints();
        bottomGbc.fill = GridBagConstraints.HORIZONTAL;
        bottomGbc.weightx = 1.0;
        bottomWrapper.add(bottomContent, bottomGbc);

        JPanel strip = new JPanel(new BorderLayout());
        strip.setOpaque(true);
        strip.setBackground(Theme.SIDEBAR_BACKGROUND);
        strip.setPreferredSize(new Dimension(10, Theme.PROFILE_SECTION_HEIGHT));
        strip.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SIDEBAR_DIVIDER));
        strip.add(bottomWrapper, BorderLayout.CENTER);
        return strip;
    }

    private JComponent buildProgressStrip() {
        progressTaskLabel.setForeground(Theme.GOLD_TEXT_MUTED);
        progressTaskLabel.setFont(progressTaskLabel.getFont().deriveFont(Font.PLAIN, 12.5f));

        progressBar.setStringPainted(false);
        progressBar.setForeground(Theme.GOLD_ACCENT);
        progressBar.setPreferredSize(new Dimension(10, 10));

        // GridBagLayout con fill=HORIZONTAL (no BOTH): la barra ocupa todo el
        // ancho disponible pero mantiene su alto preferido (fino), centrada
        // verticalmente en vez de estirarse a los 72px de la franja entera.
        JPanel barWrapper = new JPanel(new GridBagLayout());
        barWrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        barWrapper.add(progressBar, gbc);

        JPanel progressStrip = new JPanel(new BorderLayout(18, 0));
        progressStrip.setOpaque(false);
        progressStrip.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        progressStrip.add(progressTaskLabel, BorderLayout.WEST);
        progressStrip.add(barWrapper, BorderLayout.CENTER);
        return progressStrip;
    }

    /** Mismo "look" de botón que MainFrame (piedra oscura, borde dorado, texto claro), a tamaño de botón normal. */
    private static void styleActionButton(JButton button) {
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setForeground(Theme.GOLD_TEXT);
        button.setBackground(Theme.STONE_BUTTON);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GOLD_ACCENT, 1),
                BorderFactory.createEmptyBorder(8, 24, 8, 24)));
    }

    /** Botón secundario, discreto, para la acción destructiva de desinstalar. */
    private static void styleDangerButton(JButton button) {
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 12f));
        button.setForeground(Theme.DANGER_TEXT);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.DANGER_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
    }

    public JButton getActionButton() {
        return actionButton;
    }

    public JButton getUninstallButton() {
        return uninstallButton;
    }

    /** Datos de la instancia mostrados sobre la imagen (nombre + versión/estado de instalación). */
    public void setInstanceInfo(String name, String details) {
        nameLabel.setText(name != null ? name : " ");
        detailsLabel.setText(details != null ? details : " ");
    }

    /** Instalada e inactiva: botón "Jugar" + botón "Desinstalar" visibles. */
    public void showInstalled() {
        SwingUtilities.invokeLater(() -> {
            actionButton.setText("Jugar");
            actionButton.setEnabled(true);
            uninstallButton.setVisible(true);
            uninstallButton.setEnabled(true);
            bottomCards.show(bottomContent, CARD_CONTROLS);
        });
    }

    /** Instalada pero con el contenido desactualizado: botón "Actualizar" + "Desinstalar". Pulsarlo vuelve a descargar el instance-pack y sobreescribe lo que haga falta. */
    public void showUpdateAvailable() {
        SwingUtilities.invokeLater(() -> {
            actionButton.setText("Actualizar");
            actionButton.setEnabled(true);
            uninstallButton.setVisible(true);
            uninstallButton.setEnabled(true);
            bottomCards.show(bottomContent, CARD_CONTROLS);
        });
    }

    /** No instalada: solo el botón "Instalar" (nunca se puede desinstalar lo que no existe). */
    public void showNotInstalled() {
        SwingUtilities.invokeLater(() -> {
            actionButton.setText("Instalar");
            actionButton.setEnabled(true);
            uninstallButton.setVisible(false);
            bottomCards.show(bottomContent, CARD_CONTROLS);
        });
    }

    /** Minecraft en marcha: controles deshabilitados para no permitir un segundo lanzamiento simultáneo. */
    public void showPlaying() {
        SwingUtilities.invokeLater(() -> {
            actionButton.setText("Jugando...");
            actionButton.setEnabled(false);
            uninstallButton.setEnabled(false);
            bottomCards.show(bottomContent, CARD_CONTROLS);
        });
    }

    /**
     * Instalando o desinstalando: sustituye los botones por una barra de progreso
     * embebida en este mismo panel (nunca una ventana aparte), para que el resto
     * del launcher -incluida cualquier otra instancia- se pueda seguir usando.
     * Coincide con la forma de {@link es.sund.launcher.util.DownloadUtil.ProgressListener}
     * para poder pasarse directamente como listener.
     */
    public void showProgress(String taskName, long bytesDone, long bytesTotal) {
        SwingUtilities.invokeLater(() -> {
            progressTaskLabel.setText(taskName != null ? taskName : "Preparando...");
            if (bytesTotal > 0) {
                progressBar.setIndeterminate(false);
                progressBar.setValue((int) ((bytesDone * 100) / bytesTotal));
            } else {
                progressBar.setIndeterminate(true);
            }
            bottomCards.show(bottomContent, CARD_PROGRESS);
        });
    }

    /** Carga (o recarga) la imagen de fondo desde el classpath. Si no existe, se pinta blanco (fallback seguro). */
    public void setBackgroundImageResource(String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            backgroundImage = in != null ? ImageIO.read(in) : null;
        } catch (IOException e) {
            backgroundImage = null;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
