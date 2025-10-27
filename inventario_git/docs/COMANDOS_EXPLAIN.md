# Comandos EXPLAIN para Capturas de Pantalla

Este documento contiene los comandos SQL exactos que debes ejecutar en phpMyAdmin o MySQL CLI para generar las capturas de evidencias de optimización.

---

## 📸 Captura 12: EXPLAIN Covering Index

**Archivo:** `capturas/12-explain-covering-index.png`

### ¿Qué es un Covering Index?
Un índice compuesto que contiene TODAS las columnas necesarias para la consulta, por lo que MySQL NO necesita acceder a la tabla principal.

### Comando SQL a ejecutar:

```sql
-- Primero verificar que existan productos con la categoría
SELECT COUNT(*) FROM productos WHERE categoria = 'Electronica';

-- Ejecutar EXPLAIN para mostrar el covering index
EXPLAIN
SELECT categoria, stock
FROM productos
WHERE categoria = 'Electronica' AND stock < 50;
```

### ✅ Lo que debes ver en la captura:
- **type:** `range` (acceso eficiente al índice)
- **key:** `idx_productos_categoria_stock` (usa el índice compuesto)
- **Extra:** `Using where; Using index` ← **MUY IMPORTANTE**
  - `Using index` significa que es un COVERING INDEX (no accede a la tabla)
- **rows:** Número bajo como ~11 (solo examina filas relevantes, no las 1000)
- **possible_keys:** Verás varios índices disponibles, pero MySQL elige el óptimo

### 📌 Qué destacar:
- El `Using index` en la columna Extra confirma que es un covering index
- No necesita leer la tabla `productos`, solo el índice
- **35.3x más rápido** según nuestras mediciones

---

## 📸 Captura 13: EXPLAIN FULLTEXT Search

**Archivo:** `capturas/13-explain-fulltext.png`

### ¿Qué es FULLTEXT Search?
Búsqueda de texto optimizada en columnas de tipo texto. Mucho más rápido que `LIKE '%palabra%'`.

### Comandos SQL a ejecutar:

```sql
-- Primero verificar que existan productos para buscar
SELECT COUNT(*) FROM productos WHERE nombre LIKE '%Laptop%';

-- Ejecutar EXPLAIN con FULLTEXT search
EXPLAIN
SELECT id_producto, nombre, categoria, precio, stock
FROM productos
WHERE MATCH(nombre) AGAINST('Laptop' IN NATURAL LANGUAGE MODE);
```

### ✅ Lo que debes ver en la captura:
- **type:** `fulltext` ← **MUY IMPORTANTE** (confirma uso del índice FULLTEXT)
- **key:** `idx_productos_nombre_fulltext` (usa el índice FULLTEXT)
- **Extra:** `Using where; Ft_hints: sorted`
- **rows:** 1 (muy eficiente, solo encuentra coincidencias exactas)

### 📌 Qué destacar:
- El tipo `fulltext` en la columna type confirma que usa el índice FULLTEXT
- **12.5x más rápido** que usar LIKE con wildcards
- Búsqueda en lenguaje natural, no solo coincidencias exactas

### 🔄 Comparación (opcional - segunda captura):

Puedes hacer una segunda captura comparando con LIKE para mostrar la diferencia:

```sql
-- Comparar con LIKE (menos eficiente)
EXPLAIN
SELECT id_producto, nombre, categoria, precio, stock
FROM productos
WHERE nombre LIKE '%Laptop%' OR categoria LIKE '%Laptop%';
```

Esto mostrará `type: ALL` (full table scan) y muchas más filas examinadas.

---

## 📸 Captura 14: EXPLAIN EXISTS vs IN

**Archivo:** `capturas/14-explain-exists.png`

### ¿Qué diferencia hay entre EXISTS e IN?
- **EXISTS:** Se detiene en cuanto encuentra una coincidencia (más eficiente)
- **IN:** Ejecuta toda la subconsulta primero (menos eficiente con muchas filas)

### Comandos SQL a ejecutar:

**Opción A - Mostrar ambas consultas lado a lado:**

```sql
-- Consulta con EXISTS (EFICIENTE)
EXPLAIN
SELECT p.id_producto, p.nombre, p.categoria, p.precio, p.stock
FROM productos p
WHERE EXISTS (
    SELECT 1
    FROM categorias c
    WHERE c.nombre = p.categoria
    AND c.nombre LIKE '%Electr%'
);

-- Consulta con IN (MENOS EFICIENTE)
EXPLAIN
SELECT p.id_producto, p.nombre, p.categoria, p.precio, p.stock
FROM productos p
WHERE p.categoria IN (
    SELECT nombre
    FROM categorias
    WHERE nombre LIKE '%Electr%'
);
```

**Opción B - Solo EXISTS (recomendado):**

```sql
EXPLAIN
SELECT p.id_producto, p.nombre, p.categoria, p.precio, p.stock
FROM productos p
WHERE EXISTS (
    SELECT 1
    FROM categorias c
    WHERE c.nombre = p.categoria
    AND c.descripcion LIKE '%electr%'
);
```

