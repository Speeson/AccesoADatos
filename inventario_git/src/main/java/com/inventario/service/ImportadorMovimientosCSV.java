package com.inventario.service;

import com.inventario.dao.MovimientoStockDAO;
import com.inventario.dao.impl.MovimientoStockDAOImpl;
import com.inventario.model.MovimientoStock;
import com.inventario.util.LogUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase para importar movimientos de stock desde archivos CSV
 * Implementa procesamiento por lotes con transacciones y rollback
 */
public class ImportadorMovimientosCSV {

    private static final int TAMANO_LOTE = 100; // Procesar en lotes de 100 movimientos
    private final MovimientoStockDAO movimientoDAO;

    // Estadísticas de la importación
    private int totalLineas = 0;
    private int movimientosExitosos = 0;
    private int movimientosConError = 0;
    private int lotesExitosos = 0;
    private int lotesConError = 0;
    private final List<String> errores = new ArrayList<>();

    public ImportadorMovimientosCSV() {
        this.movimientoDAO = new MovimientoStockDAOImpl();
    }

    /**
     * Importa movimientos desde un archivo CSV
     * Procesa en lotes con transacciones atómicas y rollback automático en caso de error
     */
    public ResultadoImportacion importarDesdeCSV(String rutaArchivo) {
        resetearEstadisticas();

        LogUtil.registrarOperacionExitosa("IMPORTAR_CSV_INICIO",
            "Iniciando importación de movimientos desde: " + rutaArchivo);

        System.out.println("\n=== IMPORTACIÓN DE MOVIMIENTOS DESDE CSV ===");
        System.out.println("Archivo: " + rutaArchivo);
        System.out.println("Tamaño de lote: " + TAMANO_LOTE + " movimientos");
        System.out.println();

        try {
            // Validar estructura del CSV
            if (!validarEstructuraCSV(rutaArchivo)) {
                errores.add("ERROR CRÍTICO: El archivo no tiene la estructura correcta");
                return generarResultado(false);
            }

            // Leer y procesar el archivo
            List<MovimientoStock> todosMovimientos = leerMovimientosDesdeCSV(rutaArchivo);

            if (todosMovimientos.isEmpty()) {
                System.out.println("No se encontraron movimientos válidos para importar.");
                return generarResultado(true);
            }

            // Procesar en lotes
            procesarEnLotes(todosMovimientos);

            // Generar resultado final
            return generarResultado(true);

        } catch (IOException e) {
            String mensajeError = "Error al leer el archivo CSV: " + e.getMessage();
            errores.add(mensajeError);
            LogUtil.registrarError("IMPORTAR_CSV_IO", mensajeError, e);
            return generarResultado(false);
        } catch (Exception e) {
            String mensajeError = "Error inesperado durante la importación: " + e.getMessage();
            errores.add(mensajeError);
            LogUtil.registrarError("IMPORTAR_CSV_ERROR", mensajeError, e);
            return generarResultado(false);
        }
    }

