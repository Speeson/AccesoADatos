package com.inventario.xml;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Clase de prueba para las funcionalidades de exportación/importación XML
 */
public class XMLManagerCLI {
    
    // Configuración de la base de datos - AJUSTA SEGÚN TU CONFIGURACIÓN
    private static final String DB_URL = "jdbc:mysql://localhost:33061/inventario_db";
    private static final String DB_USER = "inventario_user";
    private static final String DB_PASSWORD = "inventario_pass";
    
    // Rutas de archivos
    private static final String RUTA_XSD = "src/main/resources/inventario.xsd";
    private static final String RUTA_XML_BACKUP = "backups/inventario_backup.xml";
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Connection connection = null;
        
        try {
            // Conectar a la base de datos
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            XMLManager xmlManager = new XMLManager(connection);
            
            boolean salir = false;
            while (!salir) {
                mostrarMenu();
                int opcion = scanner.nextInt();
                scanner.nextLine(); // Limpiar buffer
                
                switch (opcion) {
                    case 1:
                        exportarInventario(xmlManager, scanner);
                        break;
                    case 2:
                        validarArchivoXML(xmlManager, scanner);
                        break;
                    case 3:
                        importarInventario(xmlManager, scanner);
                        break;
                    case 4:
                        exportarYValidar(xmlManager);
                        break;
                    case 0:
                        salir = true;
                        System.out.println("¡Hasta luego!");
                        break;
                    default:
                        System.out.println("Opción no válida");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error de conexión a la base de datos: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            scanner.close();
        }
    }
    
    private static void mostrarMenu() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  GESTIÓN XML DEL INVENTARIO - V2.0     ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("1. Exportar inventario a XML");
        System.out.println("2. Validar archivo XML");
        System.out.println("3. Importar inventario desde XML");
        System.out.println("4. Exportar y validar (proceso completo)");
        System.out.println("0. Salir");
        System.out.print("\nSeleccione una opción: ");
    }
    
    private static void exportarInventario(XMLManager manager, Scanner scanner) {
        System.out.print("\nRuta del archivo XML (vacío para usar ruta por defecto): ");
        String ruta = scanner.nextLine().trim();
        if (ruta.isEmpty()) {
            ruta = RUTA_XML_BACKUP;
        }
        
        System.out.println("\n→ Exportando inventario...");
        if (manager.exportarInventarioXML(ruta)) {
            System.out.println("✓ Exportación completada exitosamente");
        } else {
            System.out.println("✗ Error en la exportación");
        }
    }
    
    private static void validarArchivoXML(XMLManager manager, Scanner scanner) {
        System.out.print("\nRuta del archivo XML a validar: ");
        String rutaXML = scanner.nextLine().trim();
        
        System.out.print("Ruta del archivo XSD (vacío para usar ruta por defecto): ");
        String rutaXSD = scanner.nextLine().trim();
        if (rutaXSD.isEmpty()) {
            rutaXSD = RUTA_XSD;
        }
        
        System.out.println("\n→ Validando XML contra esquema XSD...");
        if (manager.validarXML(rutaXML, rutaXSD)) {
            System.out.println("✓ El archivo XML es válido");
        } else {
            System.out.println("✗ El archivo XML NO es válido");
        }
    }
    
    private static void importarInventario(XMLManager manager, Scanner scanner) {
        System.out.print("\nRuta del archivo XML a importar: ");
        String rutaXML = scanner.nextLine().trim();
        
        System.out.print("Ruta del archivo XSD (vacío para usar ruta por defecto): ");
        String rutaXSD = scanner.nextLine().trim();
        if (rutaXSD.isEmpty()) {
            rutaXSD = RUTA_XSD;
        }
        
        System.out.print("\n¿Desea limpiar las tablas antes de importar? (S/N): ");
        String respuesta = scanner.nextLine().trim().toUpperCase();
        boolean limpiar = respuesta.equals("S") || respuesta.equals("SI") || respuesta.equals("SÍ");
        
        if (limpiar) {
            System.out.print("⚠️  ADVERTENCIA: Se eliminarán todos los datos actuales. ¿Confirma? (S/N): ");
            String confirmacion = scanner.nextLine().trim().toUpperCase();
            if (!confirmacion.equals("S") && !confirmacion.equals("SI") && !confirmacion.equals("SÍ")) {
                System.out.println("Importación cancelada");
                return;
            }
        }
        
        System.out.println("\n→ Importando inventario desde XML...");
        if (manager.importarInventarioXML(rutaXML, rutaXSD, limpiar)) {
            System.out.println("✓ Importación completada exitosamente");
        } else {
            System.out.println("✗ Error en la importación");
        }
    }
    
    private static void exportarYValidar(XMLManager manager) {
        System.out.println("\n→ Proceso completo: Exportar y validar");
        System.out.println("→ Exportando inventario...");
        
        if (manager.exportarInventarioXML(RUTA_XML_BACKUP)) {
            System.out.println("✓ Exportación completada");
            System.out.println("\n→ Validando el XML exportado...");
            
            if (manager.validarXML(RUTA_XML_BACKUP, RUTA_XSD)) {
                System.out.println("✓ Validación exitosa");
                System.out.println("\n✓ Proceso completo finalizado correctamente");
            } else {
                System.out.println("✗ El XML exportado no es válido");
            }
        } else {
            System.out.println("✗ Error en la exportación");
        }
    }
}