package com.inventario.util;

import com.inventario.model.Categoria;
import com.inventario.model.Producto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para lectura y escritura de archivos CSV con separador punto y coma
 */
public class CsvUtil {
    private static final Logger logger = LoggerFactory.getLogger(CsvUtil.class);
    
    /**
     * Lee productos desde un archivo CSV con separador punto y coma
     */
    public static List<Producto> leerProductosCSV(String rutaArchivo) throws IOException {
        List<Producto> productos = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        logger.info("Iniciando lectura de productos desde: {}", rutaArchivo);
        
        try (FileReader reader = new FileReader(rutaArchivo, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
                     .setDelimiter(';')
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {
            
            int lineNumber = 1; // Header es línea 0
            
            for (CSVRecord record : parser) {
                lineNumber++;
                try {
                    Producto producto = parsearProducto(record);
                    if (producto.isValid()) {
                        productos.add(producto);
                        logger.debug("Producto leído: {}", producto.getNombre());
                    } else {
                        String error = String.format("Línea %d: Producto inválido - %s", 
                                lineNumber, producto.toString());
                        errores.add(error);
                        logger.warn(error);
                    }
                } catch (Exception e) {
                    String error = String.format("Línea %d: Error al procesar - %s", 
                            lineNumber, e.getMessage());
                    errores.add(error);
                    logger.error(error, e);
                }
            }
            
            logger.info("Lectura completada. Productos válidos: {}, Errores: {}", 
                    productos.size(), errores.size());
            
            // Guardar errores en archivo de log si existen
            if (!errores.isEmpty()) {
                LogUtil.guardarErroresCSV(errores, "productos_errores.log");
            }
            
        } catch (IOException e) {
            logger.error("Error al leer archivo CSV: {}", rutaArchivo, e);
            throw e;
        }
        
        return productos;
    }
    
    /**
     * Lee categorías desde un archivo CSV con separador punto y coma
     */
    public static List<Categoria> leerCategoriasCSV(String rutaArchivo) throws IOException {
        List<Categoria> categorias = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        
        logger.info("Iniciando lectura de categorías desde: {}", rutaArchivo);
        
        try (FileReader reader = new FileReader(rutaArchivo, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
                     .setDelimiter(';')
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {
            
            int lineNumber = 1;
            
            for (CSVRecord record : parser) {
                lineNumber++;
                try {
                    Categoria categoria = parsearCategoria(record);
                    if (categoria.isValid()) {
                        categorias.add(categoria);
                        logger.debug("Categoría leída: {}", categoria.getNombre());
                    } else {
                        String error = String.format("Línea %d: Categoría inválida - %s", 
                                lineNumber, categoria.toString());
                        errores.add(error);
                        logger.warn(error);
                    }
                } catch (Exception e) {
                    String error = String.format("Línea %d: Error al procesar - %s", 
                            lineNumber, e.getMessage());
                    errores.add(error);
                    logger.error(error, e);
                }
            }
            
            logger.info("Lectura completada. Categorías válidas: {}, Errores: {}", 
                    categorias.size(), errores.size());
            
            if (!errores.isEmpty()) {
                LogUtil.guardarErroresCSV(errores, "categorias_errores.log");
            }
            
        } catch (IOException e) {
            logger.error("Error al leer archivo CSV: {}", rutaArchivo, e);
            throw e;
        }
        
        return categorias;
    }
    
    /**
     * Parsea un registro CSV a objeto Producto
     */
    private static Producto parsearProducto(CSVRecord record) {
        Producto producto = new Producto();
        
        // ID (opcional, puede ser auto-generado)
        String idStr = record.get("id_producto");
        if (idStr != null && !idStr.trim().isEmpty()) {
            producto.setIdProducto(Integer.parseInt(idStr.trim()));
        }
        
        // Nombre (requerido)
        producto.setNombre(record.get("nombre"));
        
        // Categoría (requerido) - Normalizar acentos
        String categoria = record.get("categoria");
        if (categoria != null) {
            // Convertir categorías con acentos a sin acentos
            categoria = categoria.trim()
                .replace("Electrónica", "Electronica")
                .replace("Informática", "Informatica")
                .replace("Alimentación", "Alimentacion");
            producto.setCategoria(categoria);
        }
        
        // Precio (requerido)
        String precioStr = record.get("precio");
        if (precioStr != null && !precioStr.trim().isEmpty()) {
            producto.setPrecio(new BigDecimal(precioStr.trim()));
        }
        
        // Stock (requerido)
        String stockStr = record.get("stock");
        if (stockStr != null && !stockStr.trim().isEmpty()) {
            producto.setStock(Integer.parseInt(stockStr.trim()));
        }
        
        return producto;
    }
    
    /**
     * Parsea un registro CSV a objeto Categoria
     */
    private static Categoria parsearCategoria(CSVRecord record) {
        Categoria categoria = new Categoria();
        
        // ID (opcional)
        String idStr = record.get("id_categoria");
        if (idStr != null && !idStr.trim().isEmpty()) {
            categoria.setIdCategoria(Integer.parseInt(idStr.trim()));
        }
        
        // Nombre (requerido) - Normalizar acentos
        String nombre = record.get("nombre");
        if (nombre != null) {
            nombre = nombre.trim()
                .replace("Electrónica", "Electronica")
                .replace("Informática", "Informatica")
                .replace("Alimentación", "Alimentacion");
            categoria.setNombre(nombre);
        }
        
        // Descripción (opcional)
        categoria.setDescripcion(record.get("descripcion"));
        
        return categoria;
    }
    
    /**
     * Valida que un archivo CSV tenga las columnas requeridas
     */
    public static boolean validarEstructuraProductos(String rutaArchivo) {
        String[] columnasRequeridas = {"id_producto", "nombre", "categoria", "precio", "stock"};
        return validarColumnas(rutaArchivo, columnasRequeridas);
    }
    
    public static boolean validarEstructuraCategorias(String rutaArchivo) {
        String[] columnasRequeridas = {"nombre", "descripcion"};
        return validarColumnas(rutaArchivo, columnasRequeridas);
    }
    
    private static boolean validarColumnas(String rutaArchivo, String[] columnasRequeridas) {
        try (FileReader reader = new FileReader(rutaArchivo, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
                     .setDelimiter(';')
                     .setHeader()
                     .build())) {
            
            logger.info("Headers detectados en el archivo: {}", parser.getHeaderNames());
            logger.info("Buscando columnas requeridas: {}", java.util.Arrays.toString(columnasRequeridas));

            for (String columna : columnasRequeridas) {
                if (!parser.getHeaderNames().contains(columna)) {
                    logger.error("Columna requerida '{}' no encontrada en {}", columna, rutaArchivo);
                    return false;
                }
            }
            return true;
            
        } catch (IOException e) {
            logger.error("Error validando estructura CSV: {}", rutaArchivo, e);
            return false;
        }
    }
}