package com.inventario.dao.impl;

import com.inventario.dao.ProductoDAO;
import com.inventario.model.Producto;
import com.inventario.util.DatabaseConfig;
import com.inventario.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del DAO para Producto
 */
public class ProductoDAOImpl implements ProductoDAO {
    private static final Logger logger = LoggerFactory.getLogger(ProductoDAOImpl.class);
    private final DatabaseConfig dbConfig;
    
    public ProductoDAOImpl() {
        this.dbConfig = DatabaseConfig.getInstance();
    }
    
    @Override
    public int crear(Producto producto) throws SQLException {
        String sql = "INSERT INTO productos (nombre, categoria, precio, stock) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getCategoria());
            stmt.setBigDecimal(3, producto.getPrecio());
            stmt.setInt(4, producto.getStock());
            
            int filasAfectadas = stmt.executeUpdate();
            
            if (filasAfectadas > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        producto.setIdProducto(id);
                        LogUtil.registrarOperacionExitosa("CREAR_PRODUCTO", 
                            "Producto creado: " + producto.getNombre() + " (ID: " + id + ")");
                        return id;
                    }
                }
            }
            
            throw new SQLException("No se pudo crear el producto, no se generó ID");
            
        } catch (SQLException e) {
            LogUtil.registrarError("CREAR_PRODUCTO", "Error al crear producto: " + producto.getNombre(), e);
            throw e;
        }
    }
    
    @Override
    public Optional<Producto> buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion " +
                    "FROM productos WHERE id_producto = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearProducto(rs));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            LogUtil.registrarError("BUSCAR_PRODUCTO_ID", "Error al buscar producto por ID: " + id, e);
            throw e;
        }
    }
    
    @Override
    public List<Producto> buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion " +
                    "FROM productos WHERE nombre LIKE ? ORDER BY nombre";
        
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + nombre + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapearProducto(rs));
                }
            }
            
            return productos;
            
        } catch (SQLException e) {
            LogUtil.registrarError("BUSCAR_PRODUCTO_NOMBRE", "Error al buscar productos por nombre: " + nombre, e);
            throw e;
        }
    }
    
    @Override
    public List<Producto> buscarPorCategoria(String categoria) throws SQLException {
        String sql = "SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion " +
                    "FROM productos WHERE categoria = ? ORDER BY nombre";
        
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, categoria);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapearProducto(rs));
                }
            }
            
            logger.debug("Encontrados {} productos en categoría: {}", productos.size(), categoria);
            return productos;
            
        } catch (SQLException e) {
            LogUtil.registrarError("BUSCAR_PRODUCTOS_CATEGORIA", 
                "Error al buscar productos por categoría: " + categoria, e);
            throw e;
        }
    }
    
    @Override
    public List<Producto> obtenerTodos() throws SQLException {
        String sql = "SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion " +
                    "FROM productos ORDER BY nombre";
        
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }
            
            logger.debug("Obtenidos {} productos", productos.size());
            return productos;
            
        } catch (SQLException e) {
            LogUtil.registrarError("OBTENER_TODOS_PRODUCTOS", "Error al obtener todos los productos", e);
            throw e;
        }
    }
    
    @Override
    public List<Producto> obtenerConStockBajo(int limite) throws SQLException {
        String sql = "SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion " +
                    "FROM productos WHERE stock < ? ORDER BY stock ASC";
        
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limite);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapearProducto(rs));
                }
            }
            
            logger.debug("Encontrados {} productos con stock bajo (< {})", productos.size(), limite);
            return productos;
            
        } catch (SQLException e) {
            LogUtil.registrarError("OBTENER_STOCK_BAJO", "Error al obtener productos con stock bajo", e);
            throw e;
        }
    }
    
    public List<Producto> obtenerPorRangoPrecio(BigDecimal precioMin, BigDecimal precioMax) throws SQLException {
        String sql = "SELECT id_producto, nombre, categoria, precio, stock, fecha_creacion, fecha_modificacion " +
                    "FROM productos WHERE precio BETWEEN ? AND ? ORDER BY precio";
        
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, precioMin);
            stmt.setBigDecimal(2, precioMax);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapearProducto(rs));
                }
            }
            
            return productos;
            
        } catch (SQLException e) {
            LogUtil.registrarError("OBTENER_POR_RANGO_PRECIO", 
                "Error al obtener productos por rango de precio", e);
            throw e;
        }
    }
    
    @Override
    public boolean actualizar(Producto producto) throws SQLException {
        String sql = "UPDATE productos SET nombre = ?, categoria = ?, precio = ?, stock = ? WHERE id_producto = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getCategoria());
            stmt.setBigDecimal(3, producto.getPrecio());
            stmt.setInt(4, producto.getStock());
            stmt.setInt(5, producto.getIdProducto());
            
            int filasAfectadas = stmt.executeUpdate();
            
            if (filasAfectadas > 0) {
                LogUtil.registrarOperacionExitosa("ACTUALIZAR_PRODUCTO", 
                    "Producto actualizado: " + producto.getNombre() + " (ID: " + producto.getIdProducto() + ")");
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            LogUtil.registrarError("ACTUALIZAR_PRODUCTO", 
                "Error al actualizar producto: " + producto.getNombre(), e);
            throw e;
        }
    }
    
    @Override
    public boolean actualizarStock(int idProducto, int nuevoStock) throws SQLException {
        String sql = "UPDATE productos SET stock = ? WHERE id_producto = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nuevoStock);
            stmt.setInt(2, idProducto);
            
            int filasAfectadas = stmt.executeUpdate();
            
            if (filasAfectadas > 0) {
                LogUtil.registrarOperacionExitosa("ACTUALIZAR_STOCK", 
                    "Stock actualizado para producto ID: " + idProducto + " -> " + nuevoStock);
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            LogUtil.registrarError("ACTUALIZAR_STOCK", 
                "Error al actualizar stock del producto ID: " + idProducto, e);
            throw e;
        }
    }
    
    @Override
    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM productos WHERE id_producto = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            int filasAfectadas = stmt.executeUpdate();
            
            if (filasAfectadas > 0) {
                LogUtil.registrarOperacionExitosa("ELIMINAR_PRODUCTO", "Producto eliminado (ID: " + id + ")");
                return true;
            }
            
            return false;
            
        } catch (SQLException e) {
            LogUtil.registrarError("ELIMINAR_PRODUCTO", "Error al eliminar producto (ID: " + id + ")", e);
            throw e;
        }
    }
    
    @Override
    public boolean existePorNombre(String nombre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM productos WHERE nombre = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nombre);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
            
        } catch (SQLException e) {
            LogUtil.registrarError("EXISTE_PRODUCTO_NOMBRE", 
                "Error al verificar existencia de producto: " + nombre, e);
            throw e;
        }
    }
    
    @Override
    public int contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM productos";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
            
        } catch (SQLException e) {
            LogUtil.registrarError("CONTAR_PRODUCTOS", "Error al contar productos", e);
            throw e;
        }
    }
    
    @Override
    public int contarPorCategoria(String categoria) throws SQLException {
        String sql = "SELECT COUNT(*) FROM productos WHERE categoria = ?";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, categoria);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
            
        } catch (SQLException e) {
            LogUtil.registrarError("CONTAR_PRODUCTOS_CATEGORIA", 
                "Error al contar productos por categoría: " + categoria, e);
            throw e;
        }
    }
    
    @Override
    public int crearMultiples(List<Producto> productos) throws SQLException {
        String sql = "INSERT INTO productos (nombre, categoria, precio, stock) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        
        try {
            conn = dbConfig.getConnectionForTransaction();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (Producto producto : productos) {
                    stmt.setString(1, producto.getNombre());
                    stmt.setString(2, producto.getCategoria());
                    stmt.setBigDecimal(3, producto.getPrecio());
                    stmt.setInt(4, producto.getStock());
                    stmt.addBatch();
                }
                
                int[] resultados = stmt.executeBatch();
                dbConfig.commit(conn);
                
                int totalCreados = 0;
                for (int resultado : resultados) {
                    if (resultado > 0) totalCreados++;
                }
                
                LogUtil.registrarOperacionExitosa("CREAR_MULTIPLES_PRODUCTOS", 
                    "Creados " + totalCreados + " de " + productos.size() + " productos");
                
                return totalCreados;
                
            } catch (SQLException e) {
                dbConfig.rollback(conn);
                throw e;
            }
            
        } catch (SQLException e) {
            LogUtil.registrarError("CREAR_MULTIPLES_PRODUCTOS", 
                "Error al crear múltiples productos", e);
            throw e;
        } finally {
            dbConfig.closeConnection(conn);
        }
    }
    
    @Override
    public BigDecimal obtenerValorTotalInventario() throws SQLException {
        String sql = "SELECT SUM(precio * stock) as valor_total FROM productos";
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                BigDecimal valorTotal = rs.getBigDecimal("valor_total");
                return valorTotal != null ? valorTotal : BigDecimal.ZERO;
            }
            return BigDecimal.ZERO;
            
        } catch (SQLException e) {
            LogUtil.registrarError("OBTENER_VALOR_TOTAL", "Error al calcular valor total del inventario", e);
            throw e;
        }
    }
    
    @Override
    public List<Object[]> obtenerEstadisticasPorCategoria() throws SQLException {
        String sql = "SELECT categoria, COUNT(*) as total_productos, SUM(stock) as stock_total, " +
                    "AVG(precio) as precio_promedio, SUM(precio * stock) as valor_total " +
                    "FROM productos GROUP BY categoria ORDER BY valor_total DESC";
        
        List<Object[]> estadisticas = new ArrayList<>();
        
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Object[] fila = new Object[5];
                fila[0] = rs.getString("categoria");
                fila[1] = rs.getInt("total_productos");
                fila[2] = rs.getInt("stock_total");
                fila[3] = rs.getBigDecimal("precio_promedio");
                fila[4] = rs.getBigDecimal("valor_total");
                estadisticas.add(fila);
            }
            
            return estadisticas;
            
        } catch (SQLException e) {
            LogUtil.registrarError("OBTENER_ESTADISTICAS_CATEGORIA", 
                "Error al obtener estadísticas por categoría", e);
            throw e;
        }
    }
    
    /**
     * Mapea un ResultSet a objeto Producto
     */
    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setNombre(rs.getString("nombre"));
        producto.setCategoria(rs.getString("categoria"));
        producto.setPrecio(rs.getBigDecimal("precio"));
        producto.setStock(rs.getInt("stock"));
        
        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        if (fechaCreacion != null) {
            producto.setFechaCreacion(fechaCreacion.toLocalDateTime());
        }
        
        Timestamp fechaModificacion = rs.getTimestamp("fecha_modificacion");
        if (fechaModificacion != null) {
            producto.setFechaModificacion(fechaModificacion.toLocalDateTime());
        }
        
        return producto;
    }
}