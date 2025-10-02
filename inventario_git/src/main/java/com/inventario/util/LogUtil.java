package com.inventario.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilidad para manejo de logs y archivos de error
 */
public class LogUtil {
    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);
    private static final String LOGS_DIR = "logs/";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Guarda una lista de errores en un archivo de log
     */
    public static void guardarErroresCSV(List<String> errores, String nombreArchivo) {
        String rutaCompleta = LOGS_DIR + nombreArchivo;
        
        try {
            // Crear directorio si no existe
            java.io.File logsDir = new java.io.File(LOGS_DIR);
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(rutaCompleta, StandardCharsets.UTF_8, true)) {
                writer.write("=== ERRORES DE PROCESAMIENTO CSV ===\n");
                writer.write("Fecha: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
                writer.write("Total errores: " + errores.size() + "\n\n");
                
                for (String error : errores) {
                    writer.write(error + "\n");
                }
                
                writer.write("\n" + "=".repeat(50) + "\n\n");
                writer.flush();
            }
            
            logger.info("Errores guardados en archivo: {}", rutaCompleta);
            
        } catch (IOException e) {
            logger.error("Error al guardar archivo de errores: {}", rutaCompleta, e);
        }
    }
    
    /**
     * Registra una operación exitosa en el log de actividades
     */
    public static void registrarOperacionExitosa(String operacion, String detalles) {
        String mensaje = String.format("OPERACIÓN EXITOSA - %s: %s", operacion, detalles);
        logger.info(mensaje);
        guardarEnLogActividades("INFO", operacion, mensaje);
    }
    
    /**
     * Registra un error en el log de actividades
     */
    public static void registrarError(String operacion, String error, Exception e) {
        String mensaje = String.format("ERROR - %s: %s", operacion, error);
        logger.error(mensaje, e);
        guardarEnLogActividades("ERROR", operacion, mensaje + (e != null ? " - " + e.getMessage() : ""));
    }
    
    /**
     * Registra una advertencia en el log de actividades
     */
    public static void registrarAdvertencia(String operacion, String advertencia) {
        String mensaje = String.format("ADVERTENCIA - %s: %s", operacion, advertencia);
        logger.warn(mensaje);
        guardarEnLogActividades("WARN", operacion, mensaje);
    }
    
    /**
     * Guarda un registro en el archivo de log de actividades
     */
    private static void guardarEnLogActividades(String nivel, String operacion, String mensaje) {
        String rutaArchivo = LOGS_DIR + "actividades.log";
        
        try {
            java.io.File logsDir = new java.io.File(LOGS_DIR);
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(rutaArchivo, StandardCharsets.UTF_8, true)) {
                String linea = String.format("[%s] %s - %s - %s%n",
                        LocalDateTime.now().format(TIMESTAMP_FORMAT),
                        nivel,
                        operacion,
                        mensaje);
                writer.write(linea);
                writer.flush();
            }
            
        } catch (IOException e) {
            logger.error("Error al escribir en log de actividades", e);
        }
    }
    
    /**
     * Crea un reporte de importación/exportación
     */
    public static void crearReporteOperacion(String tipoOperacion, int totalProcesados, 
                                           int exitosos, int errores, long tiempoMs) {
        String rutaArchivo = LOGS_DIR + "reportes_operaciones.log";
        
        try {
            java.io.File logsDir = new java.io.File(LOGS_DIR);
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            
            try (FileWriter writer = new FileWriter(rutaArchivo, StandardCharsets.UTF_8, true)) {
                writer.write("=== REPORTE DE OPERACIÓN ===\n");
                writer.write("Fecha: " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "\n");
                writer.write("Operación: " + tipoOperacion + "\n");
                writer.write("Total procesados: " + totalProcesados + "\n");
                writer.write("Exitosos: " + exitosos + "\n");
                writer.write("Errores: " + errores + "\n");
                writer.write("Tiempo total: " + tiempoMs + " ms\n");
                writer.write("Tasa de éxito: " + String.format("%.2f%%", (exitosos * 100.0 / totalProcesados)) + "\n");
                writer.write("\n" + "=".repeat(30) + "\n\n");
                writer.flush();
            }
            
            logger.info("Reporte de operación guardado: {}", rutaArchivo);
            
        } catch (IOException e) {
            logger.error("Error al crear reporte de operación", e);
        }
    }
    
    /**
     * Genera un nombre de archivo de log con timestamp
     */
    public static String generarNombreLogConTimestamp(String baseName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("%s_%s.log", baseName, timestamp);
    }
}