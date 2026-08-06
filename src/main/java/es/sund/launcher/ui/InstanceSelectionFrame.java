package es.sund.launcher.ui;

import es.sund.launcher.config.AppConstants;
import es.sund.launcher.model.GameInstance;
import es.sund.launcher.util.IcoImageLoader;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla de selección de instancias (estilo Riot Client), mostrada tras el
 * login (manual o automático) en vez de lanzar el juego directamente.
 *
 * Layout: una columna a la izquierda con la lista de instancias (franjas
 * seleccionables de ancho completo, ver InstanceGridTile) y los datos del
 * jugador debajo; a la derecha, un CardLayout con el panel de detalle de
 * lo seleccionado. Al
 * pulsar una instancia se ve su InstancePanel (imagen + datos + controles
 * Instalar/Jugar/Desinstalar); al pulsar la sección de perfil se ve un
 * ProfileScreen con los datos del jugador y un desplegable de ajustes. Todos
 * los paneles de detalle se crean de una vez en showInstances() y no se
 * reconstruyen al cambiar de selección: solo cambia cuál de las cartas ya
 * existentes se ve, así que el ActionListener del botón (enganchado desde
 * fuera, en PlayOrInstallAction) no se pierde.
 *
 * Esta clase SOLO construye y expone componentes de UI: no llama a la API, no
 * toca ficheros, no instala ni lanza Minecraft. Esa lógica vive en
 * InstanceCatalogController y PlayOrInstallAction, que se enganchan a esta
 * ventana desde fuera.
 *
 * Tiene un constructor público sin argumentos a propósito: es un requisito de
 * WindowBuilder para poder abrir la pestaña "Design". Las instancias se
 * añaden después, con showInstances(), igual que MainFrame precarga
 * credenciales tras construirse.
 */
public class InstanceSelectionFrame extends JFrame {

    private static final long serialVersionUID = 5192447561094413882L;

    private static final int SIDEBAR_WIDTH = 260;
    private static final Color STATUS_ERROR = new Color(255, 120, 110);

    /** Alto fijo de cada franja de instancia en la columna izquierda; ver InstanceGridTile. El ancho se estira al de la columna. */
    static final int INSTANCE_TILE_HEIGHT = 110;
    private static final int TILE_GAP = 10;

    private static final String CARD_PROFILE = "__profile__";

    private final JPanel sidebarGrid = new ScrollableStackPanel();
    private final JPanel detailCards = new JPanel(new CardLayout());
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel profileNameLabel = new JLabel(" ");
    private final ProfileScreen profileScreen = new ProfileScreen();
    private final Map<Integer, InstancePanel> panelsByInstanceId = new LinkedHashMap<>();
    private final Map<Integer, InstanceGridTile> tilesByInstanceId = new LinkedHashMap<>();
    private JComponent profilePanel;
    private boolean profileSelected;

    /** Tamaño mínimo para que el grid/panel de detalle no lleguen a deformarse; sin máximo, admite pantalla completa. */
    private static final int MIN_WIDTH = 760;
    private static final int MIN_HEIGHT = 520;

