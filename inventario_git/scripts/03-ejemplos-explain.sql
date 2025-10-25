-- ============================================
-- EJEMPLOS DE ANÁLISIS EXPLAIN
-- Fase II - Actividad Evaluable 2
-- ============================================
-- Este script muestra cómo usar EXPLAIN para analizar
-- el rendimiento de las consultas SQL
-- ============================================

USE inventario_db;

-- ============================================
-- ACTIVAR PROFILING PARA MEDIR TIEMPOS
-- ============================================
SET profiling = 1;

-- ============================================
-- 1. TOP N PRODUCTOS MÁS VENDIDOS
-- ============================================

-- Mostrar plan de ejecución
EXPLAIN
SELECT
    p.id_producto,
    p.nombre,
    p.categoria,
    p.precio,
    p.stock as stock_actual,
    SUM(m.cantidad) as total_vendido,
    COUNT(m.id_movimiento) as num_transacciones,
    SUM(m.cantidad * p.precio) as ingresos_generados
FROM productos p
INNER JOIN movimientos_stock m ON p.id_producto = m.id_producto
WHERE m.tipo_movimiento = 'SALIDA'
GROUP BY p.id_producto, p.nombre, p.categoria, p.precio, p.stock
ORDER BY total_vendido DESC
LIMIT 10;

-- Ejecutar consulta real para medir tiempo
SELECT
    p.id_producto,
    p.nombre,
    p.categoria,
    p.precio,
    p.stock as stock_actual,
    SUM(m.cantidad) as total_vendido,
    COUNT(m.id_movimiento) as num_transacciones,
    SUM(m.cantidad * p.precio) as ingresos_generados
FROM productos p
INNER JOIN movimientos_stock m ON p.id_producto = m.id_producto
WHERE m.tipo_movimiento = 'SALIDA'
GROUP BY p.id_producto, p.nombre, p.categoria, p.precio, p.stock
ORDER BY total_vendido DESC
LIMIT 10;

-- ============================================
-- 2. VALOR TOTAL DE STOCK POR CATEGORÍA
-- ============================================

EXPLAIN
SELECT
    categoria,
    COUNT(*) as total_productos,
    SUM(stock) as unidades_stock,
    MIN(precio) as precio_minimo,
    MAX(precio) as precio_maximo,
    AVG(precio) as precio_promedio,
    SUM(precio * stock) as valor_total_stock
FROM productos
GROUP BY categoria
ORDER BY valor_total_stock DESC;

-- Ejecutar consulta real
SELECT
    categoria,
    COUNT(*) as total_productos,
    SUM(stock) as unidades_stock,
    MIN(precio) as precio_minimo,
    MAX(precio) as precio_maximo,
    AVG(precio) as precio_promedio,
    SUM(precio * stock) as valor_total_stock
FROM productos
GROUP BY categoria
ORDER BY valor_total_stock DESC;

-- ============================================
-- 3. HISTÓRICO DE MOVIMIENTOS
-- ============================================

EXPLAIN
SELECT
    m.id_movimiento,
    m.fecha_movimiento,
    p.id_producto,
    p.nombre as producto,
    p.categoria,
    m.tipo_movimiento,
    m.cantidad,
    m.stock_anterior,
    m.stock_nuevo,
    m.motivo,
    m.usuario,
    p.precio,
    (m.cantidad * p.precio) as valor_movimiento
FROM movimientos_stock m
INNER JOIN productos p ON m.id_producto = p.id_producto
WHERE m.fecha_movimiento BETWEEN DATE_SUB(NOW(), INTERVAL 30 DAY) AND NOW()
ORDER BY m.fecha_movimiento DESC;

-- Ejecutar consulta real
SELECT
    m.id_movimiento,
    m.fecha_movimiento,
    p.id_producto,
    p.nombre as producto,
    p.categoria,
    m.tipo_movimiento,
    m.cantidad,
    m.stock_anterior,
    m.stock_nuevo,
    m.motivo,
    m.usuario,
    p.precio,
    (m.cantidad * p.precio) as valor_movimiento
FROM movimientos_stock m
INNER JOIN productos p ON m.id_producto = p.id_producto
WHERE m.fecha_movimiento BETWEEN DATE_SUB(NOW(), INTERVAL 30 DAY) AND NOW()
ORDER BY m.fecha_movimiento DESC
LIMIT 100;

-- ============================================
-- 4. PRODUCTOS CON BAJO STOCK + HISTÓRICO
-- ============================================

EXPLAIN
SELECT
    p.id_producto,
    p.nombre,
    p.categoria,
    p.stock,
    p.precio,
    COUNT(DISTINCT m.id_movimiento) as movimientos_recientes,
    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'ENTRADA' THEN m.cantidad ELSE 0 END), 0) as entradas_recientes,
    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) as salidas_recientes,
    MAX(m.fecha_movimiento) as ultimo_movimiento
FROM productos p
LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
    AND m.fecha_movimiento >= DATE_SUB(NOW(), INTERVAL 30 DAY)
WHERE p.stock < 50
GROUP BY p.id_producto, p.nombre, p.categoria, p.stock, p.precio
ORDER BY p.stock ASC, salidas_recientes DESC;

-- Ejecutar consulta real
SELECT
    p.id_producto,
    p.nombre,
    p.categoria,
    p.stock,
    p.precio,
    COUNT(DISTINCT m.id_movimiento) as movimientos_recientes,
    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'ENTRADA' THEN m.cantidad ELSE 0 END), 0) as entradas_recientes,
    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) as salidas_recientes,
    MAX(m.fecha_movimiento) as ultimo_movimiento
FROM productos p
LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
    AND m.fecha_movimiento >= DATE_SUB(NOW(), INTERVAL 30 DAY)
