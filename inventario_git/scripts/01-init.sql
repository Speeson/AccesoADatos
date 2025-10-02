-- Crear base de datos si no existe
CREATE DATABASE IF NOT EXISTS inventario_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE inventario_db;

-- Tabla de categorías
CREATE TABLE IF NOT EXISTS categorias (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabla de productos
CREATE TABLE IF NOT EXISTS productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    categoria VARCHAR(100) NOT NULL,
    precio DECIMAL(10,2) NOT NULL CHECK (precio >= 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria) REFERENCES categorias(nombre) ON UPDATE CASCADE
);

-- Tabla de movimientos de stock
CREATE TABLE IF NOT EXISTS movimientos_stock (
    id_movimiento INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    tipo_movimiento ENUM('ENTRADA', 'SALIDA') NOT NULL,
    cantidad INT NOT NULL CHECK (cantidad > 0),
    stock_anterior INT NOT NULL,
    stock_nuevo INT NOT NULL,
    motivo VARCHAR(255),
    fecha_movimiento TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario VARCHAR(100) DEFAULT 'sistema',
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
);

-- Tabla de logs de aplicación
CREATE TABLE IF NOT EXISTS logs_aplicacion (
    id_log INT AUTO_INCREMENT PRIMARY KEY,
    nivel VARCHAR(20) NOT NULL,
    mensaje TEXT NOT NULL,
    clase VARCHAR(255),
    metodo VARCHAR(100),
    fecha_log TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    detalles JSON
);

-- Índices para mejorar rendimiento
CREATE INDEX idx_productos_categoria ON productos(categoria);
CREATE INDEX idx_productos_stock_bajo ON productos(stock);
CREATE INDEX idx_movimientos_producto ON movimientos_stock(id_producto);
CREATE INDEX idx_movimientos_fecha ON movimientos_stock(fecha_movimiento);
CREATE INDEX idx_logs_fecha ON logs_aplicacion(fecha_log);
CREATE INDEX idx_logs_nivel ON logs_aplicacion(nivel);

-- Insertar categorías por defecto
INSERT IGNORE INTO categorias (nombre, descripcion) VALUES
('Electronica', 'Dispositivos electrónicos y accesorios'),
('Ropa', 'Prendas de vestir y complementos'),
('Informatica', 'Equipos y accesorios informáticos'),
('Hogar', 'Artículos para el hogar'),
('Alimentacion', 'Productos alimenticios');