    public InstanceSelectionFrame() {
        super("SunD Launcher - Selecciona un juego");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setIconImages(IcoImageLoader.loadAllSizes(AppConstants.WINDOW_ICON_RESOURCE));

        Dimension windowSize = computeWindowSize();
        setSize(windowSize);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(Theme.SIDEBAR_BACKGROUND);

        statusLabel.setForeground(STATUS_ERROR);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        detailCards.add(profileScreen, CARD_PROFILE);

        contentPane.add(statusLabel, BorderLayout.NORTH);
        contentPane.add(buildSidebar(), BorderLayout.WEST);
        contentPane.add(detailCards, BorderLayout.CENTER);

        setContentPane(contentPane);
    }

    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setOpaque(true);
        sidebar.setBackground(Theme.SIDEBAR_BACKGROUND);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.SIDEBAR_DIVIDER));

        // BoxLayout.Y_AXIS: cada InstanceGridTile es una franja de ancho
        // completo (de lado a lado de la columna) y alto fijo
        // (INSTANCE_TILE_HEIGHT), apiladas una debajo de otra. El ancho lo
        // estira el propio BoxLayout (ver InstanceGridTile.getMaximumSize):
        // no queda hueco vacío a los lados como pasaba con el grid de
        // cuadrados anterior.
        sidebarGrid.setLayout(new BoxLayout(sidebarGrid, BoxLayout.Y_AXIS));
        sidebarGrid.setOpaque(false);
        sidebarGrid.setBorder(BorderFactory.createEmptyBorder(TILE_GAP, TILE_GAP, TILE_GAP, TILE_GAP));

        JScrollPane scrollPane = new JScrollPane(sidebarGrid);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        sidebar.add(scrollPane, BorderLayout.CENTER);
        sidebar.add(buildProfilePanel(), BorderLayout.SOUTH);
        return sidebar;
    }

    /**
     * Fila de perfil, ahora seleccionable como si fuera una instancia más: un
     * click en cualquier punto (no un botón pequeño aparte) lleva el panel de
     * detalle a ProfileScreen, con los datos del jugador y el desplegable de
     * ajustes (sustituye al antiguo botón de rueda con JPopupMenu).
     */
    private JComponent buildProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                if (profileSelected) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(Theme.SIDEBAR_SELECTED);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Theme.GOLD_ACCENT);
                    g2.fillRect(0, 0, 3, getHeight());
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(SIDEBAR_WIDTH, Theme.PROFILE_SECTION_HEIGHT));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SIDEBAR_DIVIDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 14)));
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectProfile();
            }
        });

        JPanel textColumn = new JPanel();
        textColumn.setOpaque(false);
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));

        JLabel caption = new JLabel("SESIÓN INICIADA COMO");
        caption.setForeground(Theme.GOLD_TEXT_MUTED);
        caption.setFont(caption.getFont().deriveFont(Font.PLAIN, 10.5f));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);

        profileNameLabel.setForeground(Theme.GOLD_TEXT);
        profileNameLabel.setFont(profileNameLabel.getFont().deriveFont(Font.BOLD, 15f));
        profileNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileNameLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        textColumn.add(Box.createVerticalGlue());
        textColumn.add(caption);
        textColumn.add(profileNameLabel);
        textColumn.add(Box.createVerticalGlue());

        JLabel chevron = new JLabel("›");
        chevron.setForeground(Theme.GOLD_TEXT_MUTED);
        chevron.setFont(chevron.getFont().deriveFont(Font.BOLD, 18f));

        panel.add(textColumn, BorderLayout.CENTER);
        panel.add(chevron, BorderLayout.EAST);
        this.profilePanel = panel;
        return panel;
    }

    private Dimension computeWindowSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * AppConstants.MAIN_WINDOW_SCREEN_RATIO);
        int height = (int) (screenSize.height * AppConstants.MAIN_WINDOW_SCREEN_RATIO);
        return new Dimension(width, height);
    }

    /** Datos del perfil del jugador mostrados debajo del grid de instancias. */
    public void setUsername(String username) {
        profileNameLabel.setText(username != null && !username.isBlank() ? username : " ");
        profileScreen.setUsername(username);
    }

    /**
     * Construye una franja (columna izquierda) y una carta de detalle por
     * instancia. La primera instancia de la lista queda seleccionada por
     * defecto para que la pantalla no se vea vacía al entrar.
     */
    public void showInstances(List<GameInstance> instances) {
        sidebarGrid.removeAll();
        // Ojo: detailCards también contiene profileScreen (añadido en el
        // constructor bajo CARD_PROFILE), así que no se vacía aquí entero;
        // solo se quitan las cartas de instancia de la vez anterior (si la
        // hubiera) antes de limpiar el mapa y reconstruirlas.
        for (InstancePanel oldPanel : panelsByInstanceId.values()) {
            detailCards.remove(oldPanel);
        }
        panelsByInstanceId.clear();
        tilesByInstanceId.clear();

        boolean first = true;
        for (GameInstance instance : instances) {
            InstancePanel detailPanel = new InstancePanel();
            detailPanel.setBackgroundImageResource(String.format(AppConstants.INSTANCE_BACKGROUND_RESOURCE_PATTERN, instance.id));
            panelsByInstanceId.put(instance.id, detailPanel);
            detailCards.add(detailPanel, String.valueOf(instance.id));

            if (!first) {
                sidebarGrid.add(Box.createVerticalStrut(TILE_GAP));
            }
            first = false;

            InstanceGridTile tile = new InstanceGridTile();
            tile.setInstanceName(instance.name);
            tile.setBackgroundImageResource(String.format(AppConstants.INSTANCE_BACKGROUND_RESOURCE_PATTERN, instance.id));
            tile.addActionListener(e -> selectInstance(instance.id));
            tilesByInstanceId.put(instance.id, tile);
            sidebarGrid.add(tile);
        }

        sidebarGrid.revalidate();
        sidebarGrid.repaint();

        if (!instances.isEmpty()) {
            selectInstance(instances.get(0).id);
        }
    }

    /** Muestra el panel de detalle de la instancia dada y resalta su cuadro en el grid (quitando la selección del perfil). */
    private void selectInstance(int instanceId) {
        CardLayout layout = (CardLayout) detailCards.getLayout();
        layout.show(detailCards, String.valueOf(instanceId));
        for (Map.Entry<Integer, InstanceGridTile> entry : tilesByInstanceId.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == instanceId);
        }
        setProfileSelected(false);
    }

    /** Muestra ProfileScreen en el panel de detalle y resalta la fila de perfil (quitando la selección de instancias). */
    private void selectProfile() {
        CardLayout layout = (CardLayout) detailCards.getLayout();
        layout.show(detailCards, CARD_PROFILE);
        for (InstanceGridTile tile : tilesByInstanceId.values()) {
            tile.setSelected(false);
        }
        setProfileSelected(true);
    }

    private void setProfileSelected(boolean selected) {
        profileSelected = selected;
        if (profilePanel != null) {
            profilePanel.repaint();
        }
    }

    public InstancePanel getInstancePanel(int instanceId) {
        return panelsByInstanceId.get(instanceId);
    }

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }
}