WHERE p.stock < 50
GROUP BY p.id_producto, p.nombre, p.categoria, p.stock, p.precio
ORDER BY p.stock ASC, salidas_recientes DESC;

-- ============================================
-- 5. PRODUCTOS SIN MOVIMIENTOS
-- ============================================

EXPLAIN
SELECT
    p.id_producto,
    p.nombre,
    p.categoria,
    p.stock,
    p.precio,
    p.fecha_creacion,
    MAX(m.fecha_movimiento) as ultimo_movimiento,
    DATEDIFF(NOW(), COALESCE(MAX(m.fecha_movimiento), p.fecha_creacion)) as dias_sin_actividad
FROM productos p
LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
GROUP BY p.id_producto, p.nombre, p.categoria, p.stock, p.precio, p.fecha_creacion
HAVING dias_sin_actividad >= 30
ORDER BY dias_sin_actividad DESC;

-- Ejecutar consulta real
SELECT
    p.id_producto,
    p.nombre,
    p.categoria,
    p.stock,
    p.precio,
    p.fecha_creacion,
    MAX(m.fecha_movimiento) as ultimo_movimiento,
    DATEDIFF(NOW(), COALESCE(MAX(m.fecha_movimiento), p.fecha_creacion)) as dias_sin_actividad
FROM productos p
LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
GROUP BY p.id_producto, p.nombre, p.categoria, p.stock, p.precio, p.fecha_creacion
HAVING dias_sin_actividad >= 30
ORDER BY dias_sin_actividad DESC;

-- ============================================
-- 6. ANÁLISIS DE ROTACIÓN POR CATEGORÍA
-- ============================================

EXPLAIN
SELECT
    p.categoria,
    COUNT(DISTINCT p.id_producto) as total_productos,
    SUM(p.stock) as stock_total,
    SUM(p.precio * p.stock) as valor_inventario,
    COUNT(DISTINCT CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.id_movimiento END) as total_ventas,
    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) as unidades_vendidas,
    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'ENTRADA' THEN m.cantidad ELSE 0 END), 0) as unidades_compradas,
    ROUND(
        COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) /
        NULLIF(AVG(p.stock), 0),
    2) as indice_rotacion
FROM productos p
LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
    AND m.fecha_movimiento >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY p.categoria
ORDER BY indice_rotacion DESC;

-- Ejecutar consulta real
SELECT
    p.categoria,
    COUNT(DISTINCT p.id_producto) as total_productos,
    SUM(p.stock) as stock_total,
    SUM(p.precio * p.stock) as valor_inventario,
    COUNT(DISTINCT CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.id_movimiento END) as total_ventas,
    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) as unidades_vendidas,
    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'ENTRADA' THEN m.cantidad ELSE 0 END), 0) as unidades_compradas,
    ROUND(
        COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) /
        NULLIF(AVG(p.stock), 0),
    2) as indice_rotacion
FROM productos p
LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
    AND m.fecha_movimiento >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY p.categoria
ORDER BY indice_rotacion DESC;

-- ============================================
-- VER PERFILES DE TIEMPO
-- ============================================

-- Mostrar todos los tiempos de ejecución
SHOW PROFILES;

-- Para ver detalles de una consulta específica (cambiar número según corresponda)
-- SHOW PROFILE FOR QUERY 1;

-- ============================================
-- VERIFICAR ÍNDICES UTILIZADOS
-- ============================================

-- Índices de tabla productos
SHOW INDEX FROM productos;

-- Índices de tabla movimientos_stock
SHOW INDEX FROM movimientos_stock;

-- Índices de tabla categorias
SHOW INDEX FROM categorias;

-- ============================================
-- ESTADÍSTICAS DE TABLAS
-- ============================================

-- Información de tabla productos
SHOW TABLE STATUS LIKE 'productos';

-- Información de tabla movimientos_stock
SHOW TABLE STATUS LIKE 'movimientos_stock';

-- Información de tabla categorias
SHOW TABLE STATUS LIKE 'categorias';

-- ============================================
-- ANÁLISIS DETALLADO DE ÍNDICE
-- ============================================

-- Usar EXPLAIN EXTENDED para información adicional
EXPLAIN EXTENDED
SELECT
    p.id_producto,
    p.nombre,
    SUM(m.cantidad) as total_vendido
FROM productos p
INNER JOIN movimientos_stock m ON p.id_producto = m.id_producto
WHERE m.tipo_movimiento = 'SALIDA'
GROUP BY p.id_producto, p.nombre
ORDER BY total_vendido DESC
LIMIT 10;

-- Ver avisos adicionales de EXPLAIN EXTENDED
-- SHOW WARNINGS;

-- ============================================
-- FORMATO JSON DE EXPLAIN (más detallado)
-- ============================================

EXPLAIN FORMAT=JSON
SELECT
    p.categoria,
    COUNT(*) as total_productos,
    SUM(precio * stock) as valor_total_stock
FROM productos p
GROUP BY p.categoria
ORDER BY valor_total_stock DESC;

-- ============================================
-- DESACTIVAR PROFILING
-- ============================================
SET profiling = 0;

-- ============================================
-- NOTAS DE USO
-- ============================================
-- 1. Ejecuta este script sección por sección
-- 2. Observa los resultados de EXPLAIN:
--    - type: ALL (malo), index (regular), ref/range (bueno), const (excelente)
--    - rows: número de filas escaneadas (menor = mejor)
--    - Extra: información adicional (busca "Using index", evita "Using filesort")
-- 3. Compara los tiempos con SHOW PROFILES
-- 4. Guarda las capturas de pantalla para tu documentación
-- ============================================
