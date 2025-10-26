package com.inventario;

import com.inventario.service.ImportadorMovimientosCSV;

public class TestImportacion {
    public static void main(String[] args) {
        System.out.println("=== PRUEBA DE IMPORTACIÓN DE MOVIMIENTOS CSV ===\n");

        ImportadorMovimientosCSV importador = new ImportadorMovimientosCSV();

        // Probar con archivo de ejemplo
        String rutaArchivo = "data/movimientos_ejemplo.csv";
        System.out.println("Importando desde: " + rutaArchivo + "\n");

        ImportadorMovimientosCSV.ResultadoImportacion resultado = importador.importarDesdeCSV(rutaArchivo);

        System.out.println("\n=== RESULTADO FINAL ===");
        if (resultado.isExito() && resultado.getMovimientosExitosos() > 0) {
            System.out.println("✓ Importación EXITOSA");
        } else if (resultado.isExito() && resultado.getMovimientosExitosos() == 0) {
            System.out.println("⚠ No se importaron movimientos");
        } else {
            System.out.println("✗ Importación FALLIDA");
        }
    }
}
