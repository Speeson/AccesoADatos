package com.inventario.dao.impl;

import com.inventario.dao.MovimientoStockDAO;
import com.inventario.dao.ProductoDAO;
import com.inventario.model.MovimientoStock;
import com.inventario.model.Producto;
import com.inventario.util.DatabaseConfig;
import com.inventario.util.LogUtil;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para MovimientoStock
 */
public class MovimientoStockDAOImpl implements MovimientoStockDAO {
    private final DatabaseConfig dbConfig;
    private final ProductoDAO productoDAO;

    public MovimientoStockDAOImpl() {
        this.dbConfig = DatabaseConfig.getInstance();
        this.productoDAO = new ProductoDAOImpl();
    }

    @Override
    public int registrarMovimiento(MovimientoStock movimiento) throws SQLException {
        Connection conn = null;
        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            int id = registrarMovimiento(conn, movimiento);

            conn.commit(); // Confirmar transacción
            return id;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Revertir en caso de error
                } catch (SQLException ex) {
                    LogUtil.registrarError("ROLLBACK_MOVIMIENTO", "Error en rollback", ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LogUtil.registrarError("CLOSE_CONNECTION", "Error al cerrar conexión", e);
                }
            }
        }
    }

    @Override
    public int registrarMovimiento(Connection conn, MovimientoStock movimiento) throws SQLException {
        // 1. Validar que el movimiento sea válido
        if (!movimiento.isValid()) {
            throw new SQLException("Movimiento inválido: " + movimiento);
        }

        // 2. Obtener el producto y su stock actual
        Optional<Producto> productoOpt = productoDAO.buscarPorId(movimiento.getIdProducto());
        if (productoOpt.isEmpty()) {
            throw new SQLException("Producto no existe con ID: " + movimiento.getIdProducto());
        }

        Producto producto = productoOpt.get();
        int stockAnterior = producto.getStock();
        int stockNuevo;

        // 3. Calcular el nuevo stock según el tipo de movimiento
        if (movimiento.esEntrada()) {
            stockNuevo = stockAnterior + movimiento.getCantidad();
        } else if (movimiento.esSalida()) {
            if (stockAnterior < movimiento.getCantidad()) {
                throw new SQLException("Stock insuficiente. Disponible: " + stockAnterior +
                                     ", Solicitado: " + movimiento.getCantidad());
            }
            stockNuevo = stockAnterior - movimiento.getCantidad();
        } else {
            throw new SQLException("Tipo de movimiento inválido: " + movimiento.getTipoMovimiento());
        }

        // 4. Establecer los valores calculados en el movimiento
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);

        // 5. Insertar el movimiento en la base de datos
        String sqlInsert = "INSERT INTO movimientos_stock " +
                          "(id_producto, tipo_movimiento, cantidad, stock_anterior, stock_nuevo, motivo, usuario) " +
                          "VALUES (?, ?, ?, ?, ?, ?, ?)";

        int idGenerado;
        try (PreparedStatement stmt = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, movimiento.getIdProducto());
            stmt.setString(2, movimiento.getTipoMovimiento());
            stmt.setInt(3, movimiento.getCantidad());
            stmt.setInt(4, movimiento.getStockAnterior());
            stmt.setInt(5, movimiento.getStockNuevo());
            stmt.setString(6, movimiento.getMotivo());
            stmt.setString(7, movimiento.getUsuario());

            int filasAfectadas = stmt.executeUpdate();

            if (filasAfectadas == 0) {
                throw new SQLException("No se pudo insertar el movimiento");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    idGenerado = generatedKeys.getInt(1);
                    movimiento.setIdMovimiento(idGenerado);
                } else {
                    throw new SQLException("No se pudo obtener el ID generado del movimiento");
                }
            }
        }

        // 6. Actualizar el stock del producto
        String sqlUpdate = "UPDATE productos SET stock = ? WHERE id_producto = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
            stmt.setInt(1, stockNuevo);
            stmt.setInt(2, movimiento.getIdProducto());

            int filasActualizadas = stmt.executeUpdate();
            if (filasActualizadas == 0) {
                throw new SQLException("No se pudo actualizar el stock del producto");
            }
        }

        LogUtil.registrarOperacionExitosa("REGISTRAR_MOVIMIENTO",
            String.format("Movimiento registrado: ID=%d, Producto=%d, Tipo=%s, Cantidad=%d, Stock: %d -> %d",
                idGenerado, movimiento.getIdProducto(), movimiento.getTipoMovimiento(),
                movimiento.getCantidad(), stockAnterior, stockNuevo));

        return idGenerado;
    }

    @Override
    public Optional<MovimientoStock> buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_movimiento, id_producto, tipo_movimiento, cantidad, " +
                    "stock_anterior, stock_nuevo, motivo, fecha_movimiento, usuario " +
                    "FROM movimientos_stock WHERE id_movimiento = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearMovimiento(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            LogUtil.registrarError("BUSCAR_MOVIMIENTO_ID", "Error al buscar movimiento por ID: " + id, e);
            throw e;
        }
    }

    @Override
    public List<MovimientoStock> obtenerPorProducto(int idProducto) throws SQLException {
        String sql = "SELECT id_movimiento, id_producto, tipo_movimiento, cantidad, " +
                    "stock_anterior, stock_nuevo, motivo, fecha_movimiento, usuario " +
                    "FROM movimientos_stock WHERE id_producto = ? " +
                    "ORDER BY fecha_movimiento DESC";

        return ejecutarConsultaLista(sql, idProducto);
    }

    @Override
    public List<MovimientoStock> obtenerPorTipo(String tipo) throws SQLException {
        String sql = "SELECT id_movimiento, id_producto, tipo_movimiento, cantidad, " +
                    "stock_anterior, stock_nuevo, motivo, fecha_movimiento, usuario " +
                    "FROM movimientos_stock WHERE tipo_movimiento = ? " +
                    "ORDER BY fecha_movimiento DESC";

        return ejecutarConsultaLista(sql, tipo);
    }

    @Override
    public List<MovimientoStock> obtenerPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) throws SQLException {
        String sql = "SELECT id_movimiento, id_producto, tipo_movimiento, cantidad, " +
                    "stock_anterior, stock_nuevo, motivo, fecha_movimiento, usuario " +
                    "FROM movimientos_stock " +
                    "WHERE fecha_movimiento BETWEEN ? AND ? " +
                    "ORDER BY fecha_movimiento DESC";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(inicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fin));

            try (ResultSet rs = stmt.executeQuery()) {
                List<MovimientoStock> movimientos = new ArrayList<>();
                while (rs.next()) {
                    movimientos.add(mapearMovimiento(rs));
                }
                return movimientos;
            }

        } catch (SQLException e) {
            LogUtil.registrarError("OBTENER_MOVIMIENTOS_RANGO", "Error al obtener movimientos por rango de fechas", e);
            throw e;
        }
    }

    @Override
    public List<MovimientoStock> obtenerTodos() throws SQLException {
        String sql = "SELECT id_movimiento, id_producto, tipo_movimiento, cantidad, " +
                    "stock_anterior, stock_nuevo, motivo, fecha_movimiento, usuario " +
                    "FROM movimientos_stock " +
                    "ORDER BY fecha_movimiento DESC";

        return ejecutarConsultaLista(sql);
    }

    @Override
    public List<MovimientoStock> obtenerUltimos(int limite) throws SQLException {
        String sql = "SELECT id_movimiento, id_producto, tipo_movimiento, cantidad, " +
                    "stock_anterior, stock_nuevo, motivo, fecha_movimiento, usuario " +
                    "FROM movimientos_stock " +
                    "ORDER BY fecha_movimiento DESC " +
                    "LIMIT ?";

        return ejecutarConsultaLista(sql, limite);
    }

    @Override
    public int contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM movimientos_stock";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;

        } catch (SQLException e) {
            LogUtil.registrarError("CONTAR_MOVIMIENTOS", "Error al contar movimientos", e);
            throw e;
        }
    }

    @Override
    public int contarPorTipo(String tipo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM movimientos_stock WHERE tipo_movimiento = ?";

        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tipo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            LogUtil.registrarError("CONTAR_MOVIMIENTOS_TIPO", "Error al contar movimientos por tipo: " + tipo, e);
            throw e;
        }
    }

    @Override
    public int registrarMovimientosLote(List<MovimientoStock> movimientos) throws SQLException {
        if (movimientos == null || movimientos.isEmpty()) {
            return 0;
        }

        Connection conn = null;
        int procesados = 0;

        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            for (MovimientoStock movimiento : movimientos) {
                registrarMovimiento(conn, movimiento);
                procesados++;
            }

            conn.commit(); // Confirmar todos los movimientos
            LogUtil.registrarOperacionExitosa("REGISTRAR_LOTE",
                "Lote de " + procesados + " movimientos registrado exitosamente");

            return procesados;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Revertir TODOS los movimientos del lote
                    LogUtil.registrarError("ROLLBACK_LOTE",
                        "Rollback realizado. Movimientos revertidos: " + procesados, e);
                } catch (SQLException ex) {
                    LogUtil.registrarError("ROLLBACK_LOTE", "Error en rollback", ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LogUtil.registrarError("CLOSE_CONNECTION", "Error al cerrar conexión", e);
                }
            }
        }
    }

    // Métodos auxiliares privados

    private MovimientoStock mapearMovimiento(ResultSet rs) throws SQLException {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setIdMovimiento(rs.getInt("id_movimiento"));
        movimiento.setIdProducto(rs.getInt("id_producto"));
        movimiento.setTipoMovimiento(rs.getString("tipo_movimiento"));
        movimiento.setCantidad(rs.getInt("cantidad"));
        movimiento.setStockAnterior(rs.getInt("stock_anterior"));
        movimiento.setStockNuevo(rs.getInt("stock_nuevo"));
        movimiento.setMotivo(rs.getString("motivo"));

        Timestamp timestamp = rs.getTimestamp("fecha_movimiento");
        if (timestamp != null) {
            movimiento.setFechaMovimiento(timestamp.toLocalDateTime());
        }

        movimiento.setUsuario(rs.getString("usuario"));
        return movimiento;
    }

    private List<MovimientoStock> ejecutarConsultaLista(String sql, Object... parametros) throws SQLException {
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                stmt.setObject(i + 1, parametros[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<MovimientoStock> movimientos = new ArrayList<>();
                while (rs.next()) {
                    movimientos.add(mapearMovimiento(rs));
                }
                return movimientos;
            }

        } catch (SQLException e) {
            LogUtil.registrarError("EJECUTAR_CONSULTA", "Error al ejecutar consulta", e);
            throw e;
        }
    }
}
