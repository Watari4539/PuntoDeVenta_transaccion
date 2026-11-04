package mx.proyecto.pos;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VentaFrame extends JFrame {
    private static final BigDecimal TASA_IVA = new BigDecimal("0.16");
    private static final NumberFormat MONEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"));

    private final VentaRepository repository = new VentaRepository();
    private final List<LineaVenta> lineas = new ArrayList<>();
    private final JTextField codigoField = new JTextField(22);
    private final JTextField cantidadField = new JTextField("1", 4);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Código", "Producto", "Cantidad", "Precio", "Importe"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel subtotalLabel = new JLabel();
    private final JLabel ivaLabel = new JLabel();
    private final JLabel totalLabel = new JLabel();

    public VentaFrame() {
        super("Punto de venta");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 460));
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        add(crearPanelCaptura(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(crearPanelPie(), BorderLayout.SOUTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(26);
        actualizarTotales();
    }

    private JPanel crearPanelCaptura() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        panel.setBorder(BorderFactory.createTitledBorder("Agregar producto"));
        panel.add(new JLabel("Código de barras:"));
        panel.add(codigoField);
        panel.add(new JLabel("Cantidad:"));
        panel.add(cantidadField);
        JButton agregar = new JButton("Agregar");
        agregar.addActionListener(event -> agregarProducto());
        codigoField.addActionListener(event -> agregarProducto());
        cantidadField.addActionListener(event -> agregarProducto());
        panel.add(agregar);
        return panel;
    }

    private JPanel crearPanelPie() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton quitar = new JButton("Quitar seleccionado");
        quitar.addActionListener(event -> quitarSeleccionado());
        JButton limpiar = new JButton("Nueva venta");
        limpiar.addActionListener(event -> limpiarVenta());
        JButton guardar = new JButton("Guardar venta");
        guardar.addActionListener(event -> guardarVenta());
        acciones.add(quitar);
        acciones.add(limpiar);
        acciones.add(guardar);

        JPanel resumen = new JPanel(new GridLayout(3, 2, 8, 4));
        resumen.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 12));
        resumen.add(new JLabel("Subtotal:"));
        resumen.add(subtotalLabel);
        resumen.add(new JLabel("IVA (16%):"));
        resumen.add(ivaLabel);
        JLabel totalTitulo = new JLabel("Total:");
        totalTitulo.setFont(totalTitulo.getFont().deriveFont(java.awt.Font.BOLD));
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalLabel.setFont(totalLabel.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        resumen.add(totalTitulo);
        resumen.add(totalLabel);

        panel.add(acciones, BorderLayout.WEST);
        panel.add(resumen, BorderLayout.EAST);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        return panel;
    }

    private void agregarProducto() {
        String codigo = codigoField.getText().trim();
        if (codigo.isEmpty()) return;
        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadField.getText().trim());
            if (cantidad < 1) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            mostrarError("La cantidad debe ser un número entero mayor que cero.");
            cantidadField.requestFocusInWindow();
            return;
        }

        try {
            Producto producto = repository.buscarProducto(codigo);
            if (producto == null) {
                mostrarError("No se encontró un producto activo con ese código.");
                codigoField.selectAll();
                codigoField.requestFocusInWindow();
                return;
            }
            int cantidadEnCarrito = lineas.stream()
                    .filter(linea -> linea.producto().codigo().equals(producto.codigo()))
                    .mapToInt(LineaVenta::cantidad).sum();
            if (cantidadEnCarrito + cantidad > producto.stock()) {
                mostrarError("Stock disponible: " + producto.stock() + ".");
                return;
            }
            int indice = indiceDe(producto.codigo());
            if (indice >= 0) {
                lineas.set(indice, lineas.get(indice).conCantidad(cantidadEnCarrito + cantidad));
            } else {
                lineas.add(new LineaVenta(producto, cantidad));
            }
            refrescarTabla();
            codigoField.setText("");
            cantidadField.setText("1");
            codigoField.requestFocusInWindow();
        } catch (SQLException error) {
            mostrarError("No se pudo consultar MySQL: " + error.getMessage());
        }
    }

    private void quitarSeleccionado() {
        int fila = table.getSelectedRow();
        if (fila < 0) {
            mostrarError("Selecciona primero un producto de la tabla.");
            return;
        }
        lineas.remove(fila);
        refrescarTabla();
    }

    private void limpiarVenta() {
        lineas.clear();
        codigoField.setText("");
        cantidadField.setText("1");
        refrescarTabla();
        codigoField.requestFocusInWindow();
    }

    private void guardarVenta() {
        if (lineas.isEmpty()) {
            mostrarError("Agrega al menos un producto antes de guardar.");
            return;
        }
        BigDecimal subtotal = calcularSubtotal();
        BigDecimal iva = subtotal.multiply(TASA_IVA).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);
        try {
            long folio = repository.guardarVenta(List.copyOf(lineas), subtotal, iva, total);
            JOptionPane.showMessageDialog(this,
                    "Venta guardada. Folio: " + folio + "\nTotal: " + MONEDA.format(total),
                    "Venta completada", JOptionPane.INFORMATION_MESSAGE);
            limpiarVenta();
        } catch (SQLException error) {
            mostrarError("No se guardó la venta; la transacción se revirtió. " + error.getMessage());
        }
    }

    private int indiceDe(String codigo) {
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).producto().codigo().equals(codigo)) return i;
        }
        return -1;
    }

    private BigDecimal calcularSubtotal() {
        return lineas.stream().map(LineaVenta::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private void refrescarTabla() {
        tableModel.setRowCount(0);
        for (LineaVenta linea : lineas) {
            tableModel.addRow(new Object[]{linea.producto().codigo(), linea.producto().nombre(),
                    linea.cantidad(), MONEDA.format(linea.producto().precio()), MONEDA.format(linea.importe())});
        }
        actualizarTotales();
    }

    private void actualizarTotales() {
        BigDecimal subtotal = calcularSubtotal();
        BigDecimal iva = subtotal.multiply(TASA_IVA).setScale(2, RoundingMode.HALF_UP);
        subtotalLabel.setText(MONEDA.format(subtotal));
        ivaLabel.setText(MONEDA.format(iva));
        totalLabel.setText(MONEDA.format(subtotal.add(iva).setScale(2, RoundingMode.HALF_UP)));
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}
