package com.inventario.dao;

import com.inventario.model.MovimientoStock;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz DAO para operaciones CRUD de MovimientoStock
 */
public interface MovimientoStockDAO {

    /**
     * Registra un nuevo movimiento de stock
     * Actualiza automáticamente el stock del producto
     */
    int registrarMovimiento(MovimientoStock movimiento) throws SQLException;

    /**
     * Registra un movimiento usando una conexión existente (para transacciones)
     */
    int registrarMovimiento(Connection conn, MovimientoStock movimiento) throws SQLException;

    /**
     * Busca un movimiento por ID
     */
    Optional<MovimientoStock> buscarPorId(int id) throws SQLException;

    /**
     * Obtiene todos los movimientos de un producto
     */
    List<MovimientoStock> obtenerPorProducto(int idProducto) throws SQLException;

    /**
     * Obtiene movimientos por tipo (ENTRADA o SALIDA)
     */
    List<MovimientoStock> obtenerPorTipo(String tipo) throws SQLException;

    /**
     * Obtiene movimientos en un rango de fechas
     */
    List<MovimientoStock> obtenerPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) throws SQLException;

    /**
     * Obtiene todos los movimientos
     */
    List<MovimientoStock> obtenerTodos() throws SQLException;

    /**
     * Obtiene los últimos N movimientos
     */
    List<MovimientoStock> obtenerUltimos(int limite) throws SQLException;

    /**
     * Cuenta el total de movimientos
     */
    int contarTotal() throws SQLException;

    /**
     * Cuenta movimientos por tipo
     */
    int contarPorTipo(String tipo) throws SQLException;

    /**
     * Registra múltiples movimientos en una transacción atómica
     * Si uno falla, se hace rollback de todos
     */
    int registrarMovimientosLote(List<MovimientoStock> movimientos) throws SQLException;
}
