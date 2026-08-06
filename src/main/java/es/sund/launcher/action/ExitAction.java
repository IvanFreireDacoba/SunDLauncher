package es.sund.launcher.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/** Cierra la aplicación. Es su propia clase por consistencia y para poder añadir lógica de cierre limpio más adelante. */
public class ExitAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        System.exit(0);
    }
}
