# Sistema de Inventario - Actividad Evaluable 1

Sistema completo de gestión de inventario desarrollado en Java que permite cargar datos desde archivos CSV, gestionar productos y categorías mediante operaciones CRUD, registrar movimientos de stock con transacciones, y exportar reportes en formato JSON.

## Información del Proyecto

- **Materia**: Acceso a Datos
- **Nivel**: 2º DAM (Desarrollo de Aplicaciones Multiplataforma)
- **Ponderación**: 15%
- **Autor**: Esteban Garces
- **Fecha**: 02/10/2025

## Índice

1. [Características Principales](#características-principales)
2. [Requisitos del Sistema](#requisitos-del-sistema)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Instalación y Configuración](#instalación-y-configuración)
5. [Uso de la Aplicación](#uso-de-la-aplicación)
6. [Base de Datos](#base-de-datos)
7. [Archivos CSV](#archivos-csv)
8. [Funcionalidades Detalladas](#funcionalidades-detalladas)
9. [Sistema de Logging](#sistema-de-logging)
10. [Exportación de Datos](#exportación-de-datos)
11. [Solución de Problemas](#solución-de-problemas)
12. [Tecnologías Utilizadas](#tecnologías-utilizadas)

---

## Características Principales

### Gestión de Datos
- **Carga automática desde CSV**: Importa productos y categorías desde archivos CSV con validación de datos y manejo robusto de errores
- **Validación de estructura**: Verifica que los archivos CSV tengan el formato correcto antes de procesar
- **Manejo de duplicados**: Evita la inserción de datos duplicados en la base de datos
- **Normalización automática**: Convierte categorías con acentos a formato sin acentos para mantener consistencia

### Operaciones CRUD
- **Productos**: Crear, leer, actualizar y eliminar productos con todas sus propiedades
- **Categorías**: Gestión completa de categorías con validación de integridad referencial
- **Búsquedas avanzadas**: Por ID, nombre, categoría, rango de precio, y stock bajo
- **Validación de datos**: Verifica que los datos sean válidos antes de guardarlos

### Gestión de Stock
- **Entradas y salidas**: Registro de movimientos de stock con motivo y usuario
- **Transacciones seguras**: Uso de transacciones ACID con rollback automático ante errores
- **Historial completo**: Registro de todos los movimientos con stock anterior y nuevo
- **Validación de stock**: Impide ventas con stock insuficiente

### Reportes y Exportación
- **Exportación a JSON**: Genera reportes en formato JSON con timestamp automático
- **Productos con stock bajo**: Reporte filtrado de productos que requieren reposición
- **Estadísticas por categoría**: Análisis agregado con totales y promedios
- **Formato legible**: JSON con indentación para fácil lectura

### Sistema de Logging
- **Logs detallados**: Registro de todas las operaciones con timestamp y nivel
- **Logs de errores**: Archivo separado para errores con stack traces completos
- **Reportes de operaciones**: Estadísticas de rendimiento y tasas de éxito
- **Rotación automática**: Gestión de logs antiguos para optimizar espacio

---

## Requisitos del Sistema

### Software Necesario
- **Sistema Operativo**: Windows 11 / Windows 10 / Linux / macOS
- **Docker Desktop**: Versión 20.10 o superior
- **Docker Compose**: Versión 2.0 o superior (incluido en Docker Desktop)
- **Navegador Web**: Para acceder a phpMyAdmin

### Software Incluido en Contenedores
- Java 17 (OpenJDK)
- Maven 3.8+
- MySQL 8.0
- phpMyAdmin (última versión)

### Requisitos de Hardware
- **RAM**: Mínimo 4 GB (recomendado 8 GB)
- **Disco**: 2 GB de espacio libre
- **Procesador**: Compatible con x64

---

## Estructura del Proyecto

```
sistema-inventario/
│
├── src/
│   └── main/
│       ├── java/com/inventario/
│       │   ├── dao/                      # Interfaces DAO
│       │   │   ├── CategoriaDAO.java
│       │   │   ├── ProductoDAO.java
│       │   │   └── MovimientoStockDAO.java
│       │   │
│       │   ├── dao/impl/                 # Implementaciones DAO
│       │   │   ├── CategoriaDAOImpl.java
│       │   │   └── ProductoDAOImpl.java
│       │   │
│       │   ├── model/                    # Clases de modelo
│       │   │   ├── Categoria.java
│       │   │   ├── Producto.java
│       │   │   └── MovimientoStock.java
│       │   │
│       │   ├── service/                  # Interfaces de servicio
│       │   │   └── InventarioService.java
│       │   │
│       │   ├── service/impl/             # Implementaciones de servicio
│       │   │   └── InventarioServiceImpl.java
│       │   │
│       │   ├── util/                     # Clases de utilidad
│       │   │   ├── DatabaseConfig.java   # Configuración de BD
│       │   │   ├── CsvUtil.java          # Lectura/escritura CSV
│       │   │   ├── JsonUtil.java         # Exportación JSON
│       │   │   └── LogUtil.java          # Sistema de logs
│       │   │
│       │   └── Main.java                 # Clase principal
│       │
│       └── resources/
│           └── logback.xml               # Configuración de logging
│
├── data/                                 # Archivos de datos
│   ├── productos.csv                     # CSV de productos
│   └── categorias.csv                    # CSV de categorías
│
├── logs/                                 # Archivos de log
│   ├── inventario.log                    # Log general
│   ├── errores.log                       # Log de errores
│   ├── actividades.log                   # Log de actividades
│   └── reportes_operaciones.log          # Reportes de rendimiento
│
├── scripts/                              # Scripts SQL
│   └── 01-init.sql                       # Script de inicialización
│
├── docker-compose.yml                    # Configuración Docker Compose
├── Dockerfile                            # Imagen de la aplicación
├── pom.xml                              # Configuración Maven
└── README.md                            # Este archivo
```

---

## Instalación y Configuración

### Paso 1: Preparar el Entorno

1. **Instalar Docker Desktop**
   - Descargar desde: https://www.docker.com/products/docker-desktop
   - Ejecutar el instalador y seguir las instrucciones
   - Reiniciar el ordenador si es necesario
   - Verificar instalación: `docker --version` y `docker-compose --version`

2. **Clonar o descargar el proyecto**
   ```bash
   git clone https://github.com/Speeson/AccesoADatos/tree/master/inventario_git
   cd sistema-inventario
   ```

### Paso 2: Configuración de Puertos

El proyecto usa estos puertos por defecto:
- **MySQL**: 33060 (externo) → 3306 (interno)
- **phpMyAdmin**: 9090 (externo) → 80 (interno)

Si algún puerto está ocupado, edita `docker-compose.yml`:

```yaml
services:
  mysql:
    ports:
      - "33061:3306"  # Cambiar 33060 por otro puerto libre
```

### Paso 3: Levantar los Servicios

**En Windows (PowerShell):**
```powershell
# Navegar al directorio del proyecto
cd C:\ruta\a\inventario

# Levantar todos los servicios
docker-compose up -d

# Verificar que estén corriendo
docker-compose ps
```

**Deberías ver 3 servicios:**
- `inventario_mysql` (MySQL)
- `inventario_phpmyadmin` (phpMyAdmin)
- `inventario_app` (Aplicación Java)

### Paso 4: Verificar la Base de Datos

1. Abrir phpMyAdmin: http://localhost:9090
2. Credenciales:
   - **Usuario**: `inventario_user`
   - **Contraseña**: `inventario_pass`
3. Seleccionar base de datos: `inventario_db`
4. Verificar que existen las tablas: `categorias`, `productos`, `movimientos_stock`, `logs_aplicacion`

---

## Uso de la Aplicación

### Ejecutar la Aplicación

```powershell
# Entrar al contenedor de la aplicación
docker-compose exec app bash

# Ejecutar el sistema
mvn exec:java -Dexec.mainClass="com.inventario.Main"
```

### Menú Principal

Al iniciar, verás el menú principal:

```
==================================================
           SISTEMA DE INVENTARIO
==================================================
1. Gestionar Productos
2. Gestionar Categorías
3. Gestionar Stock
4. Generar Reportes
5. Exportar Datos
6. Ver Estadísticas
0. Salir
==================================================
```

### Ejemplo de Uso: Listar Productos

1. Seleccionar opción `1` (Gestionar Productos)
2. Seleccionar opción `1` (Listar todos los productos)
3. Ver la lista completa con ID, nombre, categoría, precio y stock

### Ejemplo de Uso: Crear Producto

1. Seleccionar opción `1` (Gestionar Productos)
2. Seleccionar opción `4` (Crear nuevo producto)
3. Ingresar datos:
   - Nombre: `Smartphone Samsung Galaxy`
   - Categoría: `Electronica`
   - Precio: `699.99`
   - Stock inicial: `50`
4. El sistema confirma la creación con el ID asignado

### Ejemplo de Uso: Registrar Entrada de Stock

1. Seleccionar opción `3` (Gestionar Stock)
2. Seleccionar opción `2` (Entrada de stock)
3. Ingresar:
   - ID del producto: `5`
   - Cantidad: `100`
   - Motivo: `Reposición semanal`
4. El sistema actualiza el stock y registra el movimiento

### Ejemplo de Uso: Generar Reporte de Stock Bajo

1. Seleccionar opción `4` (Generar Reportes)
2. Seleccionar opción `1` (Reporte de stock bajo)
3. Ingresar límite (ej: `200`)
4. El sistema genera un archivo JSON en `logs/stock_bajo_YYYYMMDD_HHMMSS.json`

---

## Base de Datos

### Esquema de la Base de Datos

#### Tabla: `categorias`
```sql
CREATE TABLE categorias (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**Campos:**
- `id_categoria`: Identificador único
- `nombre`: Nombre de la categoría (único)
- `descripcion`: Descripción opcional
- `fecha_creacion`: Fecha de creación automática
- `fecha_modificacion`: Fecha de última modificación

#### Tabla: `productos`
```sql
CREATE TABLE productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    categoria VARCHAR(100) NOT NULL,
    precio DECIMAL(10,2) NOT NULL CHECK (precio >= 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria) REFERENCES categorias(nombre) ON UPDATE CASCADE
);
```

**Campos:**
- `id_producto`: Identificador único
- `nombre`: Nombre del producto
- `categoria`: Categoría (clave foránea a categorias.nombre)
- `precio`: Precio con 2 decimales (no negativo)
- `stock`: Cantidad disponible (no negativa)
- `fecha_creacion`: Fecha de creación automática
- `fecha_modificacion`: Fecha de última modificación

#### Tabla: `movimientos_stock`
```sql
CREATE TABLE movimientos_stock (
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
```

**Campos:**
- `id_movimiento`: Identificador único
- `id_producto`: Producto relacionado
- `tipo_movimiento`: ENTRADA o SALIDA
- `cantidad`: Cantidad del movimiento
- `stock_anterior`: Stock antes del movimiento
- `stock_nuevo`: Stock después del movimiento
- `motivo`: Razón del movimiento
- `fecha_movimiento`: Timestamp del movimiento
- `usuario`: Usuario que realizó el movimiento

### Triggers Automáticos

El sistema incluye un trigger que registra automáticamente los movimientos de stock:

```sql
CREATE TRIGGER tr_producto_stock_update
AFTER UPDATE ON productos
FOR EACH ROW
BEGIN
    IF OLD.stock != NEW.stock THEN
        INSERT INTO movimientos_stock (...)
        VALUES (...);
    END IF;
END;
```

### Vistas Disponibles

#### Vista: `v_productos_stock_bajo`
Muestra productos con stock menor a 200 unidades

#### Vista: `v_estadisticas_categoria`
Estadísticas agregadas por categoría con totales y promedios

---

## Archivos CSV

### Formato de `productos.csv`

El archivo debe usar **punto y coma (;)** como separador:

```csv
id_producto;nombre;categoria;precio;stock
1;Smartphone Samsung;Electronica;699.99;50
2;Camiseta Adidas;Ropa;29.99;150
3;Laptop Dell XPS;Informatica;1299.00;25
```

**Campos:**
- `id_producto`: Número único (opcional, se genera automáticamente)
- `nombre`: Nombre del producto (requerido)
- `categoria`: Categoría del producto (requerido, debe existir)
- `precio`: Precio decimal (requerido, >= 0)
- `stock`: Cantidad entera (requerido, >= 0)

**Notas importantes:**
- Las categorías con acentos se normalizan automáticamente
- Si id_producto no se proporciona, se genera automáticamente
- El archivo debe estar en UTF-8

### Formato de `categorias.csv`

```csv
id_categoria;nombre;descripcion
1;Electronica;Dispositivos electrónicos y accesorios
2;Ropa;Prendas de vestir y complementos
3;Informatica;Equipos informáticos y accesorios
```

**Campos:**
- `id_categoria`: Número único (opcional)
- `nombre`: Nombre de la categoría (requerido, único)
- `descripcion`: Descripción opcional

---

## Funcionalidades Detalladas

### 1. Gestión de Productos

**Listar Productos**
- Muestra todos los productos en formato tabular
- Incluye ID, nombre, categoría, precio y stock
- Ordenados por nombre

**Buscar Producto**
- Por ID: Búsqueda exacta
- Por nombre: Búsqueda parcial (LIKE)
- Por categoría: Todos los productos de una categoría

**Crear Producto**
- Validación de datos de entrada
- Creación automática de categoría si no existe
- Asignación automática de ID
- Registro en logs

**Actualizar Producto**
- Modificación de nombre, precio y stock
- Validación de datos
- Actualización de timestamp automática

**Eliminar Producto**
- Confirmación antes de eliminar
- Eliminación en cascada de movimientos
- Registro en logs

### 2. Gestión de Categorías

**Listar Categorías**
- Muestra todas con ID, nombre y descripción

**Crear Categoría**
- Validación de nombre único
- Descripción opcional

**Actualizar Categoría**
- Modificación de nombre y descripción
- Actualización de productos relacionados (ON UPDATE CASCADE)

**Eliminar Categoría**
- Verifica que no tenga productos asociados
- Impide eliminación si tiene productos

### 3. Gestión de Stock

**Ver Stock Bajo**
- Configurable con límite personalizado
- Por defecto muestra productos < 200 unidades
- Ordenado por stock ascendente

**Registrar Entrada**
- Aumenta el stock
- Requiere motivo
- Registra en movimientos_stock

**Registrar Salida**
- Disminuye el stock
- Valida stock suficiente
- Requiere motivo

### 4. Reportes

**Reporte de Stock Bajo (JSON)**
```json
{
  "fecha_reporte": "2024-10-02T15:30:45",
  "limite_stock_bajo": 200,
  "total_productos_stock_bajo": 15,
  "productos_stock_bajo": [...],
  "estadisticas": {
    "stock_total": 1250,
    "valor_total": "45678.90"
  }
}
```

**Reporte de Todos los Productos**
- Exportación completa en JSON
- Incluye todos los campos
- Timestamp en nombre de archivo

**Estadísticas por Categoría**
- Total de productos por categoría
- Stock total
- Precio promedio
- Valor total del inventario

---

## Sistema de Logging

### Niveles de Log

- **INFO**: Operaciones normales exitosas
- **WARN**: Advertencias que no detienen la ejecución
- **ERROR**: Errores que requieren atención
- **DEBUG**: Información detallada para desarrollo

### Archivos de Log

**`logs/inventario.log`**
- Log general de todas las operaciones
- Rotación automática diaria
- Retención: 30 días
- Tamaño máximo por archivo: 10 MB

**`logs/errores.log`**
- Solo errores (nivel ERROR)
- Incluye stack traces completos
- Retención: 90 días

**`logs/actividades.log`**
- Registro de actividades de usuario
- Formato: `[timestamp] [nivel] operacion - mensaje`
- Append mode (no sobrescribe)

**`logs/reportes_operaciones.log`**
- Estadísticas de rendimiento
- Tasa de éxito de operaciones
- Tiempo de ejecución

### Ejemplo de Entrada de Log

```
[2024-10-02 15:30:45] INFO - CREAR_PRODUCTO: Producto creado: Smartphone Samsung (ID: 1523)
[2024-10-02 15:31:02] ERROR - CARGAR_PRODUCTOS_CSV: Error al cargar productos desde CSV
java.lang.Exception: Estructura del archivo CSV inválida
    at com.inventario.service.impl.InventarioServiceImpl...
```

---

## Exportación de Datos

### Formato JSON

Todos los reportes se exportan en JSON con:
- Formato legible (indentación)
- Timestamp en el nombre del archivo
- Metadatos del reporte
- Datos solicitados

### Ubicación de Exportaciones

- **Reportes**: `logs/nombre_reporte_YYYYMMDD_HHMMSS.json`
- **Exportaciones**: `data/exportacion_YYYYMMDD_HHMMSS.json`

### Tipos de Exportación

1. **Stock Bajo**: Productos bajo un límite con estadísticas
2. **Todos los Productos**: Exportación completa
3. **Por Categoría**: Filtrado por categoría específica
4. **Estadísticas**: Análisis agregado por categoría

---

## Solución de Problemas

### Error: Puerto 33060 ya en uso

**Problema**: MySQL local está usando el puerto

**Solución 1**: Cambiar el puerto en `docker-compose.yml`
```yaml
ports:
  - "33061:3306"  # Usar puerto diferente
```

**Solución 2**: Detener MySQL local
```powershell
net stop MySQL80
```

### Error: No se pueden cargar los productos

**Problema**: Formato CSV incorrecto

**Soluciones**:
1. Verificar que usa punto y coma (;) como separador
2. Verificar encoding UTF-8
3. Revisar logs en `logs/productos_errores.log`
4. Validar que las categorías existen

### Error: Conexión rechazada a MySQL

**Problema**: MySQL no está listo

**Solución**: Esperar 30-60 segundos después de `docker-compose up -d`

```powershell
# Ver estado de MySQL
docker-compose logs mysql

# Esperar mensaje "ready for connections"
```

### Los datos se cargan múltiples veces

**Problema**: El código no verifica si ya existen datos

**Solución**: Ya implementado en `Main.java` - verifica conteo antes de cargar

### Muchas conexiones DEBUG en logs

**Problema**: Nivel de log muy detallado

**Solución**: Cambiar en `logback.xml`
```xml
<logger name="com.inventario.util.DatabaseConfig" level="INFO">
```

---

## Tecnologías Utilizadas

### Backend
- **Java 17**: Lenguaje de programación
- **Maven 3.8+**: Gestión de dependencias y construcción
- **JDBC**: Conectividad con base de datos
- **SLF4J + Logback**: Sistema de logging

### Base de Datos
- **MySQL 8.0**: Sistema de gestión de base de datos
- **phpMyAdmin**: Administración web de MySQL

### Bibliotecas Java
- **Apache Commons CSV 1.9.0**: Lectura/escritura de CSV
- **Jackson 2.15.2**: Serialización/deserialización JSON
- **MySQL Connector 8.0.33**: Driver JDBC para MySQL

### DevOps
- **Docker**: Contenedorización
- **Docker Compose**: Orquestación de servicios

### Arquitectura
- **Patrón DAO**: Separación de lógica de acceso a datos
- **Patrón Service**: Lógica de negocio encapsulada
- **Transacciones ACID**: Integridad de datos
- **PreparedStatements**: Seguridad SQL injection

---

## Variables de Entorno

El sistema usa estas variables (ya configuradas en `docker-compose.yml`):

```yaml
DB_HOST: mysql
DB_PORT: 3306
DB_NAME: inventario_db
DB_USER: inventario_user
DB_PASSWORD: inventario_pass
```

Para cambiar credenciales, edita `docker-compose.yml` en ambas secciones (mysql y app).

---

## Comandos Útiles

### Docker

```bash
# Levantar servicios
docker-compose up -d

# Ver estado
docker-compose ps

# Ver logs
docker-compose logs -f app

# Parar servicios
docker-compose down

# Parar y eliminar volúmenes (⚠️ elimina BD)
docker-compose down -v

# Reconstruir aplicación
docker-compose build app

# Reiniciar un servicio
docker-compose restart app
```

### Maven (dentro del contenedor)

```bash
# Compilar
mvn compile

# Limpiar y compilar
mvn clean compile

# Ejecutar aplicación
mvn exec:java -Dexec.mainClass="com.inventario.Main"

# Ver dependencias
mvn dependency:tree
```

---

## Próximas Mejoras

- [ ] Interfaz web con Spring Boot
- [ ] API REST para integración externa
- [ ] Autenticación y autorización de usuarios
- [ ] Reportes en PDF
- [ ] Dashboard en tiempo real
- [ ] Alertas automáticas de stock bajo
- [ ] Integración con código de barras
- [ ] Backup automático de base de datos

---

## Contacto y Soporte

Para preguntas o problemas:
- **Email**: [e.garces@pro2fp.es]
- **GitHub**: [Speeson]

---

**Fecha de última actualización**: Octubre 2024  
**Versión**: 1.0.0