    /**
     * Valida que el CSV tenga las columnas requeridas
     */
    private boolean validarEstructuraCSV(String rutaArchivo) {
        String[] columnasRequeridas = {"id_producto", "tipo_movimiento", "cantidad"};

        try (FileReader reader = new FileReader(rutaArchivo, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
                     .setDelimiter(',')
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            List<String> headers = parser.getHeaderNames();
            System.out.println("Columnas detectadas: " + headers);

            for (String columna : columnasRequeridas) {
                if (!headers.stream().anyMatch(h -> h.equalsIgnoreCase(columna))) {
                    String error = "Falta columna requerida: " + columna;
                    errores.add(error);
                    System.err.println("ERROR: " + error);
                    return false;
                }
            }

            System.out.println("Estructura del CSV validada correctamente.\n");
            return true;

        } catch (IOException e) {
            errores.add("Error al validar estructura: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lee y parsea todos los movimientos del CSV
     */
    private List<MovimientoStock> leerMovimientosDesdeCSV(String rutaArchivo) throws IOException {
        List<MovimientoStock> movimientos = new ArrayList<>();

        try (FileReader reader = new FileReader(rutaArchivo, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.Builder.create()
                     .setDelimiter(',')
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            int numeroLinea = 1; // La primera línea de datos es la 2 (línea 1 es el header)

            for (CSVRecord record : parser) {
                numeroLinea++;
                totalLineas++;

                try {
                    MovimientoStock movimiento = parsearMovimiento(record);

                    // Validar el movimiento parseado
                    if (validarMovimiento(movimiento, numeroLinea)) {
                        movimientos.add(movimiento);
                    } else {
                        movimientosConError++;
                    }

                } catch (Exception e) {
                    String error = String.format("Línea %d: Error al parsear - %s",
                            numeroLinea, e.getMessage());
                    errores.add(error);
                    movimientosConError++;
                }
            }

            System.out.println("Lectura completada: " + movimientos.size() +
                             " movimientos válidos, " + movimientosConError + " con errores\n");

        } catch (IOException e) {
            LogUtil.registrarError("LEER_CSV", "Error al leer archivo CSV", e);
            throw e;
        }

        return movimientos;
    }

    /**
     * Parsea un registro CSV a objeto MovimientoStock
     */
    private MovimientoStock parsearMovimiento(CSVRecord record) {
        int idProducto = Integer.parseInt(record.get("id_producto").trim());
        String tipoMovimiento = record.get("tipo_movimiento").trim().toUpperCase();
        int cantidad = Integer.parseInt(record.get("cantidad").trim());

        // Campos opcionales
        String motivo = obtenerCampoOpcional(record, "motivo");
        String usuario = obtenerCampoOpcional(record, "usuario");

        return new MovimientoStock(idProducto, tipoMovimiento, cantidad, motivo, usuario);
    }

    /**
     * Obtiene un campo opcional del CSV
     */
    private String obtenerCampoOpcional(CSVRecord record, String nombreCampo) {
        try {
            String valor = record.get(nombreCampo);
            return (valor != null && !valor.trim().isEmpty()) ? valor.trim() : null;
        } catch (IllegalArgumentException e) {
            // La columna no existe en el CSV
            return null;
        }
    }

    /**
     * Valida un movimiento antes de procesarlo
     */
    private boolean validarMovimiento(MovimientoStock movimiento, int numeroLinea) {
        List<String> erroresValidacion = new ArrayList<>();

        // Validación básica del modelo
        if (!movimiento.isValid()) {
            erroresValidacion.add("Movimiento inválido según modelo");
        }

        // Validaciones específicas
        if (movimiento.getIdProducto() <= 0) {
            erroresValidacion.add("ID de producto inválido: " + movimiento.getIdProducto());
        }

        if (!movimiento.getTipoMovimiento().equals("ENTRADA") &&
            !movimiento.getTipoMovimiento().equals("SALIDA")) {
            erroresValidacion.add("Tipo de movimiento inválido: " + movimiento.getTipoMovimiento());
        }

        if (movimiento.getCantidad() <= 0) {
            erroresValidacion.add("Cantidad debe ser mayor a 0: " + movimiento.getCantidad());
        }

        // Si hay errores, registrarlos
        if (!erroresValidacion.isEmpty()) {
            String error = String.format("Línea %d: %s",
                    numeroLinea, String.join(", ", erroresValidacion));
            errores.add(error);
            return false;
        }

        return true;
    }

    /**
     * Procesa los movimientos en lotes con transacciones atómicas
     */
    private void procesarEnLotes(List<MovimientoStock> movimientos) {
        int totalMovimientos = movimientos.size();
        int numeroLote = 0;

        System.out.println("=== PROCESAMIENTO POR LOTES ===");
        System.out.println("Total de movimientos a procesar: " + totalMovimientos);
        System.out.println();

        for (int i = 0; i < totalMovimientos; i += TAMANO_LOTE) {
            numeroLote++;
            int fin = Math.min(i + TAMANO_LOTE, totalMovimientos);
            List<MovimientoStock> lote = movimientos.subList(i, fin);

            System.out.printf("Procesando lote %d/%d (%d movimientos)... ",
                    numeroLote,
                    (int) Math.ceil((double) totalMovimientos / TAMANO_LOTE),
                    lote.size());

            try {
                // Procesar lote completo en una transacción atómica
                int procesados = movimientoDAO.registrarMovimientosLote(lote);
                movimientosExitosos += procesados;
                lotesExitosos++;
                System.out.println("✓ EXITOSO (" + procesados + " movimientos)");

            } catch (SQLException e) {
                // Si falla, se hace ROLLBACK automático de todo el lote
                lotesConError++;
                movimientosConError += lote.size();

                String error = String.format("Lote %d FALLÓ (ROLLBACK aplicado): %s",
                        numeroLote, e.getMessage());
                errores.add(error);

                System.out.println("✗ FALLÓ - ROLLBACK aplicado");
                System.err.println("  Error: " + e.getMessage());

                LogUtil.registrarError("PROCESAR_LOTE",
                    "Error en lote " + numeroLote + " - Rollback aplicado", e);
            }
        }

        System.out.println();
    }

    /**
     * Genera el resultado final de la importación
     */
    private ResultadoImportacion generarResultado(boolean exito) {
        ResultadoImportacion resultado = new ResultadoImportacion();
        resultado.setExito(exito);
        resultado.setTotalLineas(totalLineas);
        resultado.setMovimientosExitosos(movimientosExitosos);
        resultado.setMovimientosConError(movimientosConError);
        resultado.setLotesExitosos(lotesExitosos);
        resultado.setLotesConError(lotesConError);
        resultado.setErrores(new ArrayList<>(errores));

        // Mostrar resumen
        mostrarResumen(resultado);

        // Registrar en log
        if (exito) {
            LogUtil.registrarOperacionExitosa("IMPORTAR_CSV_FIN",
                String.format("Importación completada: %d exitosos, %d errores",
                        movimientosExitosos, movimientosConError));
        } else {
            LogUtil.registrarError("IMPORTAR_CSV_FIN",
                "Importación completada con errores", null);
        }

        return resultado;
    }

    /**
     * Muestra el resumen de la importación
     */
    private void mostrarResumen(ResultadoImportacion resultado) {
        System.out.println("\n=== RESUMEN DE IMPORTACIÓN ===");
        System.out.println("Total de líneas procesadas: " + resultado.getTotalLineas());
        System.out.println("Movimientos exitosos: " + resultado.getMovimientosExitosos());
        System.out.println("Movimientos con error: " + resultado.getMovimientosConError());
        System.out.println("Lotes exitosos: " + resultado.getLotesExitosos());
        System.out.println("Lotes con error (rollback): " + resultado.getLotesConError());

        if (resultado.getTotalLineas() > 0) {
            double tasaExito = (resultado.getMovimientosExitosos() * 100.0) / resultado.getTotalLineas();
            System.out.printf("Tasa de éxito: %.1f%%\n", tasaExito);
        }

        if (!resultado.getErrores().isEmpty()) {
            System.out.println("\nErrores encontrados (" + resultado.getErrores().size() + "):");
            resultado.getErrores().forEach(error -> System.err.println("  - " + error));
        }

        System.out.println("================================\n");
    }

    /**
     * Resetea las estadísticas para una nueva importación
     */
    private void resetearEstadisticas() {
        totalLineas = 0;
        movimientosExitosos = 0;
        movimientosConError = 0;
        lotesExitosos = 0;
        lotesConError = 0;
        errores.clear();
    }

    /**
     * Clase interna para encapsular el resultado de la importación
     */
    public static class ResultadoImportacion {
        private boolean exito;
        private int totalLineas;
        private int movimientosExitosos;
        private int movimientosConError;
        private int lotesExitosos;
        private int lotesConError;
        private List<String> errores;

        public boolean isExito() { return exito; }
        public void setExito(boolean exito) { this.exito = exito; }

        public int getTotalLineas() { return totalLineas; }
        public void setTotalLineas(int totalLineas) { this.totalLineas = totalLineas; }

        public int getMovimientosExitosos() { return movimientosExitosos; }
        public void setMovimientosExitosos(int movimientosExitosos) { this.movimientosExitosos = movimientosExitosos; }

        public int getMovimientosConError() { return movimientosConError; }
        public void setMovimientosConError(int movimientosConError) { this.movimientosConError = movimientosConError; }

        public int getLotesExitosos() { return lotesExitosos; }
        public void setLotesExitosos(int lotesExitosos) { this.lotesExitosos = lotesExitosos; }

        public int getLotesConError() { return lotesConError; }
        public void setLotesConError(int lotesConError) { this.lotesConError = lotesConError; }

        public List<String> getErrores() { return errores; }
        public void setErrores(List<String> errores) { this.errores = errores; }
    }
}
