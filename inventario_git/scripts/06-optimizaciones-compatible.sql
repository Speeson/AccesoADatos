-- ============================================
-- SCRIPT DE OPTIMIZACIONES (COMPATIBLE)
-- Fase II - Compatible con MySQL 5.7 y MariaDB
-- ============================================

USE inventario_db;

-- ============================================
-- ELIMINAR ÍNDICES SI EXISTEN (para recrear)
-- ============================================

-- Productos
DROP INDEX IF EXISTS idx_productos_nombre ON productos;
DROP INDEX IF EXISTS idx_productos_nombre_fulltext ON productos;
DROP INDEX IF EXISTS idx_productos_precio ON productos;
DROP INDEX IF EXISTS idx_productos_cat_precio_stock ON productos;
DROP INDEX IF EXISTS idx_productos_precio_stock ON productos;
DROP INDEX IF EXISTS idx_productos_categoria_nombre ON productos;
DROP INDEX IF EXISTS idx_productos_categoria_stock ON productos;

-- Movimientos
DROP INDEX IF EXISTS idx_movimientos_tipo_producto ON movimientos_stock;
DROP INDEX IF EXISTS idx_movimientos_fecha_tipo ON movimientos_stock;
DROP INDEX IF EXISTS idx_movimientos_usuario ON movimientos_stock;

-- Categorías
DROP INDEX IF EXISTS idx_categorias_nombre ON categorias;

-- ============================================
-- CREAR ÍNDICES OPTIMIZADOS
-- ============================================

SELECT 'Creando índices para tabla PRODUCTOS...' as Estado;

-- Índice para búsquedas por nombre
CREATE INDEX idx_productos_nombre ON productos(nombre);

-- Índice FULLTEXT para búsquedas de texto
CREATE FULLTEXT INDEX idx_productos_nombre_fulltext ON productos(nombre);

-- Índice para filtros de precio
CREATE INDEX idx_productos_precio ON productos(precio);

-- Índice compuesto (COVERING INDEX)
CREATE INDEX idx_productos_cat_precio_stock ON productos(categoria, precio, stock);

-- Índice para valor total inventario
CREATE INDEX idx_productos_precio_stock ON productos(precio, stock);

-- Índice compuesto categoría+nombre
CREATE INDEX idx_productos_categoria_nombre ON productos(categoria, nombre);

-- Índice categoría+stock
CREATE INDEX idx_productos_categoria_stock ON productos(categoria, stock);

SELECT 'Índices de PRODUCTOS creados ✓' as Estado;

-- ============================================
-- MOVIMIENTOS_STOCK
-- ============================================

SELECT 'Creando índices para tabla MOVIMIENTOS_STOCK...' as Estado;

CREATE INDEX idx_movimientos_tipo_producto ON movimientos_stock(tipo_movimiento, id_producto);
CREATE INDEX idx_movimientos_fecha_tipo ON movimientos_stock(fecha_movimiento, tipo_movimiento);
CREATE INDEX idx_movimientos_usuario ON movimientos_stock(usuario);

SELECT 'Índices de MOVIMIENTOS_STOCK creados ✓' as Estado;

-- ============================================
-- CATEGORÍAS
-- ============================================

SELECT 'Creando índices para tabla CATEGORIAS...' as Estado;

CREATE INDEX idx_categorias_nombre ON categorias(nombre);

SELECT 'Índices de CATEGORIAS creados ✓' as Estado;

-- ============================================
-- ACTUALIZAR ESTADÍSTICAS
-- ============================================

SELECT 'Actualizando estadísticas...' as Estado;

ANALYZE TABLE productos;
ANALYZE TABLE categorias;
ANALYZE TABLE movimientos_stock;

SELECT 'Estadísticas actualizadas ✓' as Estado;

-- ============================================
-- VERIFICACIÓN
-- ============================================

SELECT '========================================' as '';
SELECT 'VERIFICACIÓN DE ÍNDICES CREADOS' as '';
SELECT '========================================' as '';

SELECT
    TABLE_NAME as Tabla,
    COUNT(DISTINCT INDEX_NAME) as Total_Indices
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
GROUP BY TABLE_NAME;

SELECT '========================================' as '';

-- Detalle de índices de productos
SELECT
    INDEX_NAME as Indice,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as Columnas,
    INDEX_TYPE as Tipo
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME = 'productos'
GROUP BY INDEX_NAME, INDEX_TYPE
ORDER BY INDEX_NAME;

SELECT '========================================' as '';
SELECT 'OPTIMIZACIONES APLICADAS CORRECTAMENTE ✓' as '';
SELECT '========================================' as '';
