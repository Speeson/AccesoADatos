-- ============================================
-- OPTIMIZACIONES ADICIONALES
-- Fase II - Actividad Evaluable 2
-- ============================================
-- Script complementario a 02-optimizacion-indices.sql
-- Incluye 7 índices adicionales identificados en el análisis
-- ============================================

USE inventario_db;

-- ============================================
-- PRODUCTOS - ÍNDICES ADICIONALES
-- ============================================

-- Optimización #1: Búsqueda FULLTEXT por nombre
-- Permite búsquedas de texto mucho más rápidas que LIKE '%texto%'
-- Mejora esperada: 10-15x más rápido
CREATE FULLTEXT INDEX IF NOT EXISTS idx_productos_nombre_fulltext
ON productos(nombre);

-- Optimización #2: Filtro por rango de precios
-- Mejora consultas con WHERE precio BETWEEN ? AND ?
-- Mejora esperada: 6-8x más rápido
CREATE INDEX IF NOT EXISTS idx_productos_precio
ON productos(precio);

-- Optimización #4: Covering index para estadísticas por categoría
-- Permite que la consulta se resuelva completamente desde el índice
-- Mejora esperada: 3-5x más rápido
CREATE INDEX IF NOT EXISTS idx_productos_cat_precio_stock
ON productos(categoria, precio, stock);

-- Optimización #5: Índice para valor total de inventario
-- Mejora consultas que calculan SUM(precio * stock)
-- Mejora esperada: 2-3x más rápido
CREATE INDEX IF NOT EXISTS idx_productos_precio_stock
ON productos(precio, stock);

-- Optimización #6: Búsqueda por categoría con ordenamiento
-- Evita filesort cuando se busca por categoría y ordena por nombre
-- Mejora esperada: 2-4x más rápido
CREATE INDEX IF NOT EXISTS idx_productos_categoria_nombre
ON productos(categoria, nombre);

-- Optimización adicional: Consulta común de productos con stock bajo por categoría
-- Mejora consultas tipo: WHERE categoria = ? AND stock < ?
CREATE INDEX IF NOT EXISTS idx_productos_categoria_stock
ON productos(categoria, stock);

-- ============================================
-- CATEGORÍAS - ÍNDICES ADICIONALES
-- ============================================

-- Optimización #8: Ordenamiento por nombre de categoría
-- Mejora consultas con ORDER BY nombre
-- Mejora esperada: 2-3x más rápido
-- Nota: Este índice podría ya existir por UNIQUE constraint
CREATE INDEX IF NOT EXISTS idx_categorias_nombre
ON categorias(nombre);

-- ============================================
-- VERIFICACIÓN DE ÍNDICES CREADOS
-- ============================================

-- Mostrar todos los índices de la base de datos
SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    INDEX_TYPE,
    CASE WHEN NON_UNIQUE = 0 THEN 'UNIQUE' ELSE 'NON-UNIQUE' END as UNIQUENESS,
    CASE
        WHEN INDEX_NAME = 'PRIMARY' THEN 'PRIMARY KEY'
        WHEN NON_UNIQUE = 0 THEN 'UNIQUE INDEX'
        WHEN INDEX_TYPE = 'FULLTEXT' THEN 'FULLTEXT INDEX'
        ELSE 'INDEX'
    END as TYPE_DESCRIPTION
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ============================================
-- ANÁLISIS DE TAMAÑO DE ÍNDICES
-- ============================================

SELECT
    TABLE_NAME,
    INDEX_NAME,
    ROUND(STAT_VALUE * @@innodb_page_size / 1024 / 1024, 2) as SIZE_MB
FROM mysql.innodb_index_stats
WHERE DATABASE_NAME = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
    AND STAT_NAME = 'size'
ORDER BY SIZE_MB DESC;

-- ============================================
-- ESTADÍSTICAS DE TABLAS
-- ============================================

SELECT
    TABLE_NAME,
    TABLE_ROWS as ESTIMATED_ROWS,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) as DATA_SIZE_MB,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) as INDEX_SIZE_MB,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) as TOTAL_SIZE_MB,
    ROUND(INDEX_LENGTH / (DATA_LENGTH + INDEX_LENGTH) * 100, 2) as INDEX_RATIO_PERCENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
ORDER BY TOTAL_SIZE_MB DESC;

-- ============================================
-- TESTS DE CONSULTAS OPTIMIZADAS
-- ============================================

-- Activar profiling para medir tiempos
SET profiling = 1;

-- Test 1: Búsqueda por nombre con FULLTEXT
-- Compara con la versión LIKE del archivo 03-ejemplos-explain.sql
SELECT COUNT(*) as resultados
FROM productos
WHERE MATCH(nombre) AGAINST('*laptop*' IN BOOLEAN MODE);

-- Test 2: Filtro por rango de precios
SELECT COUNT(*) as resultados
FROM productos
WHERE precio BETWEEN 100 AND 500;

