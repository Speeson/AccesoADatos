# 🚀 PASO 1: Aplicar Índices de Optimización

## Guía Paso a Paso para Ejecutar las Optimizaciones

---

## Opción A: Desde Git Bash o Terminal (MÁS FÁCIL)

### 1. Abrir terminal en la carpeta del proyecto

```bash
# Navega a la carpeta del proyecto
cd /c/dam2/accesodatos/git/inventario_git
```

### 2. Ejecutar el script de optimización

```bash
mysql -u root -p inventario_db < scripts/05-aplicar-todas-optimizaciones.sql
```

### 3. Ingresar contraseña
- Te pedirá la contraseña de MySQL
- Escríbela y presiona Enter
- **Nota:** No verás los caracteres mientras escribes (es normal)

### 4. Ver resultados
- Verás mensajes como:
  ```
  Creando índices para tabla PRODUCTOS...
  Índices de PRODUCTOS creados correctamente ✓
  ...
  OPTIMIZACIONES APLICADAS CORRECTAMENTE ✓
  ```

---

## Opción B: Desde MySQL Workbench (MÁS VISUAL)

### 1. Abrir MySQL Workbench
- Inicia MySQL Workbench
- Conecta a tu servidor local

### 2. Abrir el script
- Menú: `File` → `Open SQL Script...`
- Navega a: `C:\dam2\accesodatos\git\inventario_git\scripts`
- Selecciona: `05-aplicar-todas-optimizaciones.sql`

### 3. Seleccionar la base de datos
- En el dropdown superior, selecciona: `inventario_db`
- O ejecuta primero: `USE inventario_db;`

### 4. Ejecutar el script
- Presiona el botón del rayo ⚡ o `Ctrl + Shift + Enter`
- Espera a que termine (toma 5-10 segundos)

### 5. Ver resultados
- En el panel "Output" verás:
  - Mensajes de creación de índices
  - Tabla de verificación
  - Estadísticas de tamaño
  - Resumen final

---

## Opción C: Desde línea de comandos MySQL (Interactiva)

### 1. Conectar a MySQL
```bash
mysql -u root -p
```

### 2. Dentro de MySQL
```sql
-- Seleccionar base de datos
USE inventario_db;

-- Ejecutar script (ajusta la ruta según tu sistema)
SOURCE C:/dam2/accesodatos/git/inventario_git/scripts/05-aplicar-todas-optimizaciones.sql;
```

### 3. Salir
```sql
EXIT;
```

---

## ✅ Verificación: ¿Se aplicaron correctamente?

Después de ejecutar el script, verifica con estos comandos:

### Verificación 1: Contar índices
```sql
SELECT
    TABLE_NAME,
    COUNT(DISTINCT INDEX_NAME) as TOTAL_INDICES
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'inventario_db'
    AND TABLE_NAME IN ('productos', 'categorias', 'movimientos_stock')
GROUP BY TABLE_NAME;
```

**Resultado esperado:**
```
+-------------------+---------------+
| TABLE_NAME        | TOTAL_INDICES |
+-------------------+---------------+
| productos         |          13   |
| categorias        |           3   |
| movimientos_stock |           6   |
+-------------------+---------------+
```

### Verificación 2: Ver índices de productos
```sql
SHOW INDEX FROM productos;
```

**Deberías ver estos índices:**
- PRIMARY
- categoria (FK)
- idx_productos_categoria
- idx_productos_stock_bajo
- idx_productos_nombre
- idx_productos_nombre_fulltext (FULLTEXT)
- idx_productos_precio
- idx_productos_cat_precio_stock
- idx_productos_precio_stock
- idx_productos_categoria_nombre
- idx_productos_categoria_stock

### Verificación 3: Ver índices de movimientos_stock
```sql
SHOW INDEX FROM movimientos_stock;
```

**Deberías ver:**
- PRIMARY
- id_producto (FK)
- idx_movimientos_producto
- idx_movimientos_fecha
- idx_movimientos_tipo_producto
- idx_movimientos_fecha_tipo
- idx_movimientos_usuario

---

## 🎯 Captura de Pantalla para tu Documentación

**Captura ESTO para tu entrega:**

1. **Resultado del script ejecutándose**
   - Muestra los mensajes de "✓ creados correctamente"

2. **SHOW INDEX FROM productos**
   - Captura la lista completa de índices

3. **Tabla de verificación**
   - La tabla que muestra total de índices por tabla

---

## ❌ Solución de Problemas

### Error: "Access denied for user"
```
Solución: Verifica tu usuario y contraseña de MySQL
mysql -u root -p
# O usa tu usuario específico
```

### Error: "Unknown database 'inventario_db'"
```
Solución: La base de datos no existe, créala primero
mysql -u root -p < scripts/01-init.sql
```

### Error: "Can't find file"
```
Solución: Verifica que estás en la carpeta correcta
cd /c/dam2/accesodatos/git/inventario_git
pwd  # Verificar ruta actual
```

### Warning: "Duplicate key name"
```
No es error: El índice ya existe, se omite
Los scripts usan IF NOT EXISTS para seguridad
```

---

## 📊 ¿Cuánto Tiempo Toma?

- **Ejecución del script:** 5-10 segundos
- **Verificación:** 1-2 minutos
- **Capturas:** 2-3 minutos
- **TOTAL:** ~5 minutos

---

## ✅ Checklist

- [ ] Ejecuté el script `05-aplicar-todas-optimizaciones.sql`
- [ ] Vi el mensaje "OPTIMIZACIONES APLICADAS CORRECTAMENTE ✓"
- [ ] Verifiqué con `SHOW INDEX FROM productos`
- [ ] Capturé pantallas para documentación
- [ ] productos tiene ~13 índices
- [ ] movimientos_stock tiene ~6 índices
- [ ] categorias tiene ~3 índices

---

## 🎉 ¡Listo!

Una vez completado este paso, tus consultas **automáticamente** serán más rápidas.

**Próximo paso:** PASO 2 - Ejecutar EXPLAIN y medir mejoras

---

**¿Tienes dudas o errores?** Pregúntame y te ayudo a resolverlos.
