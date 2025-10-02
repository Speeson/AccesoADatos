package com.inventario.dao;

import com.inventario.model.Categoria;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para operaciones CRUD de Categoria
 */
public interface CategoriaDAO {
    
    /**
     * Crea una nueva categoría
     */
    int crear(Categoria categoria) throws SQLException;
    
    /**
     * Busca una categoría por ID
     */
    Optional<Categoria> buscarPorId(int id) throws SQLException;
    
    /**
     * Busca una categoría por nombre
     */
    Optional<Categoria> buscarPorNombre(String nombre) throws SQLException;
    
    /**
     * Obtiene todas las categorías
     */
    List<Categoria> obtenerTodas() throws SQLException;
    
    /**
     * Actualiza una categoría existente
     */
    boolean actualizar(Categoria categoria) throws SQLException;
    
    /**
     * Elimina una categoría por ID
     */
    boolean eliminar(int id) throws SQLException;
    
    /**
     * Verifica si existe una categoría con el nombre dado
     */
    boolean existePorNombre(String nombre) throws SQLException;
    
    /**
     * Cuenta el total de categorías
     */
    int contarTotal() throws SQLException;
    
    /**
     * Crea múltiples categorías en una transacción
     */
    int crearMultiples(List<Categoria> categorias) throws SQLException;
}