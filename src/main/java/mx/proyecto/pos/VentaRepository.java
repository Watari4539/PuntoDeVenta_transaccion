package mx.proyecto.pos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class VentaRepository {
    public Producto buscarProducto(String codigo) throws SQLException {
        String sql = "SELECT codigo, nombre, precio, stock FROM productos WHERE codigo = ? AND activo = 1";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, codigo);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new Producto(result.getString("codigo"), result.getString("nombre"),
                        result.getBigDecimal("precio"), result.getInt("stock"));
            }
        }
    }

    public long guardarVenta(List<LineaVenta> lineas, BigDecimal subtotal, BigDecimal iva,
                             BigDecimal total) throws SQLException {
        String insertarVenta = "INSERT INTO ventas (subtotal, iva, total) VALUES (?, ?, ?)";
        String descontarStock = "UPDATE productos SET stock = stock - ? " +
                "WHERE codigo = ? AND activo = 1 AND stock >= ?";
        String insertarDetalle = "INSERT INTO detalles_venta " +
                "(venta_id, codigo_producto, descripcion, cantidad, precio_unitario, importe) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long ventaId;
                try (PreparedStatement statement = connection.prepareStatement(insertarVenta,
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setBigDecimal(1, subtotal);
                    statement.setBigDecimal(2, iva);
                    statement.setBigDecimal(3, total);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No se pudo obtener el folio de la venta.");
                        ventaId = keys.getLong(1);
                    }
                }

                try (PreparedStatement stockStatement = connection.prepareStatement(descontarStock);
                     PreparedStatement detailStatement = connection.prepareStatement(insertarDetalle)) {
                    for (LineaVenta linea : lineas) {
                        int cantidad = linea.cantidad();
                        stockStatement.setInt(1, cantidad);
                        stockStatement.setString(2, linea.producto().codigo());
                        stockStatement.setInt(3, cantidad);
                        if (stockStatement.executeUpdate() != 1) {
                            throw new SQLException("Stock insuficiente para: " + linea.producto().nombre());
                        }

                        detailStatement.setLong(1, ventaId);
                        detailStatement.setString(2, linea.producto().codigo());
                        detailStatement.setString(3, linea.producto().nombre());
                        detailStatement.setInt(4, cantidad);
                        detailStatement.setBigDecimal(5, linea.producto().precio());
                        detailStatement.setBigDecimal(6, linea.importe());
                        detailStatement.executeUpdate();
                    }
                }
                connection.commit();
                return ventaId;
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
