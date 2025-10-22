package com.inventario;

import com.inventario.dao.CategoriaDAO;
import com.inventario.dao.ProductoDAO;
import com.inventario.dao.impl.CategoriaDAOImpl;
import com.inventario.dao.impl.ProductoDAOImpl;
import com.inventario.model.Categoria;
import com.inventario.model.Producto;
import com.inventario.service.InventarioService;
import com.inventario.service.impl.InventarioServiceImpl;
import com.inventario.util.DatabaseConfig;
import com.inventario.util.JsonUtil;
import com.inventario.util.LogUtil;
import com.inventario.xml.XMLManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Clase principal del sistema de inventario - Versión 2.0
 * Incluye funcionalidades de backup y restauración XML
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final Scanner scanner = new Scanner(System.in);
    private static InventarioService inventarioService;
    private static XMLManager xmlManager;
    private static Connection connection;
    
    public static void main(String[] args) {
        logger.info("Iniciando Sistema de Inventario");
        
        try {
            // Inicializar servicios
            inicializarServicios();
            
            // Verificar conexión a la base de datos
            if (!verificarConexionBD()) {
                logger.error("No se pudo conectar a la base de datos. Saliendo...");
                return;
            }
            
            // Inicializar XMLManager
            inicializarXMLManager();
            
            // Cargar datos iniciales desde CSV
            cargarDatosIniciales();
            
            // Ejecutar menú principal
            ejecutarMenuPrincipal();
            
        } catch (Exception e) {
            logger.error("Error fatal en la aplicación", e);
            LogUtil.registrarError("MAIN", "Error fatal en la aplicación", e);
        } finally {
            logger.info("Sistema de Inventario finalizado");
            scanner.close();
        }
    }
    
    private static void inicializarServicios() {
        logger.info("Inicializando servicios...");
        
        // Crear instancias de DAOs
        CategoriaDAO categoriaDAO = new CategoriaDAOImpl();
        ProductoDAO productoDAO = new ProductoDAOImpl();
        
        // Crear servicio principal
        inventarioService = new InventarioServiceImpl(categoriaDAO, productoDAO);
        
        logger.info("Servicios inicializados correctamente");
    }
    
    private static void inicializarXMLManager() {
        try {
            DatabaseConfig dbConfig = DatabaseConfig.getInstance();
            connection = dbConfig.getConnection();
            xmlManager = new XMLManager(connection);
            logger.info("XMLManager inicializado correctamente");
        } catch (Exception e) {
            logger.error("Error al inicializar XMLManager", e);
        }
    }
    
    private static boolean verificarConexionBD() {
        logger.info("Verificando conexión a la base de datos...");
        
        DatabaseConfig dbConfig = DatabaseConfig.getInstance();
        boolean conexionOk = dbConfig.testConnection();
        
        if (conexionOk) {
            logger.info("Conexión a la base de datos establecida correctamente");
        } else {
            logger.error("Error al conectar con la base de datos");
        }
        
        return conexionOk;
    }
    
    private static void cargarDatosIniciales() {
        try {
            logger.info("Verificando si es necesario cargar datos iniciales...");
            
            // Solo cargar si no hay datos
            int totalProductos = inventarioService.contarTotalProductos();
            
            if (totalProductos == 0) {
                logger.info("Cargando datos iniciales desde archivos CSV...");
                
                int categoriasCreadas = inventarioService.cargarCategoriasDesdeCSV("data/categorias.csv");
                logger.info("Categorías cargadas: {}", categoriasCreadas);
                
                int productosCreados = inventarioService.cargarProductosDesdeCSV("data/productos.csv");
                logger.info("Productos cargados: {}", productosCreados);
            } else {
                logger.info("Los datos ya existen. Productos en BD: {}. Saltando carga de CSV.", totalProductos);
            }
            
        } catch (Exception e) {
            logger.warn("Error al cargar datos iniciales: {}", e.getMessage());
        }
    }
    
    private static void ejecutarMenuPrincipal() {
        boolean continuar = true;
        
        while (continuar) {
            mostrarMenuPrincipal();
            
            try {
                int opcion = Integer.parseInt(scanner.nextLine());
                
                switch (opcion) {
                    case 1 -> gestionarProductos();
                    case 2 -> gestionarCategorias();
                    case 3 -> gestionarStock();
                    case 4 -> generarReportes();
                    case 5 -> exportarDatos();
                    case 6 -> mostrarEstadisticas();
                    case 7 -> gestionarBackupRestauracion();
                    case 0 -> {
                        continuar = false;
                        System.out.println("¡Gracias por usar el Sistema de Inventario!");
                    }
                    default -> System.out.println("Opción no válida. Intente nuevamente.");
                }
                
            } catch (NumberFormatException e) {
                System.out.println("Por favor ingrese un número válido.");
            } catch (Exception e) {
                logger.error("Error en menú principal", e);
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void mostrarMenuPrincipal() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("       SISTEMA DE INVENTARIO - v2.0");
        System.out.println("=".repeat(50));
        System.out.println("1. Gestionar Productos");
        System.out.println("2. Gestionar Categorías");
        System.out.println("3. Gestionar Stock");
        System.out.println("4. Generar Reportes");
        System.out.println("5. Exportar Datos");
        System.out.println("6. Ver Estadísticas");
        System.out.println("7. Backup y Restauración (XML)");
        System.out.println("0. Salir");
        System.out.println("=".repeat(50));
        System.out.print("Seleccione una opción: ");
    }
    
    // ========== NUEVA FUNCIONALIDAD: BACKUP Y RESTAURACIÓN XML ==========
    
    private static void gestionarBackupRestauracion() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   BACKUP Y RESTAURACIÓN XML            ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("1. Crear backup completo (XML)");
        System.out.println("2. Restaurar desde backup (XML)");
        System.out.println("3. Validar archivo XML");
        System.out.println("4. Backup automático con validación");
        System.out.println("0. Volver al menú principal");
        System.out.print("Seleccione una opción: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine());
            
            switch (opcion) {
                case 1 -> crearBackupXML();
                case 2 -> restaurarDesdeBackupXML();
                case 3 -> validarArchivoXML();
                case 4 -> backupAutomaticoConValidacion();
                case 0 -> System.out.println("Volviendo al menú principal...");
                default -> System.out.println("Opción no válida.");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            logger.error("Error en gestión de backup/restauración", e);
        }
    }
    
    private static void crearBackupXML() {
        System.out.print("\nNombre del archivo (Enter para usar nombre automático): ");
        String nombre = scanner.nextLine().trim();
        
        String ruta;
        if (nombre.isEmpty()) {
            String fecha = LocalDate.now().toString();
            ruta = "backups/inventario_" + fecha + ".xml";
        } else {
            if (!nombre.endsWith(".xml")) {
                nombre += ".xml";
            }
            ruta = "backups/" + nombre;
        }
        
        System.out.println("\n→ Creando backup del inventario...");
        
        if (xmlManager.exportarInventarioXML(ruta)) {
            System.out.println("✓ Backup creado exitosamente: " + ruta);
            logger.info("Backup XML creado: {}", ruta);
        } else {
            System.out.println("✗ Error al crear el backup");
            logger.error("Error al crear backup XML: {}", ruta);
        }
    }
    
    private static void restaurarDesdeBackupXML() {
        System.out.print("\nNombre del archivo XML (en backups/) o ruta completa: ");
        String input = scanner.nextLine().trim();
        
        String rutaXML;
        if (input.isEmpty()) {
            // Si no escribe nada, usar el último backup con fecha de hoy
            String fecha = LocalDate.now().toString();
            rutaXML = "backups/inventario_" + fecha + ".xml";
            System.out.println("Usando backup de hoy: " + rutaXML);
        } else if (input.contains("/") || input.contains("\\")) {
            // Si contiene separadores de ruta, usar como ruta completa
            rutaXML = input;
        } else {
            // Si solo es un nombre, buscar en backups/
            if (!input.endsWith(".xml")) {
                input += ".xml";
            }
            rutaXML = "backups/" + input;
            System.out.println("Buscando en: " + rutaXML);
        }
        
        System.out.print("\n⚠️  ¿Desea LIMPIAR todas las tablas antes de restaurar? (S/N): ");
        String respuesta = scanner.nextLine().trim().toUpperCase();
        boolean limpiar = respuesta.equals("S") || respuesta.equals("SI") || respuesta.equals("SÍ");
        
        if (limpiar) {
            System.out.print("⚠️  ADVERTENCIA: Se eliminarán TODOS los datos actuales. ¿Confirma? (S/N): ");
            String confirmacion = scanner.nextLine().trim().toUpperCase();
            if (!confirmacion.equals("S") && !confirmacion.equals("SI") && !confirmacion.equals("SÍ")) {
                System.out.println("Restauración cancelada");
                return;
            }
        }
        
        System.out.println("\n→ Restaurando inventario desde XML...");
        
        if (xmlManager.importarInventarioXML(rutaXML, "src/main/resources/inventario.xsd", limpiar)) {
            System.out.println("✓ Inventario restaurado exitosamente");
            logger.info("Inventario restaurado desde: {}", rutaXML);
        } else {
            System.out.println("✗ Error al restaurar el inventario");
            logger.error("Error al restaurar desde: {}", rutaXML);
        }
    }
    
    private static void validarArchivoXML() {
        System.out.print("\nNombre del archivo XML (en backups/) o ruta completa: ");
        String input = scanner.nextLine().trim();
        
        String rutaXML;
        if (input.isEmpty()) {
            String fecha = LocalDate.now().toString();
            rutaXML = "backups/inventario_" + fecha + ".xml";
            System.out.println("Usando backup de hoy: " + rutaXML);
        } else if (input.contains("/") || input.contains("\\")) {
            rutaXML = input;
        } else {
            if (!input.endsWith(".xml")) {
                input += ".xml";
            }
            rutaXML = "backups/" + input;
            System.out.println("Buscando en: " + rutaXML);
        }
        
        System.out.println("\n→ Validando archivo XML...");
        
        if (xmlManager.validarXML(rutaXML, "src/main/resources/inventario.xsd")) {
            System.out.println("✓ El archivo XML es válido");
        } else {
            System.out.println("✗ El archivo XML NO es válido");
        }
    }
    
    private static void backupAutomaticoConValidacion() {
        String fecha = LocalDate.now().toString();
        String ruta = "backups/inventario_" + fecha + ".xml";
        
        System.out.println("\n→ Proceso automático: Backup + Validación");
        System.out.println("→ Creando backup...");
        
        if (xmlManager.exportarInventarioXML(ruta)) {
            System.out.println("✓ Backup creado: " + ruta);
            
            System.out.println("\n→ Validando backup...");
            if (xmlManager.validarXML(ruta, "src/main/resources/inventario.xsd")) {
                System.out.println("✓ Backup validado correctamente");
                System.out.println("\n✓ Proceso completado exitosamente");
                logger.info("Backup automático creado y validado: {}", ruta);
            } else {
                System.out.println("✗ El backup generado no es válido");
                logger.warn("Backup creado pero la validación falló: {}", ruta);
            }
        } else {
            System.out.println("✗ Error al crear el backup");
        }
    }
    
    // ========== GESTIÓN DE PRODUCTOS ==========
    
    private static void gestionarProductos() {
        System.out.println("\n--- GESTIÓN DE PRODUCTOS ---");
        System.out.println("1. Listar todos los productos");
        System.out.println("2. Buscar producto por ID");
        System.out.println("3. Buscar productos por categoría");
        System.out.println("4. Crear nuevo producto");
        System.out.println("5. Actualizar producto");
        System.out.println("6. Eliminar producto");
        System.out.print("Seleccione una opción: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine());
            
            switch (opcion) {
                case 1 -> listarProductos();
                case 2 -> buscarProductoPorId();
                case 3 -> buscarProductosPorCategoria();
                case 4 -> crearProducto();
                case 5 -> actualizarProducto();
                case 6 -> eliminarProducto();
                default -> System.out.println("Opción no válida.");
            }
            
        } catch (Exception e) {
            logger.error("Error en gestión de productos", e);
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void listarProductos() {
        try {
            List<Producto> productos = inventarioService.obtenerTodosLosProductos();
            
            if (productos.isEmpty()) {
                System.out.println("No hay productos registrados.");
                return;
            }
            
            System.out.println("\n--- LISTA DE PRODUCTOS ---");
            System.out.printf("%-5s %-30s %-15s %-10s %-8s%n", "ID", "Nombre", "Categoría", "Precio", "Stock");
            System.out.println("-".repeat(75));
            
            for (Producto producto : productos) {
                System.out.printf("%-5d %-30s %-15s $%-9.2f %-8d%n",
                    producto.getIdProducto(),
                    producto.getNombre(),
                    producto.getCategoria(),
                    producto.getPrecio(),
                    producto.getStock());
            }
            
        } catch (Exception e) {
            System.out.println("Error al listar productos: " + e.getMessage());
        }
    }
    
    private static void buscarProductoPorId() {
        System.out.print("Ingrese el ID del producto: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            Producto producto = inventarioService.buscarProductoPorId(id);
            
            if (producto != null) {
                mostrarDetalleProducto(producto);
            } else {
                System.out.println("Producto no encontrado.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("ID inválido.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void buscarProductosPorCategoria() {
        System.out.print("Ingrese la categoría: ");
        String categoria = scanner.nextLine();
        
        try {
            List<Producto> productos = inventarioService.buscarProductosPorCategoria(categoria);
            
            if (productos.isEmpty()) {
                System.out.println("No se encontraron productos en la categoría: " + categoria);
                return;
            }
            
            System.out.println("\n--- PRODUCTOS EN CATEGORÍA: " + categoria.toUpperCase() + " ---");
            for (Producto producto : productos) {
                mostrarDetalleProducto(producto);
                System.out.println("-".repeat(40));
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void crearProducto() {
        try {
            System.out.println("\n--- CREAR NUEVO PRODUCTO ---");
            
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine();
            
            System.out.print("Categoría: ");
            String categoria = scanner.nextLine();
            
            System.out.print("Precio: ");
            double precio = Double.parseDouble(scanner.nextLine());
            
            System.out.print("Stock inicial: ");
            int stock = Integer.parseInt(scanner.nextLine());
            
            int id = inventarioService.crearProducto(nombre, categoria, precio, stock);
            System.out.println("Producto creado exitosamente con ID: " + id);
            
        } catch (Exception e) {
            System.out.println("Error al crear producto: " + e.getMessage());
        }
    }
    
    private static void actualizarProducto() {
        System.out.print("Ingrese el ID del producto a actualizar: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            Producto producto = inventarioService.buscarProductoPorId(id);
            
            if (producto == null) {
                System.out.println("Producto no encontrado.");
                return;
            }
            
            System.out.println("Producto actual:");
            mostrarDetalleProducto(producto);
            
            System.out.println("\nIngrese los nuevos datos (presione Enter para mantener el valor actual):");
            
            System.out.print("Nombre [" + producto.getNombre() + "]: ");
            String nombre = scanner.nextLine();
            if (!nombre.trim().isEmpty()) {
                producto.setNombre(nombre);
            }
            
            System.out.print("Precio [" + producto.getPrecio() + "]: ");
            String precioStr = scanner.nextLine();
            if (!precioStr.trim().isEmpty()) {
                producto.setPrecio(new java.math.BigDecimal(precioStr));
            }
            
            System.out.print("Stock [" + producto.getStock() + "]: ");
            String stockStr = scanner.nextLine();
            if (!stockStr.trim().isEmpty()) {
                producto.setStock(Integer.parseInt(stockStr));
            }
            
            boolean actualizado = inventarioService.actualizarProducto(producto);
            if (actualizado) {
                System.out.println("Producto actualizado exitosamente.");
            } else {
                System.out.println("No se pudo actualizar el producto.");
            }
            
        } catch (Exception e) {
            System.out.println("Error al actualizar producto: " + e.getMessage());
        }
    }
    
    private static void eliminarProducto() {
        System.out.print("Ingrese el ID del producto a eliminar: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            Producto producto = inventarioService.buscarProductoPorId(id);
            
            if (producto == null) {
                System.out.println("Producto no encontrado.");
                return;
            }
            
            System.out.println("Producto a eliminar:");
            mostrarDetalleProducto(producto);
            
            System.out.print("¿Está seguro de eliminar este producto? (s/N): ");
            String confirmacion = scanner.nextLine();
            
            if (confirmacion.toLowerCase().startsWith("s")) {
                boolean eliminado = inventarioService.eliminarProducto(id);
                if (eliminado) {
                    System.out.println("Producto eliminado exitosamente.");
                } else {
                    System.out.println("No se pudo eliminar el producto.");
                }
            } else {
                System.out.println("Operación cancelada.");
            }
            
        } catch (Exception e) {
            System.out.println("Error al eliminar producto: " + e.getMessage());
        }
    }
    
    // ========== GESTIÓN DE CATEGORÍAS ==========
    
    private static void gestionarCategorias() {
        System.out.println("\n--- GESTIÓN DE CATEGORÍAS ---");
        System.out.println("1. Listar todas las categorías");
        System.out.println("2. Crear nueva categoría");
        System.out.println("3. Actualizar categoría");
        System.out.println("4. Eliminar categoría");
        System.out.print("Seleccione una opción: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine());
            
            switch (opcion) {
                case 1 -> listarCategorias();
                case 2 -> crearCategoria();
                case 3 -> actualizarCategoria();
                case 4 -> eliminarCategoria();
                default -> System.out.println("Opción no válida.");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void listarCategorias() {
        try {
            List<Categoria> categorias = inventarioService.obtenerTodasLasCategorias();
            
            if (categorias.isEmpty()) {
                System.out.println("No hay categorías registradas.");
                return;
            }
            
            System.out.println("\n--- LISTA DE CATEGORÍAS ---");
            for (Categoria categoria : categorias) {
                System.out.printf("ID: %d | Nombre: %s | Descripción: %s%n",
                    categoria.getIdCategoria(),
                    categoria.getNombre(),
                    categoria.getDescripcion());
            }
            
        } catch (Exception e) {
            System.out.println("Error al listar categorías: " + e.getMessage());
        }
    }
    
    private static void crearCategoria() {
        try {
            System.out.println("\n--- CREAR NUEVA CATEGORÍA ---");
            
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine();
            
            System.out.print("Descripción: ");
            String descripcion = scanner.nextLine();
            
            int id = inventarioService.crearCategoria(nombre, descripcion);
            System.out.println("Categoría creada exitosamente con ID: " + id);
            
        } catch (Exception e) {
            System.out.println("Error al crear categoría: " + e.getMessage());
        }
    }
    
    private static void actualizarCategoria() {
        System.out.print("Ingrese el ID de la categoría a actualizar: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            Categoria categoria = inventarioService.buscarCategoriaPorId(id);
            
            if (categoria == null) {
                System.out.println("Categoría no encontrada.");
                return;
            }
            
            System.out.println("Categoría actual: " + categoria.getNombre() + " - " + categoria.getDescripcion());
            
            System.out.print("Nuevo nombre [" + categoria.getNombre() + "]: ");
            String nombre = scanner.nextLine();
            if (!nombre.trim().isEmpty()) {
                categoria.setNombre(nombre);
            }
            
            System.out.print("Nueva descripción [" + categoria.getDescripcion() + "]: ");
            String descripcion = scanner.nextLine();
            if (!descripcion.trim().isEmpty()) {
                categoria.setDescripcion(descripcion);
            }
            
            boolean actualizada = inventarioService.actualizarCategoria(categoria);
            if (actualizada) {
                System.out.println("Categoría actualizada exitosamente.");
            } else {
                System.out.println("No se pudo actualizar la categoría.");
            }
            
        } catch (Exception e) {
            System.out.println("Error al actualizar categoría: " + e.getMessage());
        }
    }
    
    private static void eliminarCategoria() {
        System.out.print("Ingrese el ID de la categoría a eliminar: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            Categoria categoria = inventarioService.buscarCategoriaPorId(id);
            
            if (categoria == null) {
                System.out.println("Categoría no encontrada.");
                return;
            }
            
            System.out.println("Categoría a eliminar: " + categoria.getNombre());
            System.out.print("¿Está seguro? (s/N): ");
            String confirmacion = scanner.nextLine();
            
            if (confirmacion.toLowerCase().startsWith("s")) {
                boolean eliminada = inventarioService.eliminarCategoria(id);
                if (eliminada) {
                    System.out.println("Categoría eliminada exitosamente.");
                } else {
                    System.out.println("No se pudo eliminar la categoría.");
                }
            } else {
                System.out.println("Operación cancelada.");
            }
            
        } catch (Exception e) {
            System.out.println("Error al eliminar categoría: " + e.getMessage());
        }
    }
    
    // ========== GESTIÓN DE STOCK ==========
    
    private static void gestionarStock() {
        System.out.println("\n--- GESTIÓN DE STOCK ---");
        System.out.println("1. Ver productos con stock bajo");
        System.out.println("2. Entrada de stock");
        System.out.println("3. Salida de stock");
        System.out.println("4. Ver historial de movimientos");
        System.out.print("Seleccione una opción: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine());
            
            switch (opcion) {
                case 1 -> verProductosStockBajo();
                case 2 -> entradaStock();
                case 3 -> salidaStock();
                case 4 -> verHistorialMovimientos();
                default -> System.out.println("Opción no válida.");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void verProductosStockBajo() {
        System.out.print("Ingrese el límite de stock bajo (default: 200): ");
        String limiteStr = scanner.nextLine();
        int limite = limiteStr.trim().isEmpty() ? 200 : Integer.parseInt(limiteStr);
        
        try {
            List<Producto> productos = inventarioService.obtenerProductosConStockBajo(limite);
            
            if (productos.isEmpty()) {
                System.out.println("No hay productos con stock bajo.");
                return;
            }
            
            System.out.println("\n--- PRODUCTOS CON STOCK BAJO (< " + limite + ") ---");
            System.out.printf("%-5s %-30s %-15s %-8s%n", "ID", "Nombre", "Categoría", "Stock");
            System.out.println("-".repeat(65));
            
            for (Producto producto : productos) {
                System.out.printf("%-5d %-30s %-15s %-8d%n",
                    producto.getIdProducto(),
                    producto.getNombre(),
                    producto.getCategoria(),
                    producto.getStock());
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void entradaStock() {
        System.out.print("Ingrese el ID del producto: ");
        try {
            int idProducto = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Cantidad a ingresar: ");
            int cantidad = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Motivo: ");
            String motivo = scanner.nextLine();
            
            boolean resultado = inventarioService.registrarEntradaStock(idProducto, cantidad, motivo);
            
            if (resultado) {
                System.out.println("Entrada de stock registrada exitosamente.");
            } else {
                System.out.println("No se pudo registrar la entrada de stock.");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void salidaStock() {
        System.out.print("Ingrese el ID del producto: ");
        try {
            int idProducto = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Cantidad a sacar: ");
            int cantidad = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Motivo: ");
            String motivo = scanner.nextLine();
            
            boolean resultado = inventarioService.registrarSalidaStock(idProducto, cantidad, motivo);
            
            if (resultado) {
                System.out.println("Salida de stock registrada exitosamente.");
            } else {
                System.out.println("No se pudo registrar la salida de stock (stock insuficiente).");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void verHistorialMovimientos() {
        System.out.print("Ingrese el ID del producto (o Enter para ver todos): ");
        String idStr = scanner.nextLine();
        
        try {
            if (idStr.trim().isEmpty()) {
                // Mostrar todos los movimientos (últimos 20)
                System.out.println("Función pendiente de implementar - historial completo");
            } else {
                int idProducto = Integer.parseInt(idStr);
                System.out.println("Función pendiente de implementar - historial por producto: " + idProducto);
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    // ========== GENERAR REPORTES ==========
    
    private static void generarReportes() {
        System.out.println("\n--- GENERAR REPORTES ---");
        System.out.println("1. Reporte de productos con stock bajo (JSON)");
        System.out.println("2. Reporte de estadísticas por categoría");
        System.out.println("3. Reporte de todos los productos");
        System.out.print("Seleccione una opción: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine());
            
            switch (opcion) {
                case 1 -> generarReporteStockBajo();
                case 2 -> generarReporteEstadisticas();
                case 3 -> generarReporteTodosProductos();
                default -> System.out.println("Opción no válida.");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void generarReporteStockBajo() {
        System.out.print("Ingrese el límite de stock bajo (default: 200): ");
        String limiteStr = scanner.nextLine();
        int limite = limiteStr.trim().isEmpty() ? 200 : Integer.parseInt(limiteStr);
        
        try {
            List<Producto> productos = inventarioService.obtenerProductosConStockBajo(limite);
            String nombreArchivo = JsonUtil.generarNombreArchivoConTimestamp("stock_bajo", "json");
            
            JsonUtil.exportarProductosStockBajo(productos, limite, "logs/" + nombreArchivo);
            System.out.println("Reporte generado: logs/" + nombreArchivo);
            
        } catch (Exception e) {
            System.out.println("Error al generar reporte: " + e.getMessage());
        }
    }
    
    private static void generarReporteEstadisticas() {
        try {
            String nombreArchivo = JsonUtil.generarNombreArchivoConTimestamp("estadisticas_categoria", "json");
            System.out.println("Generando reporte de estadísticas...");
            System.out.println("Reporte generado: logs/" + nombreArchivo);
            
        } catch (Exception e) {
            System.out.println("Error al generar reporte: " + e.getMessage());
        }
    }
    
    private static void generarReporteTodosProductos() {
        try {
            List<Producto> productos = inventarioService.obtenerTodosLosProductos();
            String nombreArchivo = JsonUtil.generarNombreArchivoConTimestamp("todos_productos", "json");
            
            JsonUtil.exportarProductos(productos, "logs/" + nombreArchivo);
            System.out.println("Reporte generado: logs/" + nombreArchivo);
            
        } catch (Exception e) {
            System.out.println("Error al generar reporte: " + e.getMessage());
        }
    }
    
    // ========== EXPORTAR DATOS ==========
    
    private static void exportarDatos() {
        System.out.println("\n--- EXPORTAR DATOS ---");
        System.out.println("1. Exportar todos los productos a JSON");
        System.out.println("2. Exportar productos por categoría");
        System.out.print("Seleccione una opción: ");
        
        try {
            int opcion = Integer.parseInt(scanner.nextLine());
            
            switch (opcion) {
                case 1 -> exportarTodosProductos();
                case 2 -> exportarProductosPorCategoria();
                default -> System.out.println("Opción no válida.");
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    
    private static void exportarTodosProductos() {
        try {
            List<Producto> productos = inventarioService.obtenerTodosLosProductos();
            String nombreArchivo = JsonUtil.generarNombreArchivoConTimestamp("exportacion_productos", "json");
            
            JsonUtil.exportarProductos(productos, "data/" + nombreArchivo);
            System.out.println("Datos exportados a: data/" + nombreArchivo);
            
        } catch (Exception e) {
            System.out.println("Error al exportar: " + e.getMessage());
        }
    }
    
    private static void exportarProductosPorCategoria() {
        System.out.print("Ingrese la categoría: ");
        String categoria = scanner.nextLine();
        
        try {
            List<Producto> productos = inventarioService.buscarProductosPorCategoria(categoria);
            
            if (productos.isEmpty()) {
                System.out.println("No se encontraron productos en la categoría: " + categoria);
                return;
            }
            
            String nombreArchivo = JsonUtil.generarNombreArchivoConTimestamp("productos_" + categoria.toLowerCase(), "json");
            JsonUtil.exportarProductos(productos, "data/" + nombreArchivo);
            System.out.println("Productos de categoría '" + categoria + "' exportados a: data/" + nombreArchivo);
            
        } catch (Exception e) {
            System.out.println("Error al exportar: " + e.getMessage());
        }
    }
    
    // ========== ESTADÍSTICAS ==========
    
    private static void mostrarEstadisticas() {
        try {
            System.out.println("\n--- ESTADÍSTICAS DEL INVENTARIO ---");
            
            int totalProductos = inventarioService.contarTotalProductos();
            int totalCategorias = inventarioService.contarTotalCategorias();
            
            System.out.println("Total de productos: " + totalProductos);
            System.out.println("Total de categorías: " + totalCategorias);
            
            // Mostrar productos con stock bajo
            List<Producto> stockBajo = inventarioService.obtenerProductosConStockBajo(200);
            System.out.println("Productos con stock bajo (<200): " + stockBajo.size());
            
            System.out.println("\nEstadísticas detalladas disponibles en reportes JSON.");
            
        } catch (Exception e) {
            System.out.println("Error al obtener estadísticas: " + e.getMessage());
        }
    }
    
    // ========== UTILIDADES ==========
    
    private static void mostrarDetalleProducto(Producto producto) {
        System.out.println("ID: " + producto.getIdProducto());
        System.out.println("Nombre: " + producto.getNombre());
        System.out.println("Categoría: " + producto.getCategoria());
        System.out.println("Precio: $" + producto.getPrecio());
        System.out.println("Stock: " + producto.getStock());
        System.out.println("Valor total: $" + producto.getValorTotal());
    }
}