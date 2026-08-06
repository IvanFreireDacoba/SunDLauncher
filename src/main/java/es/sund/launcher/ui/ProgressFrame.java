package es.sund.launcher.ui;

import javax.swing.*;
import java.awt.*;

public class ProgressFrame extends JFrame {

    private static final long serialVersionUID = 6219922905745115489L;
	private final JLabel taskLabel = new JLabel("Preparando...");
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    public ProgressFrame() {
        super("SunD Launcher - Instalando");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        taskLabel.setBounds(0, 0, 0, 0); // sin uso de layout absoluto aquí, BorderLayout ya coloca
        panel.add(taskLabel, BorderLayout.NORTH);

        progressBar.setPreferredSize(new Dimension(360, 22));
        progressBar.setStringPainted(true);
        panel.add(progressBar, BorderLayout.CENTER);

        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    public void update(String taskName, long done, long total) {
        SwingUtilities.invokeLater(() -> {
            taskLabel.setText(taskName);
            if (total > 0) {
                progressBar.setIndeterminate(false);
                progressBar.setValue((int) ((done * 100) / total));
            } else {
                progressBar.setIndeterminate(true);
            }
        });
    }

    public void setTask(String text) {
        SwingUtilities.invokeLater(() -> taskLabel.setText(text));
    }
}
