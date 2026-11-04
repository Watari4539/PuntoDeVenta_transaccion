CREATE DATABASE IF NOT EXISTS pos_transacciones
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE pos_transacciones;

CREATE TABLE IF NOT EXISTS productos (
    codigo VARCHAR(64) PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_productos_precio CHECK (precio >= 0),
    CONSTRAINT chk_productos_stock CHECK (stock >= 0)
);

CREATE TABLE IF NOT EXISTS ventas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(12, 2) NOT NULL,
    iva DECIMAL(12, 2) NOT NULL,
    total DECIMAL(12, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS detalles_venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    codigo_producto VARCHAR(64) NOT NULL,
    descripcion VARCHAR(120) NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    importe DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_detalle_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (codigo_producto) REFERENCES productos(codigo),
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0)
);

INSERT INTO productos (codigo, nombre, precio, stock) VALUES
    ('7501000000011', 'Agua 600 ml', 12.00, 30),
    ('7501000000028', 'Galletas', 18.50, 20),
    ('7501000000035', 'Refresco 600 ml', 22.00, 15)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
