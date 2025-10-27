-- ============================================
-- SCRIPT DE OPTIMIZACIÓN DE ÍNDICES
-- Fase II - Actividad Evaluable 2
-- ============================================

USE inventario_db;

-- ============================================
-- ÍNDICES PARA CONSULTAS EXISTENTES
-- ============================================

-- Índice para búsquedas por nombre (mejora LIKE 'nombre%')
CREATE INDEX IF NOT EXISTS idx_productos_nombre
ON productos(nombre);

-- Índice para filtros de rango de precios
CREATE INDEX IF NOT EXISTS idx_productos_precio
ON productos(precio);

-- Índice compuesto para estadísticas por categoría
-- (categoría ya tiene índice, pero esto mejora las agregaciones)
CREATE INDEX IF NOT EXISTS idx_productos_cat_precio_stock
ON productos(categoria, precio, stock);

-- ============================================
-- ÍNDICES PARA CONSULTAS AVANZADAS (Fase II)
-- ============================================

-- Índice compuesto para Top N productos más vendidos
-- Mejora la consulta de movimientos tipo SALIDA agrupados por producto
CREATE INDEX IF NOT EXISTS idx_movimientos_tipo_producto
ON movimientos_stock(tipo_movimiento, id_producto);

-- Índice compuesto para histórico con filtros complejos
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha_tipo
ON movimientos_stock(fecha_movimiento, tipo_movimiento);

-- Índice para usuario (útil si necesitas filtrar por usuario)
CREATE INDEX IF NOT EXISTS idx_movimientos_usuario
ON movimientos_stock(usuario);

-- ============================================
-- ÍNDICES PARA CATEGORÍAS
-- ============================================

-- Índice para búsquedas de categorías por nombre
CREATE INDEX IF NOT EXISTS idx_categorias_nombre
ON categorias(nombre);

-- ============================================
-- VERIFICACIÓN DE ÍNDICES
-- ============================================

-- Ver todos los índices de la tabla productos
SHOW INDEX FROM productos;

-- Ver todos los índices de la tabla movimientos_stock
SHOW INDEX FROM movimientos_stock;

-- Ver todos los índices de la tabla categorias
SHOW INDEX FROM categorias;
