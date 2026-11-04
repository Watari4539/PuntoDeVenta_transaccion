package mx.proyecto.pos;

import java.math.BigDecimal;

public record LineaVenta(Producto producto, int cantidad) {
    public BigDecimal importe() {
        return producto.precio().multiply(BigDecimal.valueOf(cantidad));
    }

    public LineaVenta conCantidad(int nuevaCantidad) {
        return new LineaVenta(producto, nuevaCantidad);
    }
}
