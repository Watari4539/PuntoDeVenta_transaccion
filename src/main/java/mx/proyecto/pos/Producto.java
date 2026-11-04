package mx.proyecto.pos;

import java.math.BigDecimal;

public record Producto(String codigo, String nombre, BigDecimal precio, int stock) {}
