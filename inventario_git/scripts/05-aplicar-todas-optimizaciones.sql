-- ============================================
-- SCRIPT CONSOLIDADO DE TODAS LAS OPTIMIZACIONES
-- Fase II - Actividad Evaluable 2
-- ============================================
-- Este script aplica TODOS los índices de optimización
-- en un solo archivo para facilitar la implementación
-- ============================================

USE inventario_db;

-- ============================================
-- INFORMACIÓN PREVIA
-- ============================================

SELECT '========================================' as '';
SELECT 'APLICANDO OPTIMIZACIONES DE ÍNDICES' as '';
SELECT '========================================' as '';
SELECT CONCAT('Fecha: ', NOW()) as '';
SELECT CONCAT('Base de datos: ', DATABASE()) as '';
SELECT '========================================' as '';

-- ============================================
-- PRODUCTOS - TODOS LOS ÍNDICES
-- ============================================

SELECT 'Creando índices para tabla PRODUCTOS...' as 'Estado';

-- Índice para búsquedas por nombre (mejora LIKE)
CREATE INDEX IF NOT EXISTS idx_productos_nombre
ON productos(nombre);

-- Índice FULLTEXT para búsquedas de texto optimizadas
CREATE FULLTEXT INDEX IF NOT EXISTS idx_productos_nombre_fulltext
ON productos(nombre);

-- Índice para filtros de rango de precios
CREATE INDEX IF NOT EXISTS idx_productos_precio
ON productos(precio);

-- Índice compuesto para estadísticas (COVERING INDEX)
CREATE INDEX IF NOT EXISTS idx_productos_cat_precio_stock
ON productos(categoria, precio, stock);

-- Índice para valor total de inventario
CREATE INDEX IF NOT EXISTS idx_productos_precio_stock
ON productos(precio, stock);

-- Índice compuesto para búsqueda con ordenamiento
CREATE INDEX IF NOT EXISTS idx_productos_categoria_nombre
ON productos(categoria, nombre);

-- Índice para productos con stock bajo por categoría
CREATE INDEX IF NOT EXISTS idx_productos_categoria_stock
ON productos(categoria, stock);

SELECT 'Índices de PRODUCTOS creados correctamente ✓' as 'Estado';

-- ============================================
-- MOVIMIENTOS_STOCK - ÍNDICES AVANZADOS
-- ============================================

SELECT 'Creando índices para tabla MOVIMIENTOS_STOCK...' as 'Estado';

-- Índice compuesto para Top N productos vendidos
CREATE INDEX IF NOT EXISTS idx_movimientos_tipo_producto
ON movimientos_stock(tipo_movimiento, id_producto);

-- Índice compuesto para histórico con filtros
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha_tipo
ON movimientos_stock(fecha_movimiento, tipo_movimiento);

-- Índice para filtros por usuario
CREATE INDEX IF NOT EXISTS idx_movimientos_usuario
ON movimientos_stock(usuario);

SELECT 'Índices de MOVIMIENTOS_STOCK creados correctamente ✓' as 'Estado';

-- ============================================
-- CATEGORÍAS - ÍNDICES
-- ============================================

SELECT 'Creando índices para tabla CATEGORIAS...' as 'Estado';

-- Índice para búsquedas y ordenamiento por nombre
CREATE INDEX IF NOT EXISTS idx_categorias_nombre
ON categorias(nombre);

SELECT 'Índices de CATEGORIAS creados correctamente ✓' as 'Estado';

-- ============================================
-- ACTUALIZAR ESTADÍSTICAS DE TABLAS
-- ============================================

SELECT 'Actualizando estadísticas de tablas...' as 'Estado';

ANALYZE TABLE productos;
ANALYZE TABLE categorias;
ANALYZE TABLE movimientos_stock;

SELECT 'Estadísticas actualizadas ✓' as 'Estado';

-- ============================================
-- VERIFICACIÓN DE ÍNDICES CREADOS
-- ============================================

SELECT '========================================' as '';
SELECT 'VERIFICACIÓN DE ÍNDICES' as '';
SELECT '========================================' as '';

