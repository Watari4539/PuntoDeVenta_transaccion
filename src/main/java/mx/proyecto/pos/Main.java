package mx.proyecto.pos;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentaFrame().setVisible(true));
    }
}
