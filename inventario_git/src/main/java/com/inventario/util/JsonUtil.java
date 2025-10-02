package com.inventario.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.inventario.model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilidad para manejo de archivos JSON
 */
public class JsonUtil {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Exporta una lista de productos a archivo JSON
     */
    public static void exportarProductos(List<Producto> productos, String rutaArchivo) throws IOException {
        logger.info("Exportando {} productos a JSON: {}", productos.size(), rutaArchivo);
        
        try {
            // Crear directorio si no existe
            File archivo = new File(rutaArchivo);
            File directorio = archivo.getParentFile();
            if (directorio != null && !directorio.exists()) {
                directorio.mkdirs();
            }
            
            Map<String, Object> datos = new HashMap<>();
            datos.put("fecha_exportacion", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            datos.put("total_productos", productos.size());
            datos.put("productos", productos);
            
            objectMapper.writeValue(archivo, datos);
            logger.info("Exportación completada exitosamente");
            
        } catch (IOException e) {
            logger.error("Error al exportar productos a JSON: {}", rutaArchivo, e);
            throw e;
        }
    }
    
    /**
     * Exporta productos con stock bajo a JSON
     */
    public static void exportarProductosStockBajo(List<Producto> productos, int limiteBajo, String rutaArchivo) throws IOException {
        logger.info("Exportando productos con stock bajo (límite: {}) a JSON: {}", limiteBajo, rutaArchivo);
        
        try {
            // Crear directorio si no existe
            File archivo = new File(rutaArchivo);
            File directorio = archivo.getParentFile();
            if (directorio != null && !directorio.exists()) {
                directorio.mkdirs();
            }
            
            Map<String, Object> reporte = new HashMap<>();
            reporte.put("fecha_reporte", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            reporte.put("limite_stock_bajo", limiteBajo);
            reporte.put("total_productos_stock_bajo", productos.size());
            reporte.put("productos_stock_bajo", productos);
            
            // Estadísticas adicionales
            Map<String, Object> estadisticas = new HashMap<>();
            estadisticas.put("stock_total", productos.stream().mapToInt(Producto::getStock).sum());
            estadisticas.put("valor_total", productos.stream()
                    .map(Producto::getValorTotal)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
            
            reporte.put("estadisticas", estadisticas);
            
            objectMapper.writeValue(archivo, reporte);
            logger.info("Reporte de stock bajo exportado exitosamente");
            
        } catch (IOException e) {
            logger.error("Error al exportar reporte de stock bajo: {}", rutaArchivo, e);
            throw e;
        }
    }
    
    /**
     * Exporta estadísticas por categoría a JSON
     */
    public static void exportarEstadisticasPorCategoria(Map<String, Object> estadisticas, String rutaArchivo) throws IOException {
        logger.info("Exportando estadísticas por categoría a JSON: {}", rutaArchivo);
        
        try {
            // Crear directorio si no existe
            File archivo = new File(rutaArchivo);
            File directorio = archivo.getParentFile();
            if (directorio != null && !directorio.exists()) {
                directorio.mkdirs();
            }
            
            Map<String, Object> reporte = new HashMap<>();
            reporte.put("fecha_reporte", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            reporte.put("tipo_reporte", "estadisticas_por_categoria");
            reporte.put("datos", estadisticas);
            
            objectMapper.writeValue(archivo, reporte);
            logger.info("Estadísticas por categoría exportadas exitosamente");
            
        } catch (IOException e) {
            logger.error("Error al exportar estadísticas por categoría: {}", rutaArchivo, e);
            throw e;
        }
    }
    
    /**
     * Lee productos desde un archivo JSON
     */
    public static List<Producto> importarProductos(String rutaArchivo) throws IOException {
        logger.info("Importando productos desde JSON: {}", rutaArchivo);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = objectMapper.readValue(new File(rutaArchivo), Map.class);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productosMap = (List<Map<String, Object>>) datos.get("productos");
            
            List<Producto> productos = productosMap.stream()
                    .map(JsonUtil::mapToProducto)
                    .toList();
            
            logger.info("Importación completada. {} productos importados", productos.size());
            return productos;
            
        } catch (IOException e) {
            logger.error("Error al importar productos desde JSON: {}", rutaArchivo, e);
            throw e;
        }
    }
    
    /**
     * Convierte un Map a objeto Producto
     */
    private static Producto mapToProducto(Map<String, Object> map) {
        Producto producto = new Producto();
        
        if (map.get("idProducto") != null) {
            producto.setIdProducto((Integer) map.get("idProducto"));
        }
        if (map.get("nombre") != null) {
            producto.setNombre((String) map.get("nombre"));
        }
        if (map.get("categoria") != null) {
            producto.setCategoria((String) map.get("categoria"));
        }
        if (map.get("precio") != null) {
            producto.setPrecio(new java.math.BigDecimal(map.get("precio").toString()));
        }
        if (map.get("stock") != null) {
            producto.setStock((Integer) map.get("stock"));
        }
        
        return producto;
    }
    
    /**
     * Genera un nombre de archivo único con timestamp
     */
    public static String generarNombreArchivoConTimestamp(String baseName, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.%s", baseName, timestamp, extension);
    }
    
    /**
     * Valida que un archivo JSON tenga la estructura esperada
     */
    public static boolean validarEstructuraJSON(String rutaArchivo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = objectMapper.readValue(new File(rutaArchivo), Map.class);
            
            // Verificar que tenga las claves básicas
            return datos.containsKey("productos") && datos.get("productos") instanceof List;
            
        } catch (Exception e) {
            logger.error("Error validando estructura JSON: {}", rutaArchivo, e);
            return false;
        }
    }
}