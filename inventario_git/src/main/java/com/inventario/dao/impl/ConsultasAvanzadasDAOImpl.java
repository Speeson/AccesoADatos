package com.inventario.dao.impl;

import com.inventario.dao.ConsultasAvanzadasDAO;
import com.inventario.util.DatabaseConfig;
import com.inventario.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de consultas avanzadas SQL
 * Fase II - Actividad Evaluable 2
 *
 * Incluye:
 * - Top N productos más vendidos
 * - Valor total de stock por categoría
 * - Histórico de movimientos por rango de fechas
 * - Análisis de rotación de inventario
 */
public class ConsultasAvanzadasDAOImpl implements ConsultasAvanzadasDAO {
    private static final Logger logger = LoggerFactory.getLogger(ConsultasAvanzadasDAOImpl.class);
    private final DatabaseConfig dbConfig;

    public ConsultasAvanzadasDAOImpl() {
        this.dbConfig = DatabaseConfig.getInstance();
    }

    /**
     * Top N productos más vendidos
     * Consulta avanzada con JOIN y agregaciones
     */
    @Override
    public List<Object[]> obtenerTopProductosMasVendidos(int limite) throws SQLException {
        String sql = """
            SELECT
                p.id_producto,
                p.nombre,
                p.categoria,
                p.precio,
                p.stock as stock_actual,
                SUM(m.cantidad) as total_vendido,
                COUNT(m.id_movimiento) as num_transacciones,
                SUM(m.cantidad * p.precio) as ingresos_generados
            FROM productos p
            INNER JOIN movimientos_stock m ON p.id_producto = m.id_producto
            WHERE m.tipo_movimiento = 'SALIDA'
            GROUP BY p.id_producto, p.nombre, p.categoria, p.precio, p.stock
            ORDER BY total_vendido DESC
            LIMIT ?
            """;

        List<Object[]> resultados = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limite);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = new Object[8];
                    fila[0] = rs.getInt("id_producto");
                    fila[1] = rs.getString("nombre");
                    fila[2] = rs.getString("categoria");
                    fila[3] = rs.getBigDecimal("precio");
                    fila[4] = rs.getInt("stock_actual");
                    fila[5] = rs.getInt("total_vendido");
                    fila[6] = rs.getInt("num_transacciones");
                    fila[7] = rs.getBigDecimal("ingresos_generados");
                    resultados.add(fila);
                }
            }

            logger.info("Obtenidos top {} productos más vendidos", limite);
            return resultados;

        } catch (SQLException e) {
            LogUtil.registrarError("TOP_PRODUCTOS_VENDIDOS",
                    "Error al obtener top productos más vendidos", e);
            throw e;
        }
    }

    /**
     * Valor total de stock por categoría
     * Consulta con GROUP BY y múltiples agregaciones
     */
    @Override
    public List<Object[]> obtenerValorStockPorCategoria() throws SQLException {
        String sql = """
            SELECT
                categoria,
                COUNT(*) as total_productos,
                SUM(stock) as unidades_stock,
                MIN(precio) as precio_minimo,
                MAX(precio) as precio_maximo,
                AVG(precio) as precio_promedio,
                SUM(precio * stock) as valor_total_stock
            FROM productos
            GROUP BY categoria
            ORDER BY valor_total_stock DESC
            """;

        List<Object[]> resultados = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Object[] fila = new Object[7];
                fila[0] = rs.getString("categoria");
                fila[1] = rs.getInt("total_productos");
                fila[2] = rs.getInt("unidades_stock");
                fila[3] = rs.getBigDecimal("precio_minimo");
                fila[4] = rs.getBigDecimal("precio_maximo");
                fila[5] = rs.getBigDecimal("precio_promedio");
                fila[6] = rs.getBigDecimal("valor_total_stock");
                resultados.add(fila);
            }

            logger.info("Obtenido valor de stock para {} categorías", resultados.size());
            return resultados;

        } catch (SQLException e) {
            LogUtil.registrarError("VALOR_STOCK_CATEGORIA",
                    "Error al obtener valor de stock por categoría", e);
            throw e;
        }
    }

    /**
     * Histórico de movimientos por rango de fechas
     * Consulta con JOIN y filtros temporales
     */
    @Override
    public List<Object[]> obtenerHistoricoMovimientos(LocalDateTime fechaInicio, LocalDateTime fechaFin) throws SQLException {
        String sql = """
            SELECT
                m.id_movimiento,
                m.fecha_movimiento,
                p.id_producto,
                p.nombre as producto,
                p.categoria,
                m.tipo_movimiento,
                m.cantidad,
                m.stock_anterior,
                m.stock_nuevo,
                m.motivo,
                m.usuario,
                p.precio,
                (m.cantidad * p.precio) as valor_movimiento
            FROM movimientos_stock m
            INNER JOIN productos p ON m.id_producto = p.id_producto
            WHERE m.fecha_movimiento BETWEEN ? AND ?
            ORDER BY m.fecha_movimiento DESC
            """;

        List<Object[]> resultados = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(fechaInicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fechaFin));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = new Object[13];
                    fila[0] = rs.getInt("id_movimiento");
                    fila[1] = rs.getTimestamp("fecha_movimiento").toLocalDateTime();
                    fila[2] = rs.getInt("id_producto");
                    fila[3] = rs.getString("producto");
                    fila[4] = rs.getString("categoria");
                    fila[5] = rs.getString("tipo_movimiento");
                    fila[6] = rs.getInt("cantidad");
                    fila[7] = rs.getInt("stock_anterior");
                    fila[8] = rs.getInt("stock_nuevo");
                    fila[9] = rs.getString("motivo");
                    fila[10] = rs.getString("usuario");
                    fila[11] = rs.getBigDecimal("precio");
                    fila[12] = rs.getBigDecimal("valor_movimiento");
                    resultados.add(fila);
                }
            }

            logger.info("Obtenidos {} movimientos entre {} y {}",
                    resultados.size(), fechaInicio, fechaFin);
            return resultados;

        } catch (SQLException e) {
            LogUtil.registrarError("HISTORICO_MOVIMIENTOS",
                    "Error al obtener histórico de movimientos", e);
            throw e;
        }
    }

    /**
     * Productos con bajo stock y su histórico reciente
     * Consulta compleja con subconsultas
     */
    @Override
    public List<Object[]> obtenerProductosBajoStockConHistorico(int limiteStock, int diasHistorico) throws SQLException {
        String sql = """
            SELECT
                p.id_producto,
                p.nombre,
                p.categoria,
                p.stock,
                p.precio,
                COUNT(DISTINCT m.id_movimiento) as movimientos_recientes,
                COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'ENTRADA' THEN m.cantidad ELSE 0 END), 0) as entradas_recientes,
                COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) as salidas_recientes,
                MAX(m.fecha_movimiento) as ultimo_movimiento
            FROM productos p
            LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
                AND m.fecha_movimiento >= DATE_SUB(NOW(), INTERVAL ? DAY)
            WHERE p.stock < ?
            GROUP BY p.id_producto, p.nombre, p.categoria, p.stock, p.precio
            ORDER BY p.stock ASC, salidas_recientes DESC
            """;

        List<Object[]> resultados = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, diasHistorico);
            stmt.setInt(2, limiteStock);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = new Object[9];
                    fila[0] = rs.getInt("id_producto");
                    fila[1] = rs.getString("nombre");
                    fila[2] = rs.getString("categoria");
                    fila[3] = rs.getInt("stock");
                    fila[4] = rs.getBigDecimal("precio");
                    fila[5] = rs.getInt("movimientos_recientes");
                    fila[6] = rs.getInt("entradas_recientes");
                    fila[7] = rs.getInt("salidas_recientes");

                    Timestamp ultimoMov = rs.getTimestamp("ultimo_movimiento");
                    fila[8] = ultimoMov != null ? ultimoMov.toLocalDateTime() : null;

                    resultados.add(fila);
                }
            }

            logger.info("Obtenidos {} productos con stock bajo y su histórico", resultados.size());
            return resultados;

        } catch (SQLException e) {
            LogUtil.registrarError("BAJO_STOCK_HISTORICO",
                    "Error al obtener productos bajo stock con histórico", e);
            throw e;
        }
    }

    /**
     * Productos sin movimientos en período dado
     * Consulta con LEFT JOIN y filtro de nulos
     */
    @Override
    public List<Object[]> obtenerProductosSinMovimientos(int diasSinMovimiento) throws SQLException {
        String sql = """
            SELECT
                p.id_producto,
                p.nombre,
                p.categoria,
                p.stock,
                p.precio,
                p.fecha_creacion,
                MAX(m.fecha_movimiento) as ultimo_movimiento,
                DATEDIFF(NOW(), COALESCE(MAX(m.fecha_movimiento), p.fecha_creacion)) as dias_sin_actividad
            FROM productos p
            LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
            GROUP BY p.id_producto, p.nombre, p.categoria, p.stock, p.precio, p.fecha_creacion
            HAVING dias_sin_actividad >= ?
            ORDER BY dias_sin_actividad DESC
            """;

        List<Object[]> resultados = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, diasSinMovimiento);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] fila = new Object[8];
                    fila[0] = rs.getInt("id_producto");
                    fila[1] = rs.getString("nombre");
                    fila[2] = rs.getString("categoria");
                    fila[3] = rs.getInt("stock");
                    fila[4] = rs.getBigDecimal("precio");
                    fila[5] = rs.getTimestamp("fecha_creacion").toLocalDateTime();

                    Timestamp ultimoMov = rs.getTimestamp("ultimo_movimiento");
                    fila[6] = ultimoMov != null ? ultimoMov.toLocalDateTime() : null;
                    fila[7] = rs.getInt("dias_sin_actividad");

                    resultados.add(fila);
                }
            }

            logger.info("Obtenidos {} productos sin movimientos en {} días",
                    resultados.size(), diasSinMovimiento);
            return resultados;

        } catch (SQLException e) {
            LogUtil.registrarError("PRODUCTOS_SIN_MOVIMIENTOS",
                    "Error al obtener productos sin movimientos", e);
            throw e;
        }
    }

    /**
     * Análisis de rotación de inventario por categoría
     * Consulta compleja con múltiples agregaciones y cálculos
     */
    @Override
    public List<Object[]> obtenerAnalisisRotacionPorCategoria() throws SQLException {
        String sql = """
            SELECT
                p.categoria,
                COUNT(DISTINCT p.id_producto) as total_productos,
                SUM(p.stock) as stock_total,
                SUM(p.precio * p.stock) as valor_inventario,
                COUNT(DISTINCT CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.id_movimiento END) as total_ventas,
                COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) as unidades_vendidas,
                COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'ENTRADA' THEN m.cantidad ELSE 0 END), 0) as unidades_compradas,
                ROUND(
                    COALESCE(SUM(CASE WHEN m.tipo_movimiento = 'SALIDA' THEN m.cantidad ELSE 0 END), 0) /
                    NULLIF(AVG(p.stock), 0),
                2) as indice_rotacion
            FROM productos p
            LEFT JOIN movimientos_stock m ON p.id_producto = m.id_producto
                AND m.fecha_movimiento >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            GROUP BY p.categoria
            ORDER BY indice_rotacion DESC
            """;

        List<Object[]> resultados = new ArrayList<>();

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Object[] fila = new Object[8];
                fila[0] = rs.getString("categoria");
                fila[1] = rs.getInt("total_productos");
                fila[2] = rs.getInt("stock_total");
                fila[3] = rs.getBigDecimal("valor_inventario");
                fila[4] = rs.getInt("total_ventas");
                fila[5] = rs.getInt("unidades_vendidas");
                fila[6] = rs.getInt("unidades_compradas");
                fila[7] = rs.getBigDecimal("indice_rotacion");
                resultados.add(fila);
            }

            logger.info("Análisis de rotación obtenido para {} categorías", resultados.size());
            return resultados;

        } catch (SQLException e) {
            LogUtil.registrarError("ANALISIS_ROTACION",
                    "Error al obtener análisis de rotación", e);
            throw e;
        }
    }
}
