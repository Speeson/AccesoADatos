package com.inventario.dao;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface para consultas avanzadas SQL
 * Fase II - Actividad Evaluable 2
 */
public interface ConsultasAvanzadasDAO {

    /**
     * Obtiene los N productos más vendidos (basado en movimientos de tipo SALIDA)
     *
     * @param limite Número de productos a retornar (Top N)
     * @return Lista de arrays con: [id_producto, nombre, categoria, total_vendido, num_transacciones]
     * @throws SQLException Si hay error en la consulta
     */
    List<Object[]> obtenerTopProductosMasVendidos(int limite) throws SQLException;

    /**
     * Calcula el valor total de stock agrupado por categoría
     *
     * @return Lista de arrays con: [categoria, total_productos, unidades_stock, valor_total_stock]
     * @throws SQLException Si hay error en la consulta
     */
    List<Object[]> obtenerValorStockPorCategoria() throws SQLException;

    /**
     * Obtiene el histórico de movimientos en un rango de fechas
     *
     * @param fechaInicio Fecha de inicio del rango
     * @param fechaFin Fecha de fin del rango
     * @return Lista de arrays con información completa de movimientos
     * @throws SQLException Si hay error en la consulta
     */
    List<Object[]> obtenerHistoricoMovimientos(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException;

    /**
     * Obtiene productos con bajo stock y su histórico de movimientos recientes
     *
     * @param limiteStock Umbral de stock bajo
     * @param diasHistorico Días hacia atrás para el histórico
     * @return Lista de arrays con productos y su actividad reciente
     * @throws SQLException Si hay error en la consulta
     */
    List<Object[]> obtenerProductosBajoStockConHistorico(int limiteStock, int diasHistorico) throws SQLException;

    /**
     * Obtiene productos sin movimientos en un período dado
     *
     * @param diasSinMovimiento Días sin actividad
     * @return Lista de productos sin movimientos
     * @throws SQLException Si hay error en la consulta
     */
    List<Object[]> obtenerProductosSinMovimientos(int diasSinMovimiento) throws SQLException;

    /**
     * Análisis de rotación de inventario por categoría
     *
     * @return Lista con métricas de rotación por categoría
     * @throws SQLException Si hay error en la consulta
     */
    List<Object[]> obtenerAnalisisRotacionPorCategoria() throws SQLException;
}