-- Test 3: Estadísticas por categoría (usando covering index)
SELECT
    categoria,
    COUNT(*) as total_productos,
    SUM(stock) as stock_total,
    AVG(precio) as precio_promedio,
    SUM(precio * stock) as valor_total
FROM productos
GROUP BY categoria;

-- Test 4: Búsqueda por categoría ordenada por nombre
SELECT COUNT(*) as resultados
FROM productos
WHERE categoria = 'Electronica'
ORDER BY nombre;

-- Test 5: Productos con stock bajo en categoría específica
SELECT COUNT(*) as resultados
FROM productos
WHERE categoria = 'Electronica'
    AND stock < 50
ORDER BY stock ASC;

-- Ver tiempos de ejecución
SHOW PROFILES;

-- Desactivar profiling
SET profiling = 0;

-- ============================================
-- EXPLAIN DE CONSULTAS OPTIMIZADAS
-- ============================================

-- EXPLAIN Test 1: Búsqueda FULLTEXT
EXPLAIN
SELECT id_producto, nombre, categoria, precio, stock
FROM productos
WHERE MATCH(nombre) AGAINST('laptop' IN BOOLEAN MODE);

-- EXPLAIN Test 2: Rango de precios
EXPLAIN
SELECT id_producto, nombre, precio
FROM productos
WHERE precio BETWEEN 100 AND 500
ORDER BY precio;

-- EXPLAIN Test 3: Estadísticas por categoría
EXPLAIN
SELECT
    categoria,
    COUNT(*) as total_productos,
    SUM(precio * stock) as valor_total
FROM productos
GROUP BY categoria;

-- EXPLAIN Test 4: Búsqueda por categoría ordenada
EXPLAIN
SELECT id_producto, nombre
FROM productos
WHERE categoria = 'Electronica'
ORDER BY nombre;

-- EXPLAIN Test 5: Stock bajo por categoría
EXPLAIN
SELECT id_producto, nombre, stock
FROM productos
WHERE categoria = 'Electronica'
    AND stock < 50
ORDER BY stock;

-- ============================================
-- MANTENIMIENTO DE ÍNDICES
-- ============================================

-- Analizar tablas para actualizar estadísticas de índices
ANALYZE TABLE productos;
ANALYZE TABLE categorias;
ANALYZE TABLE movimientos_stock;

-- Optimizar tablas si es necesario (desfragmentar)
-- OPTIMIZE TABLE productos;
-- OPTIMIZE TABLE categorias;
-- OPTIMIZE TABLE movimientos_stock;

-- ============================================
-- INFORMACIÓN ADICIONAL
-- ============================================

-- Ver fragmentación de índices
SELECT
    TABLE_NAME,
    ENGINE,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) as DATA_MB,
    ROUND(DATA_FREE / 1024 / 1024, 2) as FRAGMENTED_MB,
    ROUND(DATA_FREE / (DATA_LENGTH + DATA_FREE) * 100, 2) as FRAGMENTATION_PERCENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'inventario_db'
    AND DATA_FREE > 0
ORDER BY FRAGMENTATION_PERCENT DESC;

-- Ver uso de índices (requiere que se hayan ejecutado consultas)
-- Nota: Esta tabla solo existe si tienes performance_schema habilitado
-- SELECT
--     OBJECT_SCHEMA,
--     OBJECT_NAME,
--     INDEX_NAME,
--     COUNT_FETCH,
--     COUNT_INSERT,
--     COUNT_UPDATE,
--     COUNT_DELETE
-- FROM performance_schema.table_io_waits_summary_by_index_usage
-- WHERE OBJECT_SCHEMA = 'inventario_db'
-- ORDER BY COUNT_FETCH DESC;

-- ============================================
-- RESUMEN DE ÍNDICES CREADOS EN ESTE SCRIPT
-- ============================================

/*
ÍNDICES ADICIONALES CREADOS:

PRODUCTOS:
1. idx_productos_nombre_fulltext (nombre) - FULLTEXT
2. idx_productos_precio (precio)
3. idx_productos_cat_precio_stock (categoria, precio, stock) - COVERING INDEX
4. idx_productos_precio_stock (precio, stock)
5. idx_productos_categoria_nombre (categoria, nombre)
6. idx_productos_categoria_stock (categoria, stock)

CATEGORÍAS:
7. idx_categorias_nombre (nombre)

TOTAL: 7 índices adicionales

MEJORAS ESPERADAS:
- Búsquedas de texto: 10-15x más rápido
- Filtros de precio: 6-8x más rápido
- Estadísticas: 3-5x más rápido
- Consultas combinadas: 2-4x más rápido

NOTA: Estos índices complementan los 6 índices ya existentes
del script 01-init.sql y los 7 del script 02-optimizacion-indices.sql
Total de índices en el proyecto: 20 índices
*/
