package com.inventario.dao;

import com.inventario.model.Producto;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para operaciones CRUD de Producto
 */
public interface ProductoDAO {
    
    /**
     * Crea un nuevo producto
     */
    int crear(Producto producto) throws SQLException;
    
    /**
     * Busca un producto por ID
     */
    Optional<Producto> buscarPorId(int id) throws SQLException;
    
    /**
     * Busca productos por nombre (búsqueda parcial)
     */
    List<Producto> buscarPorNombre(String nombre) throws SQLException;
    
    /**
     * Busca productos por categoría
     */
    List<Producto> buscarPorCategoria(String categoria) throws SQLException;
    
    /**
     * Obtiene todos los productos
     */
    List<Producto> obtenerTodos() throws SQLException;
    
    /**
     * Obtiene productos con stock bajo (menor al límite especificado)
     */
    List<Producto> obtenerConStockBajo(int limite) throws SQLException;
    
    /**
     * Actualiza un producto existente
     */
    boolean actualizar(Producto producto) throws SQLException;
    
    /**
     * Actualiza solo el stock de un producto
     */
    boolean actualizarStock(int idProducto, int nuevoStock) throws SQLException;
    
    /**
     * Elimina un producto por ID
     */
    boolean eliminar(int id) throws SQLException;
    
    /**
     * Verifica si existe un producto con el nombre dado
     */
    boolean existePorNombre(String nombre) throws SQLException;
    
    /**
     * Cuenta el total de productos
     */
    int contarTotal() throws SQLException;
    
    /**
     * Cuenta productos por categoría
     */
    int contarPorCategoria(String categoria) throws SQLException;
    
    /**
     * Crea múltiples productos en una transacción
     */
    int crearMultiples(List<Producto> productos) throws SQLException;
    
    /**
     * Obtiene el valor total del inventario
     */
    BigDecimal obtenerValorTotalInventario() throws SQLException;
    
    /**
     * Obtiene estadísticas por categoría
     */
    List<Object[]> obtenerEstadisticasPorCategoria() throws SQLException;
}