# Optimizaciones Adicionales de Consultas
## Análisis Detallado de ProductoDAOImpl y CategoriaDAOImpl

---

## 📋 Resumen Ejecutivo

He analizado en profundidad todos los DAOs y el Service del proyecto y he identificado **12 oportunidades de optimización adicionales** que pueden mejorar significativamente el rendimiento de tu aplicación.

---

## 🔍 Análisis por Archivo

### 1. ProductoDAOImpl.java - Optimizaciones Identificadas

#### 🟡 Optimización #1: Búsqueda por Nombre con LIKE
**Línea:** [ProductoDAOImpl.java:86-87](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\ProductoDAOImpl.java#L86-L87)

**Consulta Actual:**
```sql
SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion
FROM productos
WHERE nombre LIKE ?
ORDER BY nombre
```

**Problema:**
- El patrón `%texto%` no puede usar índices normales eficientemente
- Escaneo completo de tabla cuando el LIKE empieza con `%`

**Soluciones Propuestas:**

##### Opción A: Índice FULLTEXT (MEJOR para búsquedas de texto)
```sql
-- Crear índice FULLTEXT
CREATE FULLTEXT INDEX idx_productos_nombre_fulltext ON productos(nombre);

-- Modificar consulta Java para usar MATCH AGAINST
String sql = """
    SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion
    FROM productos
    WHERE MATCH(nombre) AGAINST(? IN BOOLEAN MODE)
    ORDER BY nombre
    """;
```

**Ventajas:**
- ✅ Búsquedas de texto extremadamente rápidas
- ✅ Soporta búsquedas complejas (OR, AND, frases exactas)
- ✅ Búsquedas parciales sin penalización

**Mejora esperada:** 10-15x más rápido en tablas grandes

##### Opción B: Índice Normal (Si solo buscas por prefijo)
```sql
CREATE INDEX idx_productos_nombre ON productos(nombre);
```

**Uso:**
```java
// Solo funciona eficientemente para búsquedas tipo "texto%"
stmt.setString(1, nombre + "%");  // NO usar "%" al inicio
```

**Mejora esperada:** 5-8x más rápido (solo para búsquedas prefijo)

---

#### 🟢 Optimización #2: Filtro por Rango de Precios
**Línea:** [ProductoDAOImpl.java:190-191](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\ProductoDAOImpl.java#L190-L191)

**Consulta Actual:**
```sql
SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion
FROM productos
WHERE precio BETWEEN ? AND ?
ORDER BY precio
```

**Índice Recomendado:**
```sql
CREATE INDEX idx_productos_precio ON productos(precio);
```

**Por qué es importante:**
- ✅ BETWEEN es una operación de rango que se beneficia mucho de índices
- ✅ ORDER BY precio también usa el mismo índice
- ✅ El optimizer puede hacer index range scan

**Mejora esperada:** 6-8x más rápido

**EXPLAIN esperado:**
```
type: range
key: idx_productos_precio
Extra: Using index condition
```

---

#### 🟢 Optimización #3: Productos con Stock Bajo
**Línea:** [ProductoDAOImpl.java:164-165](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\ProductoDAOImpl.java#L164-L165)

**Consulta Actual:**
```sql
SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion
FROM productos
WHERE stock < ?
ORDER BY stock ASC
```

**Estado:** ✅ **YA OPTIMIZADA**
- Ya existe: `idx_productos_stock_bajo` en [01-init.sql:56](c:\dam2\accesodatos\git\inventario_git\scripts\01-init.sql#L56)

**Posible Mejora Adicional:**
Índice compuesto si frecuentemente filtras por categoría Y stock bajo:

```sql
CREATE INDEX idx_productos_categoria_stock ON productos(categoria, stock);
```

**Uso:**
```sql
-- Si frecuentemente haces:
SELECT ... FROM productos WHERE categoria = ? AND stock < ? ORDER BY stock
```

---

#### 🟡 Optimización #4: Estadísticas por Categoría
**Línea:** [ProductoDAOImpl.java:429-431](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\ProductoDAOImpl.java#L429-L431)

**Consulta Actual:**
```sql
SELECT categoria, COUNT(*) as total_productos, SUM(stock) as stock_total,
       AVG(precio) as precio_promedio, SUM(precio * stock) as valor_total
FROM productos
GROUP BY categoria
ORDER BY valor_total DESC
```

**Estado:** ⚠️ **PARCIALMENTE OPTIMIZADA**
- Ya existe: `idx_productos_categoria`

**Mejora Propuesta:**
Índice compuesto (covering index) para evitar acceso a la tabla:

```sql
CREATE INDEX idx_productos_cat_precio_stock ON productos(categoria, precio, stock);
```

**Ventajas:**
- ✅ "Covering index": todas las columnas necesarias están en el índice
- ✅ No necesita leer la tabla, solo el índice
- ✅ Mucho más rápido en tablas grandes

**Mejora esperada:** 3-5x más rápido

**EXPLAIN esperado:**
```
Extra: Using index  -- ¡Esto es excelente!
```

---

#### 🔴 Optimización #5: Valor Total del Inventario
**Línea:** [ProductoDAOImpl.java:409](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\ProductoDAOImpl.java#L409)

**Consulta Actual:**
```sql
SELECT SUM(precio * stock) as valor_total FROM productos
```

**Problema:**
- Sin índice, hace escaneo completo de tabla
- No hay WHERE, pero las columnas `precio` y `stock` no están indexadas juntas

**Índice Recomendado:**
```sql
CREATE INDEX idx_productos_precio_stock ON productos(precio, stock);
```

**Ventaja:**
- Puede usar "index-only scan" en algunos casos
- Reduce I/O de disco

**Mejora esperada:** 2-3x más rápido (menor que otras porque siempre necesita todas las filas)

---

#### 🟢 Optimización #6: Búsqueda por Categoría
**Línea:** [ProductoDAOImpl.java:112-113](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\ProductoDAOImpl.java#L112-L113)

**Consulta Actual:**
```sql
SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion
FROM productos
WHERE categoria = ?
ORDER BY nombre
```

**Estado:** ✅ **YA OPTIMIZADA**
- Ya existe: `idx_productos_categoria`

**Posible Mejora Adicional:**
Si también ordenas por nombre frecuentemente:

```sql
CREATE INDEX idx_productos_categoria_nombre ON productos(categoria, nombre);
```

**Ventaja:**
- ✅ El ORDER BY nombre también se resuelve desde el índice
- ✅ Evita "Using filesort"

**Mejora esperada:** 2-4x más rápido

---

### 2. CategoriaDAOImpl.java - Optimizaciones Identificadas

#### 🟢 Optimización #7: Búsqueda por Nombre
**Línea:** [CategoriaDAOImpl.java:84-85](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\CategoriaDAOImpl.java#L84-L85)

**Consulta Actual:**
```sql
SELECT id_categoria, nombre, descripcion, fecha_creacion, fecha_modificacion
FROM categorias
WHERE nombre = ?
```

**Problema:**
- El campo `nombre` tiene constraint UNIQUE pero no tiene índice explícito
- MySQL crea índice automático por UNIQUE, pero conviene verificar

**Verificar:**
```sql
SHOW INDEX FROM categorias WHERE Column_name = 'nombre';
```

**Si no existe, crear:**
```sql
CREATE INDEX idx_categorias_nombre ON categorias(nombre);
```

**Estado:** ✅ **PROBABLEMENTE YA OPTIMIZADA** (por UNIQUE constraint)

---

#### 🟢 Optimización #8: Obtener Todas las Categorías
**Línea:** [CategoriaDAOImpl.java:107-108](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\CategoriaDAOImpl.java#L107-L108)

**Consulta Actual:**
```sql
SELECT id_categoria, nombre, descripcion, fecha_creacion, fecha_modificacion
FROM categorias
ORDER BY nombre
```

**Índice Recomendado:**
```sql
CREATE INDEX idx_categorias_nombre ON categorias(nombre);
```

**Ventaja:**
- ✅ ORDER BY nombre se resuelve desde el índice
- ✅ Evita "Using filesort"

**Mejora esperada:** 2-3x más rápido en tablas con muchas categorías

---

#### 🟡 Optimización #9: Verificación de Existencia
**Línea:** [CategoriaDAOImpl.java:183](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\dao\impl\CategoriaDAOImpl.java#L183)

**Consulta Actual:**
```sql
SELECT COUNT(*) FROM categorias WHERE nombre = ?
```

**Optimización Sugerida:**
Usar `EXISTS` en lugar de `COUNT(*)`:

```sql
SELECT EXISTS(SELECT 1 FROM categorias WHERE nombre = ? LIMIT 1)
```

**Por qué:**
- ✅ `EXISTS` se detiene en el primer match (más rápido)
- ✅ `COUNT(*)` tiene que contar todas las coincidencias
- ✅ Con UNIQUE nombre, solo puede haber 0 o 1 resultado

**Código Java mejorado:**
```java
@Override
public boolean existePorNombre(String nombre) throws SQLException {
    String sql = "SELECT EXISTS(SELECT 1 FROM categorias WHERE nombre = ? LIMIT 1)";

    try (Connection conn = dbConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, nombre);

        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getBoolean(1);  // EXISTS devuelve boolean
            }
            return false;
        }

    } catch (SQLException e) {
        LogUtil.registrarError("EXISTE_CATEGORIA_NOMBRE",
            "Error al verificar existencia de categoría: " + nombre, e);
        throw e;
    }
}
```

**Mejora esperada:** 2x más rápido (especialmente si no existe)

---

### 3. InventarioServiceImpl.java - Optimizaciones de Lógica

#### 🔴 Optimización #10: Verificación de Categoría Repetida
**Líneas:** [InventarioServiceImpl.java:151-155](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\service\impl\InventarioServiceImpl.java#L151-L155) y [185-189](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\service\impl\InventarioServiceImpl.java#L185-L189)

**Código Actual:**
```java
// En crearProducto()
if (!categoriaDAO.existePorNombre(categoria)) {
    Categoria nuevaCategoria = new Categoria(categoria, "Categoría creada automáticamente");
    categoriaDAO.crear(nuevaCategoria);
}

// En actualizarProducto() - ¡MISMO CÓDIGO!
if (!categoriaDAO.existePorNombre(producto.getCategoria())) {
    Categoria nuevaCategoria = new Categoria(producto.getCategoria(), "Categoría creada automáticamente");
    categoriaDAO.crear(nuevaCategoria);
}
```

**Problema:**
- ❌ **N+1 queries problem**: Cada producto hace 1 query para verificar categoría
- ❌ Código duplicado

**Solución: Extraer a Método Privado + Caché**

```java
// Caché de categorías verificadas en esta sesión
private final Set<String> categoriasVerificadas = new HashSet<>();

private void asegurarCategoriaExiste(String nombreCategoria) throws SQLException {
    // Si ya verificamos esta categoría en esta sesión, no hacer nada
    if (categoriasVerificadas.contains(nombreCategoria)) {
        return;
    }

    // Verificar en BD
    if (!categoriaDAO.existePorNombre(nombreCategoria)) {
        Categoria nuevaCategoria = new Categoria(nombreCategoria, "Categoría creada automáticamente");
        categoriaDAO.crear(nuevaCategoria);
        logger.info("Categoría creada automáticamente: {}", nombreCategoria);
    }

    // Marcar como verificada
    categoriasVerificadas.add(nombreCategoria);
}

// Uso:
@Override
public int crearProducto(String nombre, String categoria, double precio, int stock) throws Exception {
    try {
        asegurarCategoriaExiste(categoria);  // Método reutilizable
        // ... resto del código
    } catch (Exception e) {
        // ...
    }
}
```

**Mejora esperada:** 50-80% reducción de queries en operaciones masivas

---

#### 🔴 Optimización #11: Verificación por Categoría en Loop
**Líneas:** [InventarioServiceImpl.java:257-270](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\service\impl\InventarioServiceImpl.java#L257-L270)

**Código Actual:**
```java
// CREAR CATEGORÍAS UNA POR UNA - EVITAR DUPLICADOS
int categoriasCreadas = 0;
for (Categoria categoria : categorias) {
    try {
        if (!categoriaDAO.existePorNombre(categoria.getNombre())) {  // ❌ Query por cada iteración
            categoriaDAO.crear(categoria);
            categoriasCreadas++;
        }
    } catch (Exception e) {
        logger.warn("Error al crear categoría {}: {}", categoria.getNombre(), e.getMessage());
    }
}
```

**Problema:**
- ❌ **N queries** de verificación (1 por cada categoría del CSV)
- ❌ Muy lento con CSVs grandes

**Solución Optimizada:**

```java
// OPTIMIZACIÓN: Obtener todas las categorías existentes UNA SOLA VEZ
List<Categoria> categoriasExistentes = categoriaDAO.obtenerTodas();
Set<String> nombresExistentes = categoriasExistentes.stream()
    .map(Categoria::getNombre)
    .collect(Collectors.toSet());

// Filtrar solo las nuevas
List<Categoria> categoriasNuevas = categorias.stream()
    .filter(cat -> !nombresExistentes.contains(cat.getNombre()))
    .collect(Collectors.toList());

// Crear en lote si hay nuevas
int categoriasCreadas = 0;
if (!categoriasNuevas.isEmpty()) {
    categoriasCreadas = categoriaDAO.crearMultiples(categoriasNuevas);
}

LogUtil.registrarOperacionExitosa("CARGAR_CATEGORIAS_CSV",
    String.format("Creadas %d categorías nuevas de %d leídas desde %s",
        categoriasCreadas, categorias.size(), rutaArchivo));
```

**Ventajas:**
- ✅ Solo 1 query de lectura + 1 insert batch (en vez de N queries)
- ✅ Usa el método `crearMultiples()` que ya existe
- ✅ Mucho más rápido con CSVs grandes

**Mejora esperada:** 10-20x más rápido con 100+ categorías

---

#### 🟢 Optimización #12: Carga de Productos desde CSV
**Líneas:** [InventarioServiceImpl.java:52-76](c:\dam2\accesodatos\git\inventario_git\src\main\java\com\inventario\service\impl\InventarioServiceImpl.java#L52-L76)

**Código Actual:**
```java
// OPTIMIZACIÓN: Obtener todas las categorías existentes UNA SOLA VEZ
List<Categoria> categoriasExistentes = categoriaDAO.obtenerTodas();
java.util.Set<String> nombresCategoriasExistentes = new java.util.HashSet<>();
for (Categoria cat : categoriasExistentes) {
    nombresCategoriasExistentes.add(cat.getNombre());
}

// Obtener categorías únicas de los productos
java.util.Set<String> categoriasNecesarias = new java.util.HashSet<>();
for (Producto p : productos) {
    categoriasNecesarias.add(p.getCategoria());
}

// Crear solo las categorías que no existen
for (String nombreCategoria : categoriasNecesarias) {
    if (!nombresCategoriasExistentes.contains(nombreCategoria)) {
        try {
            Categoria nuevaCategoria = new Categoria(nombreCategoria, "Categoría creada automáticamente");
            categoriaDAO.crear(nuevaCategoria);  // ❌ Una por una
            logger.info("Categoría creada: {}", nombreCategoria);
        } catch (Exception e) {
            logger.warn("Error creando categoría {}: {}", nombreCategoria, e.getMessage());
        }
    }
}
```

**Estado:** ⚠️ **BIEN, pero se puede mejorar**

**Mejora Propuesta:**
Usar Streams + crear categorías en batch:

```java
// OPTIMIZACIÓN: Obtener categorías existentes UNA SOLA VEZ
Set<String> nombresCategoriasExistentes = categoriaDAO.obtenerTodas().stream()
    .map(Categoria::getNombre)
    .collect(Collectors.toSet());

// Obtener categorías únicas de los productos
Set<String> categoriasNecesarias = productos.stream()
    .map(Producto::getCategoria)
    .collect(Collectors.toSet());

// Preparar categorías nuevas para crear en batch
List<Categoria> categoriasNuevas = categoriasNecesarias.stream()
    .filter(nombre -> !nombresCategoriasExistentes.contains(nombre))
    .map(nombre -> new Categoria(nombre, "Categoría creada automáticamente"))
    .collect(Collectors.toList());

// Crear categorías en batch
if (!categoriasNuevas.isEmpty()) {
    try {
        int creadas = categoriaDAO.crearMultiples(categoriasNuevas);
        logger.info("Creadas {} categorías nuevas en batch", creadas);
    } catch (SQLException e) {
        // Si falla el batch, intentar una por una (fallback)
        logger.warn("Falló creación en batch, intentando una por una");
        for (Categoria cat : categoriasNuevas) {
            try {
                categoriaDAO.crear(cat);
            } catch (SQLException ex) {
                logger.warn("Error creando categoría {}: {}", cat.getNombre(), ex.getMessage());
            }
        }
    }
}

// Crear productos en lote
int productosCreados = productoDAO.crearMultiples(productos);
```

**Ventajas:**
- ✅ Código más limpio y funcional
- ✅ Batch insert de categorías (más rápido)
- ✅ Fallback a inserción individual si falla el batch

**Mejora esperada:** 3-5x más rápido con muchas categorías nuevas

---

## 📊 Resumen de Índices a Agregar

### Script SQL Completo de Optimizaciones Adicionales

```sql
-- ============================================
-- OPTIMIZACIONES ADICIONALES
-- Script complementario a 02-optimizacion-indices.sql
-- ============================================

USE inventario_db;

-- ============================================
-- PRODUCTOS
-- ============================================

-- Optimización #1: Búsqueda FULLTEXT por nombre
CREATE FULLTEXT INDEX idx_productos_nombre_fulltext
ON productos(nombre);

-- Optimización #2: Filtro por rango de precios
CREATE INDEX idx_productos_precio
ON productos(precio);

-- Optimización #4: Covering index para estadísticas
CREATE INDEX idx_productos_cat_precio_stock
ON productos(categoria, precio, stock);

-- Optimización #5: Valor total inventario
CREATE INDEX idx_productos_precio_stock
ON productos(precio, stock);

-- Optimización #6: Búsqueda por categoría con ordenamiento
CREATE INDEX idx_productos_categoria_nombre
ON productos(categoria, nombre);

-- Optimización adicional: Categoría + stock bajo (consulta común)
CREATE INDEX idx_productos_categoria_stock
ON productos(categoria, stock);

-- ============================================
-- CATEGORÍAS
-- ============================================

-- Optimización #8: Ordenamiento por nombre
CREATE INDEX idx_categorias_nombre
ON categorias(nombre);

-- ============================================
-- VERIFICAR ÍNDICES CREADOS
-- ============================================

SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;
```

---

## 🎯 Priorización de Optimizaciones

### 🔴 PRIORIDAD ALTA (Implementar primero)
1. **Optimización #1**: FULLTEXT index en productos.nombre (mayor impacto en búsquedas)
2. **Optimización #10**: Refactorizar verificación de categorías (reduce queries significativamente)
3. **Optimización #11**: Optimizar carga de categorías desde CSV (10-20x mejora)
4. **Optimización #4**: Covering index para estadísticas (consulta frecuente)

### 🟡 PRIORIDAD MEDIA (Implementar después)
5. **Optimización #2**: Índice precio (si usas filtros de precio frecuentemente)
6. **Optimización #6**: Índice compuesto categoría+nombre (mejora ORDER BY)
7. **Optimización #9**: Usar EXISTS en lugar de COUNT (pequeña mejora)
8. **Optimización #12**: Mejorar carga productos CSV con streams

### 🟢 PRIORIDAD BAJA (Opcionales)
9. **Optimización #5**: Índice precio+stock (consulta poco frecuente)
10. **Optimización #8**: Índice nombre categorías (tabla pequeña)

---

## 📈 Mejoras Esperadas Global

| Área | Consultas Optimizadas | Mejora Esperada |
|------|----------------------|-----------------|
| **Búsquedas de productos** | 4 consultas | **8-15x más rápido** |
| **Carga CSV masiva** | 2 flujos | **10-20x más rápido** |
| **Estadísticas y agregaciones** | 3 consultas | **3-8x más rápido** |
| **Verificaciones EXISTS** | 2 consultas | **2x más rápido** |
| **TOTAL** | **12 optimizaciones** | **Promedio 6-10x** |

---

## 🛠️ Plan de Implementación

### Fase 1: Índices (30 minutos)
```bash
# Ejecutar script de índices adicionales
mysql -u root -p inventario_db < scripts/04-optimizaciones-adicionales.sql
```

### Fase 2: Refactorización de Código (2-3 horas)
1. Implementar método `asegurarCategoriaExiste()` en InventarioServiceImpl
2. Refactorizar `cargarCategoriasDesdeCSV()` con batch insert
3. Mejorar `cargarProductosDesdeCSV()` con streams
4. Cambiar `COUNT(*)` por `EXISTS` en CategoriaDAOImpl

### Fase 3: Testing y Medición (1 hora)
1. Ejecutar EXPLAIN en todas las consultas optimizadas
2. Medir tiempos con SHOW PROFILES
3. Comparar tiempos antes/después
4. Documentar mejoras en README.md

### Fase 4: Documentación (30 minutos)
1. Capturar pantallas de EXPLAIN
2. Documentar tiempos de ejecución
3. Actualizar README.md con evidencias

---

## 📝 Código Java Mejorado

### Archivo: CategoriaDAOImpl.java

#### Mejora del método existePorNombre()

```java
@Override
public boolean existePorNombre(String nombre) throws SQLException {
    // OPTIMIZADO: Usar EXISTS en lugar de COUNT(*)
    String sql = "SELECT EXISTS(SELECT 1 FROM categorias WHERE nombre = ? LIMIT 1)";

    try (Connection conn = dbConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, nombre);

        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getBoolean(1);
            }
            return false;
        }

    } catch (SQLException e) {
        LogUtil.registrarError("EXISTE_CATEGORIA_NOMBRE",
            "Error al verificar existencia de categoría: " + nombre, e);
        throw e;
    }
}
```

### Archivo: ProductoDAOImpl.java

#### Método nuevo: búsqueda FULLTEXT

```java
/**
 * Búsqueda de productos usando FULLTEXT (más rápido que LIKE)
 * Requiere índice: CREATE FULLTEXT INDEX idx_productos_nombre_fulltext ON productos(nombre)
 */
public List<Producto> buscarPorNombreFulltext(String nombre) throws SQLException {
    String sql = """
        SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion
        FROM productos
        WHERE MATCH(nombre) AGAINST(? IN BOOLEAN MODE)
        ORDER BY nombre
        """;

    List<Producto> productos = new ArrayList<>();

    try (Connection conn = dbConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        // Para búsquedas parciales, agregar * al final
        stmt.setString(1, nombre + "*");

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }
        }

        logger.debug("Encontrados {} productos con FULLTEXT: {}", productos.size(), nombre);
        return productos;

    } catch (SQLException e) {
        LogUtil.registrarError("BUSCAR_PRODUCTO_FULLTEXT",
            "Error en búsqueda FULLTEXT: " + nombre, e);
        throw e;
    }
}
```

### Archivo: InventarioServiceImpl.java

#### Método privado para caché de categorías

```java
// Caché de categorías verificadas en esta sesión
private final Set<String> categoriasVerificadas = new HashSet<>();

/**
 * Asegura que una categoría existe en la BD
 * Usa caché para evitar queries repetidas en la misma sesión
 */
private void asegurarCategoriaExiste(String nombreCategoria) throws SQLException {
    // Si ya verificamos esta categoría, no hacer nada
    if (categoriasVerificadas.contains(nombreCategoria)) {
        return;
    }

    // Verificar en BD
    if (!categoriaDAO.existePorNombre(nombreCategoria)) {
        Categoria nuevaCategoria = new Categoria(nombreCategoria,
            "Categoría creada automáticamente");
        categoriaDAO.crear(nuevaCategoria);
        logger.info("Categoría creada automáticamente: {}", nombreCategoria);
    }

    // Marcar como verificada
    categoriasVerificadas.add(nombreCategoria);
}

// Método para limpiar caché si es necesario
public void limpiarCacheCateg orias() {
    categoriasVerificadas.clear();
    logger.debug("Caché de categorías limpiada");
}
```

#### Método mejorado: cargarCategoriasDesdeCSV()

```java
@Override
public int cargarCategoriasDesdeCSV(String rutaArchivo) throws Exception {
    logger.info("Iniciando carga de categorías desde CSV: {}", rutaArchivo);

    try {
        if (!CsvUtil.validarEstructuraCategorias(rutaArchivo)) {
            throw new Exception("Estructura del archivo CSV inválida");
        }

        List<Categoria> categorias = CsvUtil.leerCategoriasCSV(rutaArchivo);

        if (categorias.isEmpty()) {
            LogUtil.registrarAdvertencia("CARGAR_CATEGORIAS_CSV",
                "No se encontraron categorías válidas en el archivo");
            return 0;
        }

        // OPTIMIZACIÓN: Obtener categorías existentes UNA SOLA VEZ
        Set<String> nombresExistentes = categoriaDAO.obtenerTodas().stream()
            .map(Categoria::getNombre)
            .collect(Collectors.toSet());

        // Filtrar solo las nuevas
        List<Categoria> categoriasNuevas = categorias.stream()
            .filter(cat -> !nombresExistentes.contains(cat.getNombre()))
            .collect(Collectors.toList());

        if (categoriasNuevas.isEmpty()) {
            logger.info("Todas las categorías ya existen");
            return 0;
        }

        // Crear en lote
        int categoriasCreadas = categoriaDAO.crearMultiples(categoriasNuevas);

        LogUtil.registrarOperacionExitosa("CARGAR_CATEGORIAS_CSV",
            String.format("Creadas %d categorías nuevas de %d leídas desde %s",
                categoriasCreadas, categorias.size(), rutaArchivo));

        return categoriasCreadas;

    } catch (Exception e) {
        LogUtil.registrarError("CARGAR_CATEGORIAS_CSV",
            "Error al cargar categorías desde CSV", e);
        throw e;
    }
}
```

---

## 🧪 Scripts de Testing

### Script para probar mejoras

```sql
-- ============================================
-- SCRIPT DE TESTING DE OPTIMIZACIONES
-- ============================================

USE inventario_db;

SET profiling = 1;

-- Test 1: Búsqueda por nombre (LIKE vs FULLTEXT)
-- Versión antigua (LIKE)
SELECT * FROM productos WHERE nombre LIKE '%laptop%';

-- Versión nueva (FULLTEXT)
SELECT * FROM productos WHERE MATCH(nombre) AGAINST('laptop' IN BOOLEAN MODE);

-- Test 2: Filtro por precio
SELECT * FROM productos WHERE precio BETWEEN 100 AND 500 ORDER BY precio;

-- Test 3: Estadísticas por categoría
SELECT categoria, COUNT(*), SUM(precio * stock)
FROM productos
GROUP BY categoria;

-- Test 4: EXISTS vs COUNT
SELECT COUNT(*) FROM categorias WHERE nombre = 'Electronica';
SELECT EXISTS(SELECT 1 FROM categorias WHERE nombre = 'Electronica' LIMIT 1);

-- Ver resultados
SHOW PROFILES;

SET profiling = 0;
```

---

## ✅ Checklist de Implementación

### Índices
- [ ] Crear FULLTEXT index en productos.nombre
- [ ] Crear índice en productos.precio
- [ ] Crear índice compuesto productos(categoria, precio, stock)
- [ ] Crear índice compuesto productos(precio, stock)
- [ ] Crear índice compuesto productos(categoria, nombre)
- [ ] Crear índice en categorias.nombre
- [ ] Verificar todos los índices con SHOW INDEX

### Código Java
- [ ] Cambiar COUNT(*) por EXISTS en CategoriaDAOImpl.existePorNombre()
- [ ] Agregar método buscarPorNombreFulltext() en ProductoDAOImpl
- [ ] Implementar asegurarCategoriaExiste() en InventarioServiceImpl
- [ ] Refactorizar cargarCategoriasDesdeCSV() con batch insert
- [ ] Refactorizar cargarProductosDesdeCSV() con streams
- [ ] Usar asegurarCategoriaExiste() en crearProducto()
- [ ] Usar asegurarCategoriaExiste() en actualizarProducto()

### Testing
- [ ] Ejecutar EXPLAIN en todas las consultas modificadas
- [ ] Medir tiempos con SHOW PROFILES
- [ ] Comparar before/after
- [ ] Capturar pantallas de resultados

### Documentación
- [ ] Documentar tiempos en README.md
- [ ] Incluir capturas de EXPLAIN
- [ ] Documentar mejoras obtenidas
- [ ] Actualizar diagrama de arquitectura si es necesario

---

**Fecha de creación:** 2025-10-25
**Autor:** Optimización Fase II - Análisis Detallado
**Versión:** 2.0