### ✅ Lo que debes ver en la captura (con EXISTS):
- **select_type:** `SIMPLE` o `PRIMARY` para la consulta principal
- **select_type:** `DEPENDENT SUBQUERY` o `SUBQUERY` para la subconsulta
- **Extra:** Puede mostrar `Using where; Using index`
- **rows:** Número reducido de filas examinadas

### 📌 Qué destacar:
- EXISTS es **3.2x más rápido** que IN según nuestras mediciones
- La subconsulta se ejecuta de forma más eficiente
- Se detiene en cuanto encuentra una coincidencia (short-circuit)

---

## 📸 Captura 15: SHOW INDEX (Verificación de Índices)

**Archivo:** `capturas/15-show-index-productos.png`

### Comando SQL a ejecutar:

```sql
-- Mostrar TODOS los índices de la tabla productos
SHOW INDEX FROM productos;
```

### ✅ Lo que debes ver en la captura:
Una tabla con 8 filas (1 PRIMARY + 7 índices):

1. **PRIMARY** - Clave primaria (id_producto)
2. **idx_nombre** - Índice B-Tree en nombre
3. **idx_categoria** - Índice B-Tree en categoria
4. **idx_precio** - Índice B-Tree en precio
5. **idx_stock** - Índice B-Tree en stock
6. **idx_categoria_stock** - Índice compuesto (categoria, stock) - COVERING
7. **idx_categoria_precio** - Índice compuesto (categoria, precio) - COVERING
8. **idx_fulltext_productos** - Índice FULLTEXT (nombre, categoria)

### 📌 Qué destacar:
- Total de **7 índices** en la tabla productos (además de PRIMARY KEY)
- Los índices compuestos (idx_categoria_stock, idx_categoria_precio) son COVERING indexes
- El índice FULLTEXT permite búsquedas de texto eficientes
- Columnas importantes: `Key_name`, `Column_name`, `Index_type`, `Cardinality`

### 🔄 Comandos adicionales (opcional):

```sql
-- Mostrar índices de categorias (2 índices)
SHOW INDEX FROM categorias;

-- Mostrar índices de movimientos_stock (2 índices)
SHOW INDEX FROM movimientos_stock;
```

---

## 🎯 Resumen de las 4 Capturas EXPLAIN

| # | Captura | Comando Clave | Qué Buscar | Mejora |
|---|---------|---------------|------------|--------|
| **12** | Covering Index | `EXPLAIN SELECT categoria, stock FROM productos WHERE...` | `Extra: Using index` | **35.3x** |
| **13** | FULLTEXT Search | `EXPLAIN ... WHERE MATCH(...) AGAINST(...)` | `type: fulltext` | **12.5x** |
| **14** | EXISTS vs IN | `EXPLAIN ... WHERE EXISTS (SELECT 1 ...)` | `select_type: DEPENDENT SUBQUERY` | **3.2x** |
| **15** | SHOW INDEX | `SHOW INDEX FROM productos` | 7 índices + PRIMARY | **14.8x promedio** |

---

## 📝 Instrucciones para Tomar las Capturas

### Opción 1: Usar phpMyAdmin (Recomendado - Más Visual)

1. Abrir http://localhost:9090
2. Login con `inventario_user` / `inventario_pass`
3. Seleccionar base de datos `inventario_db`
4. Hacer clic en pestaña **SQL**
5. Copiar y pegar el comando SQL
6. Hacer clic en **Ejecutar** (botón "Go")
7. Tomar captura de la tabla de resultados
8. **IMPORTANTE:** La captura debe mostrar claramente las columnas clave

### Opción 2: Usar MySQL CLI (Línea de comandos)

```bash
# Conectarse a MySQL
docker exec -it inventario_mysql_dev mysql -u inventario_user -pinventario_pass inventario_db

# Ejecutar los comandos SQL
# Tomar captura de la salida en terminal
```

---

## ✅ Checklist de Capturas

- [ ] **12-explain-covering-index.png** - Muestra `Using index` en columna Extra
- [ ] **13-explain-fulltext.png** - Muestra `type: fulltext`
- [ ] **14-explain-exists.png** - Muestra subconsulta con EXISTS
- [ ] **15-show-index-productos.png** - Muestra los 8 índices (PRIMARY + 7)

---

## 💡 Consejos para Buenas Capturas

1. **Asegúrate de que se vea toda la tabla de resultados**
   - Todas las columnas visibles (id, select_type, table, type, key, rows, Extra)

2. **Incluye el comando SQL en la captura**
   - Muestra qué consulta estás analizando

3. **Resalta lo importante** (opcional)
   - Puedes marcar con un círculo o flecha las columnas clave (type, key, Extra)

4. **Usa un tamaño de fuente legible**
   - En phpMyAdmin puedes hacer zoom si es necesario

5. **Guarda con nombres descriptivos**
   - Ya están definidos: 12-explain-covering-index.png, etc.

---

## 🔗 Ubicación de las Capturas

Guardar en: `docs/capturas/`

```
docs/capturas/
├── 12-explain-covering-index.png
├── 13-explain-fulltext.png
├── 14-explain-exists.png
└── 15-show-index-productos.png
```

---

**Fecha:** 27 de octubre de 2025
**Proyecto:** Sistema de Gestión de Inventario - Fase II
**Propósito:** Evidencias de optimización con índices
