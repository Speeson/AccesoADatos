package com.inventario.service.impl;

import com.inventario.dao.CategoriaDAO;
import com.inventario.dao.ProductoDAO;
import com.inventario.model.Categoria;
import com.inventario.model.Producto;
import com.inventario.service.InventarioService;
import com.inventario.util.CsvUtil;
import com.inventario.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación del servicio principal de inventario - Versión corregida
 */
public class InventarioServiceImpl implements InventarioService {
    private static final Logger logger = LoggerFactory.getLogger(InventarioServiceImpl.class);
    
    private final CategoriaDAO categoriaDAO;
    private final ProductoDAO productoDAO;
    
    public InventarioServiceImpl(CategoriaDAO categoriaDAO, ProductoDAO productoDAO) {
        this.categoriaDAO = categoriaDAO;
        this.productoDAO = productoDAO;
    }
    
    // === GESTIÓN DE PRODUCTOS ===
    
    @Override
public int cargarProductosDesdeCSV(String rutaArchivo) throws Exception {
    logger.info("Iniciando carga de productos desde CSV: {}", rutaArchivo);
    
    try {
        if (!CsvUtil.validarEstructuraProductos(rutaArchivo)) {
            throw new Exception("Estructura del archivo CSV inválida");
        }
        
        List<Producto> productos = CsvUtil.leerProductosCSV(rutaArchivo);
        
        if (productos.isEmpty()) {
            LogUtil.registrarAdvertencia("CARGAR_PRODUCTOS_CSV", "No se encontraron productos válidos");
            return 0;
        }
        
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
                    categoriaDAO.crear(nuevaCategoria);
                    logger.info("Categoría creada: {}", nombreCategoria);
                } catch (Exception e) {
                    logger.warn("Error creando categoría {}: {}", nombreCategoria, e.getMessage());
                }
            }
        }
        
        // Crear productos en lote
        int productosCreados = productoDAO.crearMultiples(productos);
        
        LogUtil.registrarOperacionExitosa("CARGAR_PRODUCTOS_CSV", 
            String.format("Cargados %d productos", productosCreados));
        
        return productosCreados;
        
    } catch (Exception e) {
        LogUtil.registrarError("CARGAR_PRODUCTOS_CSV", "Error al cargar productos", e);
        throw e;
    }
}
    
    @Override
    public List<Producto> obtenerTodosLosProductos() throws Exception {
        try {
            List<Producto> productos = productoDAO.obtenerTodos();
            logger.debug("Obtenidos {} productos", productos.size());
            return productos;
        } catch (Exception e) {
            LogUtil.registrarError("OBTENER_TODOS_PRODUCTOS", "Error al obtener todos los productos", e);
            throw e;
        }
    }
    
    @Override
    public Producto buscarProductoPorId(int id) throws Exception {
        try {
            Optional<Producto> producto = productoDAO.buscarPorId(id);
            if (producto.isPresent()) {
                logger.debug("Producto encontrado: ID {}", id);
                return producto.get();
            } else {
                logger.debug("Producto no encontrado: ID {}", id);
                return null;
            }
        } catch (Exception e) {
            LogUtil.registrarError("BUSCAR_PRODUCTO_ID", "Error al buscar producto por ID: " + id, e);
            throw e;
        }
    }
    
    @Override
    public List<Producto> buscarProductosPorCategoria(String categoria) throws Exception {
        try {
            List<Producto> productos = productoDAO.buscarPorCategoria(categoria);
            logger.debug("Encontrados {} productos en categoría: {}", productos.size(), categoria);
            return productos;
        } catch (Exception e) {
            LogUtil.registrarError("BUSCAR_PRODUCTOS_CATEGORIA", 
                "Error al buscar productos por categoría: " + categoria, e);
            throw e;
        }
    }
    
    @Override
    public List<Producto> obtenerProductosConStockBajo(int limite) throws Exception {
        try {
            List<Producto> productos = productoDAO.obtenerConStockBajo(limite);
            logger.debug("Encontrados {} productos con stock bajo (< {})", productos.size(), limite);
            return productos;
        } catch (Exception e) {
            LogUtil.registrarError("OBTENER_STOCK_BAJO", 
                "Error al obtener productos con stock bajo", e);
            throw e;
        }
    }
    
    @Override
    public int crearProducto(String nombre, String categoria, double precio, int stock) throws Exception {
        try {
            // Verificar/crear categoría si no existe
            if (!categoriaDAO.existePorNombre(categoria)) {
                Categoria nuevaCategoria = new Categoria(categoria, "Categoría creada automáticamente");
                categoriaDAO.crear(nuevaCategoria);
                logger.info("Categoría creada automáticamente: {}", categoria);
            }
            
            // Crear producto
            Producto producto = new Producto(nombre, categoria, BigDecimal.valueOf(precio), stock);
            
            if (!producto.isValid()) {
                throw new Exception("Datos del producto inválidos");
            }
            
            int id = productoDAO.crear(producto);
            
            LogUtil.registrarOperacionExitosa("CREAR_PRODUCTO", 
                String.format("Producto creado: %s (ID: %d)", nombre, id));
            
            return id;
            
        } catch (Exception e) {
            LogUtil.registrarError("CREAR_PRODUCTO", "Error al crear producto: " + nombre, e);
            throw e;
        }
    }
    
    @Override
    public boolean actualizarProducto(Producto producto) throws Exception {
        try {
            if (!producto.isValid()) {
                throw new Exception("Datos del producto inválidos");
            }
            
            // Verificar/crear categoría si no existe
            if (!categoriaDAO.existePorNombre(producto.getCategoria())) {
                Categoria nuevaCategoria = new Categoria(producto.getCategoria(), "Categoría creada automáticamente");
                categoriaDAO.crear(nuevaCategoria);
                logger.info("Categoría creada automáticamente: {}", producto.getCategoria());
            }
            
            boolean actualizado = productoDAO.actualizar(producto);
            
            if (actualizado) {
                LogUtil.registrarOperacionExitosa("ACTUALIZAR_PRODUCTO", 
                    String.format("Producto actualizado: %s (ID: %d)", 
                        producto.getNombre(), producto.getIdProducto()));
            }
            
            return actualizado;
            
        } catch (Exception e) {
            LogUtil.registrarError("ACTUALIZAR_PRODUCTO", 
                "Error al actualizar producto ID: " + producto.getIdProducto(), e);
            throw e;
        }
    }
    
    @Override
    public boolean eliminarProducto(int id) throws Exception {
        try {
            boolean eliminado = productoDAO.eliminar(id);
            
            if (eliminado) {
                LogUtil.registrarOperacionExitosa("ELIMINAR_PRODUCTO", 
                    "Producto eliminado ID: " + id);
            }
            
            return eliminado;
            
        } catch (Exception e) {
            LogUtil.registrarError("ELIMINAR_PRODUCTO", "Error al eliminar producto ID: " + id, e);
            throw e;
        }
    }
    
    @Override
    public int contarTotalProductos() throws Exception {
        try {
            return productoDAO.contarTotal();
        } catch (Exception e) {
            LogUtil.registrarError("CONTAR_PRODUCTOS", "Error al contar productos", e);
            throw e;
        }
    }
    
    // === GESTIÓN DE CATEGORÍAS ===
    
    @Override
    public int cargarCategoriasDesdeCSV(String rutaArchivo) throws Exception {
        logger.info("Iniciando carga de categorías desde CSV: {}", rutaArchivo);
        
        try {
            // Validar estructura del archivo
            if (!CsvUtil.validarEstructuraCategorias(rutaArchivo)) {
                throw new Exception("Estructura del archivo CSV inválida");
            }
            
            // Leer categorías del CSV
            List<Categoria> categorias = CsvUtil.leerCategoriasCSV(rutaArchivo);
            
            if (categorias.isEmpty()) {
                LogUtil.registrarAdvertencia("CARGAR_CATEGORIAS_CSV", "No se encontraron categorías válidas en el archivo");
                return 0;
            }
            
            // CREAR CATEGORÍAS UNA POR UNA - EVITAR DUPLICADOS
            int categoriasCreadas = 0;
            for (Categoria categoria : categorias) {
                try {
                    if (!categoriaDAO.existePorNombre(categoria.getNombre())) {
                        categoriaDAO.crear(categoria);
                        categoriasCreadas++;
                        logger.debug("Categoría creada: {}", categoria.getNombre());
                    } else {
                        logger.debug("Categoría ya existe, saltando: {}", categoria.getNombre());
                    }
                } catch (Exception e) {
                    logger.warn("Error al crear categoría {}: {}", categoria.getNombre(), e.getMessage());
                }
            }
            
            LogUtil.registrarOperacionExitosa("CARGAR_CATEGORIAS_CSV", 
                String.format("Creadas %d categorías nuevas de %d leídas desde %s", 
                    categoriasCreadas, categorias.size(), rutaArchivo));
            
            return categoriasCreadas;
            
        } catch (Exception e) {
            LogUtil.registrarError("CARGAR_CATEGORIAS_CSV", "Error al cargar categorías desde CSV", e);
            throw e;
        }
    }
    
    @Override
    public List<Categoria> obtenerTodasLasCategorias() throws Exception {
        try {
            List<Categoria> categorias = categoriaDAO.obtenerTodas();
            logger.debug("Obtenidas {} categorías", categorias.size());
            return categorias;
        } catch (Exception e) {
            LogUtil.registrarError("OBTENER_TODAS_CATEGORIAS", "Error al obtener todas las categorías", e);
            throw e;
        }
    }
    
    @Override
    public Categoria buscarCategoriaPorId(int id) throws Exception {
        try {
            Optional<Categoria> categoria = categoriaDAO.buscarPorId(id);
            if (categoria.isPresent()) {
                logger.debug("Categoría encontrada: ID {}", id);
                return categoria.get();
            } else {
                logger.debug("Categoría no encontrada: ID {}", id);
                return null;
            }
        } catch (Exception e) {
            LogUtil.registrarError("BUSCAR_CATEGORIA_ID", "Error al buscar categoría por ID: " + id, e);
            throw e;
        }
    }
    
    @Override
    public int crearCategoria(String nombre, String descripcion) throws Exception {
        try {
            // Verificar que no exista
            if (categoriaDAO.existePorNombre(nombre)) {
                throw new Exception("Ya existe una categoría con el nombre: " + nombre);
            }
            
            Categoria categoria = new Categoria(nombre, descripcion);
            
            if (!categoria.isValid()) {
                throw new Exception("Datos de la categoría inválidos");
            }
            
            int id = categoriaDAO.crear(categoria);
            
            LogUtil.registrarOperacionExitosa("CREAR_CATEGORIA", 
                String.format("Categoría creada: %s (ID: %d)", nombre, id));
            
            return id;
            
        } catch (Exception e) {
            LogUtil.registrarError("CREAR_CATEGORIA", "Error al crear categoría: " + nombre, e);
            throw e;
        }
    }
    
    @Override
    public boolean actualizarCategoria(Categoria categoria) throws Exception {
        try {
            if (!categoria.isValid()) {
                throw new Exception("Datos de la categoría inválidos");
            }
            
            boolean actualizada = categoriaDAO.actualizar(categoria);
            
            if (actualizada) {
                LogUtil.registrarOperacionExitosa("ACTUALIZAR_CATEGORIA", 
                    String.format("Categoría actualizada: %s (ID: %d)", 
                        categoria.getNombre(), categoria.getIdCategoria()));
            }
            
            return actualizada;
            
        } catch (Exception e) {
            LogUtil.registrarError("ACTUALIZAR_CATEGORIA", 
                "Error al actualizar categoría ID: " + categoria.getIdCategoria(), e);
            throw e;
        }
    }
    
    @Override
    public boolean eliminarCategoria(int id) throws Exception {
        try {
            // Verificar que no tenga productos asociados
            Optional<Categoria> categoria = categoriaDAO.buscarPorId(id);
            if (categoria.isPresent()) {
                int productosEnCategoria = productoDAO.contarPorCategoria(categoria.get().getNombre());
                if (productosEnCategoria > 0) {
                    throw new Exception("No se puede eliminar la categoría porque tiene " + 
                        productosEnCategoria + " productos asociados");
                }
            }
            
            boolean eliminada = categoriaDAO.eliminar(id);
            
            if (eliminada) {
                LogUtil.registrarOperacionExitosa("ELIMINAR_CATEGORIA", 
                    "Categoría eliminada ID: " + id);
            }
            
            return eliminada;
            
        } catch (Exception e) {
            LogUtil.registrarError("ELIMINAR_CATEGORIA", "Error al eliminar categoría ID: " + id, e);
            throw e;
        }
    }
    
    @Override
    public int contarTotalCategorias() throws Exception {
        try {
            return categoriaDAO.contarTotal();
        } catch (Exception e) {
            LogUtil.registrarError("CONTAR_CATEGORIAS", "Error al contar categorías", e);
            throw e;
        }
    }
    
    // === GESTIÓN DE STOCK ===
    
    @Override
    public boolean registrarEntradaStock(int idProducto, int cantidad, String motivo) throws Exception {
        try {
            if (cantidad <= 0) {
                throw new Exception("La cantidad debe ser mayor a cero");
            }
            
            // Obtener producto actual
            Optional<Producto> productoOpt = productoDAO.buscarPorId(idProducto);
            if (productoOpt.isEmpty()) {
                throw new Exception("Producto no encontrado ID: " + idProducto);
            }
            
            Producto producto = productoOpt.get();
            int stockAnterior = producto.getStock();
            int stockNuevo = stockAnterior + cantidad;
            
            // Actualizar stock
            boolean actualizado = productoDAO.actualizarStock(idProducto, stockNuevo);
            
            if (actualizado) {
                LogUtil.registrarOperacionExitosa("ENTRADA_STOCK", 
                    String.format("Entrada registrada - Producto: %s, Cantidad: %d, Stock: %d → %d, Motivo: %s", 
                        producto.getNombre(), cantidad, stockAnterior, stockNuevo, motivo));
            }
            
            return actualizado;
            
        } catch (Exception e) {
            LogUtil.registrarError("ENTRADA_STOCK", 
                String.format("Error al registrar entrada - Producto ID: %d, Cantidad: %d", idProducto, cantidad), e);
            throw e;
        }
    }
    
    @Override
    public boolean registrarSalidaStock(int idProducto, int cantidad, String motivo) throws Exception {
        try {
            if (cantidad <= 0) {
                throw new Exception("La cantidad debe ser mayor a cero");
            }
            
            // Obtener producto actual
            Optional<Producto> productoOpt = productoDAO.buscarPorId(idProducto);
            if (productoOpt.isEmpty()) {
                throw new Exception("Producto no encontrado ID: " + idProducto);
            }
            
            Producto producto = productoOpt.get();
            int stockAnterior = producto.getStock();
            
            if (stockAnterior < cantidad) {
                throw new Exception(String.format("Stock insuficiente. Disponible: %d, Solicitado: %d", 
                    stockAnterior, cantidad));
            }
            
            int stockNuevo = stockAnterior - cantidad;
            
            // Actualizar stock
            boolean actualizado = productoDAO.actualizarStock(idProducto, stockNuevo);
            
            if (actualizado) {
                LogUtil.registrarOperacionExitosa("SALIDA_STOCK", 
                    String.format("Salida registrada - Producto: %s, Cantidad: %d, Stock: %d → %d, Motivo: %s", 
                        producto.getNombre(), cantidad, stockAnterior, stockNuevo, motivo));
            }
            
            return actualizado;
            
        } catch (Exception e) {
            LogUtil.registrarError("SALIDA_STOCK", 
                String.format("Error al registrar salida - Producto ID: %d, Cantidad: %d", idProducto, cantidad), e);
            throw e;
        }
    }
    
    @Override
    public boolean actualizarStock(int idProducto, int nuevoStock) throws Exception {
        try {
            if (nuevoStock < 0) {
                throw new Exception("El stock no puede ser negativo");
            }
            
            boolean actualizado = productoDAO.actualizarStock(idProducto, nuevoStock);
            
            if (actualizado) {
                LogUtil.registrarOperacionExitosa("ACTUALIZAR_STOCK", 
                    String.format("Stock actualizado - Producto ID: %d, Nuevo stock: %d", idProducto, nuevoStock));
            }
            
            return actualizado;
            
        } catch (Exception e) {
            LogUtil.registrarError("ACTUALIZAR_STOCK", 
                String.format("Error al actualizar stock - Producto ID: %d, Stock: %d", idProducto, nuevoStock), e);
            throw e;
        }
    }
}