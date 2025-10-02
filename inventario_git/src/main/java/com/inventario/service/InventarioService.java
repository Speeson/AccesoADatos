package com.inventario.service;

import com.inventario.model.Categoria;
import com.inventario.model.Producto;
import java.util.List;

/**
 * Interfaz del servicio principal de inventario
 */
public interface InventarioService {
    
    // === GESTIÓN DE PRODUCTOS ===
    
    /**
     * Carga productos desde un archivo CSV
     */
    int cargarProductosDesdeCSV(String rutaArchivo) throws Exception;
    
    /**
     * Obtiene todos los productos
     */
    List<Producto> obtenerTodosLosProductos() throws Exception;
    
    /**
     * Busca un producto por ID
     */
    Producto buscarProductoPorId(int id) throws Exception;
    
    /**
     * Busca productos por categoría
     */
    List<Producto> buscarProductosPorCategoria(String categoria) throws Exception;
    
    /**
     * Obtiene productos con stock bajo
     */
    List<Producto> obtenerProductosConStockBajo(int limite) throws Exception;
    
    /**
     * Crea un nuevo producto
     */
    int crearProducto(String nombre, String categoria, double precio, int stock) throws Exception;
    
    /**
     * Actualiza un producto existente
     */
    boolean actualizarProducto(Producto producto) throws Exception;
    
    /**
     * Elimina un producto
     */
    boolean eliminarProducto(int id) throws Exception;
    
    /**
     * Cuenta el total de productos
     */
    int contarTotalProductos() throws Exception;
    
    // === GESTIÓN DE CATEGORÍAS ===
    
    /**
     * Carga categorías desde un archivo CSV
     */
    int cargarCategoriasDesdeCSV(String rutaArchivo) throws Exception;
    
    /**
     * Obtiene todas las categorías
     */
    List<Categoria> obtenerTodasLasCategorias() throws Exception;
    
    /**
     * Busca una categoría por ID
     */
    Categoria buscarCategoriaPorId(int id) throws Exception;
    
    /**
     * Crea una nueva categoría
     */
    int crearCategoria(String nombre, String descripcion) throws Exception;
    
    /**
     * Actualiza una categoría existente
     */
    boolean actualizarCategoria(Categoria categoria) throws Exception;
    
    /**
     * Elimina una categoría
     */
    boolean eliminarCategoria(int id) throws Exception;
    
    /**
     * Cuenta el total de categorías
     */
    int contarTotalCategorias() throws Exception;
    
    // === GESTIÓN DE STOCK ===
    
    /**
     * Registra una entrada de stock
     */
    boolean registrarEntradaStock(int idProducto, int cantidad, String motivo) throws Exception;
    
    /**
     * Registra una salida de stock
     */
    boolean registrarSalidaStock(int idProducto, int cantidad, String motivo) throws Exception;
    
    /**
     * Actualiza el stock de un producto
     */
    boolean actualizarStock(int idProducto, int nuevoStock) throws Exception;
}