package es.sund.launcher.ui;

import es.sund.launcher.config.AppConstants;
import es.sund.launcher.util.IcoImageLoader;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

/**
 * Ventana principal. Esta clase SOLO construye y expone componentes de UI:
 * no llama a la API, no toca ficheros, no lanza Minecraft. Esa lógica vive
 * en las clases de es.sund.launcher.action y es.sund.launcher.controller,
 * que reciben esta ventana y se enganchan a sus botones desde fuera.
 *
 * Tiene un constructor público sin argumentos a propósito: es un requisito
 * de WindowBuilder para poder abrir la pestaña "Design" y renderizar la
 * ventana sin necesitar dependencias externas.
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 219466180975617916L;

	private final BackgroundPanel backgroundPanel = new BackgroundPanel();

    /** Panel translúcido detrás del formulario, para que los campos se lean bien sobre la ilustración de fondo. */
    private final JPanel formBacking = new JPanel(null) {
        private static final long serialVersionUID = 1L;
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.STONE_PANEL_FILL);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.setColor(Theme.STONE_BUTTON_BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.dispose();
        }
    };

    private final JLabel titleLabel = new JLabel("Iniciar sesión en SunD", SwingConstants.CENTER);
    private final JLabel usernameLabel = new JLabel("Usuario:");
    private final JLabel passwordLabel = new JLabel("Contraseña:");
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    /** Enlace opcional bajo statusLabel: "¿No tienes cuenta? Crea una en SunD.es", ver setStatusWithAccountHint(). */
    private final JLabel accountHintLabel = new JLabel(" ", SwingConstants.CENTER);

    private final JButton loginButton = new JButton("Entrar");
    private final JButton exitButton = new JButton("Salir");
    private final JButton updateButton = new JButton("Actualizar lanzador");
    /** Sustituye a usuario/contraseña/Entrar/Actualizar mientras SelfUpdateService descarga y aplica la nueva versión. */
    private final JProgressBar selfUpdateProgressBar = new JProgressBar(0, 100);

    /** Tamaño mínimo para que el formulario no llegue a deformarse; sin máximo, la ventana admite pantalla completa. */
    private static final int MIN_WIDTH = 640;
    private static final int MIN_HEIGHT = 520;

    public MainFrame() {
        super("SunD Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setIconImages(IcoImageLoader.loadAllSizes(AppConstants.WINDOW_ICON_RESOURCE));

        Dimension windowSize = computeWindowSize();
        setSize(windowSize);
        setLocationRelativeTo(null);

        buildComponents();
        setContentPane(backgroundPanel);
        layoutComponents(windowSize.width, windowSize.height);

        // Pulsar Enter en cualquier campo del formulario equivale a pulsar "Entrar": el
        // propio estado enabled del botón (updateLoginButtonState) ya exige usuario +
        // contraseña + que no haga falta actualizar, así que basta con delegar en él como
        // botón por defecto del root pane, sin duplicar esa condición aquí.
        getRootPane().setDefaultButton(loginButton);

        // El formulario usa posicionamiento absoluto (setBounds), no un
        // LayoutManager, así que al redimensionar/maximizar la ventana hay que
        // recalcular esas posiciones a mano: sin esto se quedarían clavadas en
        // las coordenadas del tamaño inicial y el formulario se vería
        // descuadrado o cortado en vez de reajustarse.
        backgroundPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutComponents(backgroundPanel.getWidth(), backgroundPanel.getHeight());
            }
        });

        // Habilitar "Entrar" solo cuando usuario y contraseña tienen contenido.
        // Esto es puramente reactivo de UI, por eso vive aquí y no en una Action.
        DocumentListener fieldsListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateLoginButtonState(); }
            @Override public void removeUpdate(DocumentEvent e) { updateLoginButtonState(); }
            @Override public void changedUpdate(DocumentEvent e) { updateLoginButtonState(); }
        };
        usernameField.getDocument().addDocumentListener(fieldsListener);
        passwordField.getDocument().addDocumentListener(fieldsListener);
        updateLoginButtonState();

        // El botón de actualizar empieza deshabilitado: solo se activa si el
        // StartupController confirma que hay conexión Y hay una versión distinta.
        updateButton.setEnabled(false);
    }

    private Dimension computeWindowSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * AppConstants.MAIN_WINDOW_SCREEN_RATIO);
        int height = (int) (screenSize.height * AppConstants.MAIN_WINDOW_SCREEN_RATIO);
        return new Dimension(width, height);
    }

    /** Estilo y alta de todos los componentes en backgroundPanel; se hace una sola vez (nunca en cada resize). */
    private void buildComponents() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(Theme.GOLD_TEXT);
        backgroundPanel.add(formBacking);
        backgroundPanel.add(titleLabel);

        usernameLabel.setForeground(Theme.GOLD_TEXT);
        backgroundPanel.add(usernameLabel);
        backgroundPanel.add(usernameField);

        passwordLabel.setForeground(Theme.GOLD_TEXT);
        backgroundPanel.add(passwordLabel);
        backgroundPanel.add(passwordField);

        statusLabel.setForeground(new Color(255, 120, 110));
        backgroundPanel.add(statusLabel);

        accountHintLabel.setForeground(Theme.GOLD_TEXT);
        accountHintLabel.setFont(accountHintLabel.getFont().deriveFont(Font.PLAIN, 12f));
        accountHintLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (accountHintVisible) {
                    openSunDWebsite();
                }
            }
        });
        backgroundPanel.add(accountHintLabel);

        styleButton(updateButton);
        backgroundPanel.add(updateButton);

        styleButton(exitButton);
        backgroundPanel.add(exitButton);

        styleButton(loginButton);
        backgroundPanel.add(loginButton);

        selfUpdateProgressBar.setStringPainted(false);
        selfUpdateProgressBar.setForeground(Theme.GOLD_ACCENT);
        selfUpdateProgressBar.setVisible(false);
        backgroundPanel.add(selfUpdateProgressBar);
    }

    /**
     * Calcula y aplica las posiciones (setBounds) de todo el formulario a
     * partir del tamaño actual de backgroundPanel. Se llama una vez al
     * construir la ventana y de nuevo cada vez que se redimensiona (ver el
     * ComponentListener del constructor), así que nunca usa el tamaño de
     * ventana original salvo la primera vez.
     */
    private void layoutComponents(int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }

        int formWidth = Math.min(420, w - 80);
        int formX = (w - formWidth) / 2;

        int titleY = (int) (h * 0.18);
        int fieldY = (int) (h * 0.30);
        int buttonY = fieldY + 168;
        int buttonHeight = 36;
        int lowerButtonY = buttonY + buttonHeight + 12;

        int backingPad = 24;
        formBacking.setBounds(formX - backingPad, titleY - backingPad,
                formWidth + backingPad * 2, (lowerButtonY + buttonHeight) - titleY + backingPad * 2);

        titleLabel.setBounds(formX, titleY, formWidth, 40);
        usernameLabel.setBounds(formX, fieldY, formWidth, 20);
        usernameField.setBounds(formX, fieldY + 22, formWidth, 32);
        passwordLabel.setBounds(formX, fieldY + 66, formWidth, 20);
        passwordField.setBounds(formX, fieldY + 88, formWidth, 32);
        statusLabel.setBounds(formX, fieldY + 122, formWidth, 20);
        accountHintLabel.setBounds(formX, fieldY + 142, formWidth, 18);
        updateButton.setBounds(formX, buttonY, formWidth, buttonHeight);
        selfUpdateProgressBar.setBounds(formX, buttonY, formWidth, buttonHeight);

        int halfWidth = (formWidth - 10) / 2;
        exitButton.setBounds(formX, lowerButtonY, halfWidth, buttonHeight);
        loginButton.setBounds(formX + halfWidth + 10, lowerButtonY, halfWidth, buttonHeight);
    }

    /** Look "SunDStudios" para los botones: piedra oscura, borde dorado, texto claro. Mismo criterio en las tres. */
    private static void styleButton(JButton button) {
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setForeground(Theme.GOLD_TEXT);
        button.setBackground(Theme.STONE_BUTTON);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.GOLD_ACCENT, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    /**
     * "Entrar" solo se habilita si hay usuario Y contraseña Y no hace falta actualizar el
     * launcher primero (updateButton.isEnabled() == hay actualización pendiente, ver
     * StartupController/setUpdateButtonEnabled). Mientras el launcher esté desactualizado,
     * el jugador debe quedarse en esta pantalla con el botón "Actualizar lanzador" activo,
     * nunca poder entrar directamente.
     */
    private void updateLoginButtonState() {
        boolean hasUsername = !usernameField.getText().trim().isEmpty();
        boolean hasPassword = passwordField.getPassword().length > 0;
        boolean updateRequired = updateButton.isEnabled();
        loginButton.setEnabled(hasUsername && hasPassword && !updateRequired);
    }

    // ---- Getters expuestos para que Action/Controller se enganchen desde fuera ----

    public JTextField getUsernameField() {
        return usernameField;
    }

    public JPasswordField getPasswordField() {
        return passwordField;
    }

    public JButton getLoginButton() {
        return loginButton;
    }

    public JButton getExitButton() {
        return exitButton;
    }

    public JButton getUpdateButton() {
        return updateButton;
    }

    private boolean accountHintVisible = false;

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            setAccountHintVisible(false);
        });
    }

    /**
     * Igual que setStatus, pero además muestra debajo un enlace clicable
     * "¿No tienes cuenta? Crea una en SunD.es" que abre el navegador. Pensado
     * para el caso de login fallido: el mensaje de error es siempre el mismo
     * genérico ("usuario o contraseña incorrectos") tanto si la cuenta no
     * existe como si la contraseña es la equivocada -nunca se distingue cuál
     * de los dos casos es, para no filtrar qué nombres de usuario existen-,
     * así que este enlace se ofrece siempre igual, sin depender de si la
     * cuenta existe de verdad.
     */
    public void setStatusWithAccountHint(String text) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            setAccountHintVisible(true);
        });
    }

    private void setAccountHintVisible(boolean visible) {
        accountHintVisible = visible;
        accountHintLabel.setText(visible ? "<html><u>&iquest;No tienes cuenta? Crea una en SunD.es</u></html>" : " ");
        accountHintLabel.setCursor(Cursor.getPredefinedCursor(visible ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void openSunDWebsite() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://sund.es"));
            }
        } catch (Exception ignored) {
            // No crítico: si no se puede abrir el navegador, el jugador puede visitar sund.es a mano.
        }
    }

    /**
     * "Salir" queda fuera a propósito: debe estar siempre disponible, incluso mientras hay
     * una comprobación de cuenta en curso (login manual o auto-login) — el jugador nunca
     * debe quedarse sin forma de cerrar el launcher.
     */
    public void setFormEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            usernameField.setEnabled(enabled);
            passwordField.setEnabled(enabled);
            if (enabled) {
                updateLoginButtonState();
            } else {
                loginButton.setEnabled(false);
            }
        });
    }

    /** También reevalúa "Entrar": si ya había usuario/contraseña rellenos, un cambio aquí puede habilitarlo o deshabilitarlo. */
    public void setUpdateButtonEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            updateButton.setEnabled(enabled);
            updateLoginButtonState();
        });
    }

    /**
     * Sustituye usuario/contraseña/Entrar/Actualizar por una barra de progreso, mientras
     * SelfUpdateService descarga la nueva versión y la aplica. "Salir" se queda visible y
     * activo a propósito (igual que el resto de la ventana, nunca se desactiva). Misma
     * forma que {@link es.sund.launcher.util.DownloadUtil.ProgressListener} para poder
     * pasarse directamente como listener.
     */
    public void showSelfUpdateProgress(String taskName, long bytesDone, long bytesTotal) {
        SwingUtilities.invokeLater(() -> {
            usernameLabel.setVisible(false);
            usernameField.setVisible(false);
            passwordLabel.setVisible(false);
            passwordField.setVisible(false);
            loginButton.setVisible(false);
            updateButton.setVisible(false);
            setAccountHintVisible(false);

            statusLabel.setText(taskName != null ? taskName : "Actualizando...");
            selfUpdateProgressBar.setVisible(true);
            if (bytesTotal > 0) {
                selfUpdateProgressBar.setIndeterminate(false);
                selfUpdateProgressBar.setValue((int) ((bytesDone * 100) / bytesTotal));
            } else {
                selfUpdateProgressBar.setIndeterminate(true);
            }
        });
    }

    /** Vuelve al formulario normal tras un fallo de autoactualización (si tiene éxito, el proceso se relanza y esta ventana ni llega a verse de nuevo). */
    public void hideSelfUpdateProgress() {
        SwingUtilities.invokeLater(() -> {
            selfUpdateProgressBar.setVisible(false);
            usernameLabel.setVisible(true);
            usernameField.setVisible(true);
            passwordLabel.setVisible(true);
            passwordField.setVisible(true);
            loginButton.setVisible(true);
            updateButton.setVisible(true);
            updateLoginButtonState();
        });
    }

    public void prefillCredentials(String username, char[] password) {
        SwingUtilities.invokeLater(() -> {
            usernameField.setText(username);
            passwordField.setText(new String(password));
            updateLoginButtonState();
        });
    }

    /**
     * Igual que prefillCredentials, pero además dispara el login automáticamente
     * (como si el jugador hubiera pulsado "Entrar") en cuanto usuario y contraseña
     * quedan rellenos, en vez de esperar un click manual. Usa doClick() sobre el
     * propio botón para reutilizar exactamente la misma validación que un click
     * real (LoginAction ya enganchado desde Main), no una copia paralela de esa
     * lógica.
     */
    public void prefillCredentialsAndAutoLogin(String username, char[] password) {
        SwingUtilities.invokeLater(() -> {
            usernameField.setText(username);
            passwordField.setText(new String(password));
            updateLoginButtonState();
            if (loginButton.isEnabled()) {
                loginButton.doClick();
            }
        });
    }
}