-- Contar índices por tabla
SELECT
    TABLE_NAME,
    COUNT(DISTINCT INDEX_NAME) as TOTAL_INDICES
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
GROUP BY TABLE_NAME;

SELECT '========================================' as '';
SELECT 'DETALLE DE ÍNDICES POR TABLA' as '';
SELECT '========================================' as '';

-- Detalle completo de índices
SELECT
    TABLE_NAME as Tabla,
    INDEX_NAME as Indice,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as Columnas,
    INDEX_TYPE as Tipo,
    CASE WHEN NON_UNIQUE = 0 THEN 'SI' ELSE 'NO' END as Unico
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ============================================
-- ESTADÍSTICAS DE TAMAÑO
-- ============================================

SELECT '========================================' as '';
SELECT 'TAMAÑO DE TABLAS E ÍNDICES' as '';
SELECT '========================================' as '';

SELECT
    TABLE_NAME as Tabla,
    TABLE_ROWS as Filas_Estimadas,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) as Datos_MB,
    ROUND(INDEX_LENGTH / 1024 / 1024, 2) as Indices_MB,
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) as Total_MB,
    ROUND(INDEX_LENGTH / (DATA_LENGTH + INDEX_LENGTH) * 100, 2) as Porcentaje_Indices
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
ORDER BY (DATA_LENGTH + INDEX_LENGTH) DESC;

-- ============================================
-- RESUMEN FINAL
-- ============================================

SELECT '========================================' as '';
SELECT 'RESUMEN DE OPTIMIZACIONES APLICADAS' as '';
SELECT '========================================' as '';

SELECT 'PRODUCTOS: 7 índices adicionales' as 'Resumen';
SELECT '  - idx_productos_nombre' as '';
SELECT '  - idx_productos_nombre_fulltext (FULLTEXT)' as '';
SELECT '  - idx_productos_precio' as '';
SELECT '  - idx_productos_cat_precio_stock (COVERING)' as '';
SELECT '  - idx_productos_precio_stock' as '';
SELECT '  - idx_productos_categoria_nombre' as '';
SELECT '  - idx_productos_categoria_stock' as '';
SELECT '' as '';

SELECT 'MOVIMIENTOS_STOCK: 3 índices adicionales' as 'Resumen';
SELECT '  - idx_movimientos_tipo_producto' as '';
SELECT '  - idx_movimientos_fecha_tipo' as '';
SELECT '  - idx_movimientos_usuario' as '';
SELECT '' as '';

SELECT 'CATEGORIAS: 1 índice adicional' as 'Resumen';
SELECT '  - idx_categorias_nombre' as '';
SELECT '' as '';

SELECT 'TOTAL: 11 índices nuevos creados' as 'Resumen';
SELECT 'Índices anteriores: 6 (de 01-init.sql)' as '';
SELECT 'TOTAL GENERAL: 17+ índices en la base de datos' as '';

SELECT '========================================' as '';
SELECT 'OPTIMIZACIONES APLICADAS CORRECTAMENTE ✓' as '';
SELECT '========================================' as '';

-- ============================================
-- NOTAS IMPORTANTES
-- ============================================

/*
PRÓXIMOS PASOS:

1. Ejecutar análisis EXPLAIN:
   mysql -u root -p inventario_db < scripts/03-ejemplos-explain.sql

2. Usar las consultas avanzadas:
   ConsultasAvanzadasDAO consultasDAO = new ConsultasAvanzadasDAOImpl();

3. Medir mejoras de rendimiento:
   - Ejecutar SHOW PROFILES para comparar tiempos
   - Documentar mejoras en README.md

4. Implementar optimizaciones de código Java:
   - Usar FULLTEXT en búsquedas
   - Cambiar COUNT(*) por EXISTS
   - Implementar caché de categorías

MEJORAS ESPERADAS:
- Búsquedas de texto: 10-15x más rápido
- Consultas con JOIN: 6-8x más rápido
- Agregaciones: 3-5x más rápido
- Consultas generales: 2-4x más rápido

¡Tus consultas ahora están optimizadas! ✓
*/
