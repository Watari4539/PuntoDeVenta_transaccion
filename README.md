# Punto de venta: manejo de transacciones

Aplicación de escritorio pequeña en Java Swing para capturar productos por código de barras y registrar ventas en MySQL. La interfaz es intencionalmente sencilla y no replica las imágenes de referencia.

## Requisitos

- Java 21
- Maven
- MySQL Server iniciado localmente (MySQL Workbench sirve para ejecutar el script y revisar las tablas)

## Preparar la base de datos

1. Abre `sql/schema.sql` en MySQL Workbench.
2. Ejecuta el script. Crea la base `pos_transacciones`, las tablas `productos`, `ventas`, `detalles_venta` y tres productos de ejemplo.
3. Si tu usuario o contraseña de MySQL no son los valores locales por defecto, configura `DB_USER` y `DB_PASSWORD` en el entorno desde el que iniciarás la aplicación.

La conexión usa `jdbc:mysql://localhost:3306/pos_transacciones`. Puedes reemplazarla definiendo `DB_URL`.

## Ejecutar

Desde la carpeta del proyecto:

```sh
mvn compile exec:java
```

Escanea o escribe el código de barras, indica la cantidad y presiona Enter o **Agregar**. Selecciona una fila para quitar un producto. **Guardar venta** inserta la venta y sus detalles, descuenta existencias y confirma todo en una sola transacción; ante un error, revierte los cambios.

La tasa de IVA está definida como 16% en `VentaFrame.java` para esta versión de ejemplo.
