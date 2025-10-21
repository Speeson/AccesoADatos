package com.inventario.xml;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase para gestionar la exportación e importación del inventario en formato XML
 * con validación mediante XSD.
 * ACTUALIZADA PARA COINCIDIR CON LA ESTRUCTURA REAL DE 01-init.sql
 */
public class XMLManager {
    
    private static final String NAMESPACE_URI = "http://inventario.dam.es";
    private static final String VERSION = "2.0";
    private Connection connection;
    
    public XMLManager(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Exporta todo el inventario (categorías, productos y movimientos) a un archivo XML
     * @param rutaArchivo Ruta donde se guardará el archivo XML
     * @return true si la exportación fue exitosa
     */
    public boolean exportarInventarioXML(String rutaArchivo) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            // Crear elemento raíz con namespace
            Element inventario = doc.createElementNS(NAMESPACE_URI, "inventario");
            inventario.setAttribute("fechaExportacion", 
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            inventario.setAttribute("version", VERSION);
            doc.appendChild(inventario);
            
            // Exportar categorías
            Element categorias = doc.createElementNS(NAMESPACE_URI, "categorias");
            inventario.appendChild(categorias);
            exportarCategorias(doc, categorias);
            
            // Exportar productos
            Element productos = doc.createElementNS(NAMESPACE_URI, "productos");
            inventario.appendChild(productos);
            exportarProductos(doc, productos);
            
            // Exportar movimientos
            // Exportar movimientos (solo si existen)
            Element movimientos = doc.createElementNS(NAMESPACE_URI, "movimientos");
            int cantidadMovimientos = exportarMovimientos(doc, movimientos);
            if (cantidadMovimientos > 0) {
                inventario.appendChild(movimientos);
            }
            
            // Guardar el documento XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(rutaArchivo));
            transformer.transform(source, result);
            
            System.out.println("✓ Inventario exportado exitosamente a: " + rutaArchivo);
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Error al exportar inventario a XML: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Exporta las categorías de la base de datos al documento XML
     */
    private void exportarCategorias(Document doc, Element categoriasElement) throws SQLException {
        String sql = "SELECT id_categoria, nombre, descripcion, fecha_creacion, fecha_modificacion " +
                    "FROM categorias ORDER BY id_categoria";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                Element categoria = doc.createElementNS(NAMESPACE_URI, "categoria");
                
                crearElemento(doc, categoria, "idCategoria", rs.getString("id_categoria"));
                crearElemento(doc, categoria, "nombre", rs.getString("nombre"));
                
                String descripcion = rs.getString("descripcion");
                if (descripcion != null && !descripcion.isEmpty()) {
                    crearElemento(doc, categoria, "descripcion", descripcion);
                }
                
                Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
                crearElemento(doc, categoria, "fechaCreacion", 
                    fechaCreacion.toLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
                
                Timestamp fechaModificacion = rs.getTimestamp("fecha_modificacion");
                crearElemento(doc, categoria, "fechaModificacion", 
                    fechaModificacion.toLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
                
                categoriasElement.appendChild(categoria);
                count++;
            }
            System.out.println("→ Exportadas " + count + " categorías");
        }
    }
    
    /**
     * Exporta los productos de la base de datos al documento XML
     */
    private void exportarProductos(Document doc, Element productosElement) throws SQLException {
        String sql = "SELECT id_producto, nombre, categoria, precio, stock, " +
                    "fecha_creacion, fecha_modificacion FROM productos ORDER BY id_producto";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                Element producto = doc.createElementNS(NAMESPACE_URI, "producto");
                
                crearElemento(doc, producto, "idProducto", rs.getString("id_producto"));
                crearElemento(doc, producto, "nombre", rs.getString("nombre"));
                crearElemento(doc, producto, "categoria", rs.getString("categoria"));
                crearElemento(doc, producto, "precio", rs.getString("precio"));
                crearElemento(doc, producto, "stock", rs.getString("stock"));
                
                Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
                crearElemento(doc, producto, "fechaCreacion", 
                    fechaCreacion.toLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
                
                Timestamp fechaModificacion = rs.getTimestamp("fecha_modificacion");
                crearElemento(doc, producto, "fechaModificacion", 
                    fechaModificacion.toLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
                
                productosElement.appendChild(producto);
                count++;
            }
            System.out.println("→ Exportados " + count + " productos");
        }
    }
    
    /**
     * Exporta los movimientos de la base de datos al documento XML
     * @return cantidad de movimientos exportados
     */
    private int exportarMovimientos(Document doc, Element movimientosElement) throws SQLException {
        String sql = "SELECT id_movimiento, id_producto, tipo_movimiento, cantidad, " +
                    "stock_anterior, stock_nuevo, motivo, fecha_movimiento, usuario " +
                    "FROM movimientos_stock ORDER BY fecha_movimiento DESC";
        
        int count = 0;
        try (Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Element movimiento = doc.createElementNS(NAMESPACE_URI, "movimiento");
                
                crearElemento(doc, movimiento, "idMovimiento", rs.getString("id_movimiento"));
                crearElemento(doc, movimiento, "idProducto", rs.getString("id_producto"));
                crearElemento(doc, movimiento, "tipoMovimiento", rs.getString("tipo_movimiento"));
                crearElemento(doc, movimiento, "cantidad", rs.getString("cantidad"));
                crearElemento(doc, movimiento, "stockAnterior", rs.getString("stock_anterior"));
                crearElemento(doc, movimiento, "stockNuevo", rs.getString("stock_nuevo"));
                
                String motivo = rs.getString("motivo");
                if (motivo != null && !motivo.isEmpty()) {
                    crearElemento(doc, movimiento, "motivo", motivo);
                }
                
                Timestamp fecha = rs.getTimestamp("fecha_movimiento");
                crearElemento(doc, movimiento, "fechaMovimiento", 
                    fecha.toLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
                
                crearElemento(doc, movimiento, "usuario", rs.getString("usuario"));
                
                movimientosElement.appendChild(movimiento);
                count++;
            }
        }
        System.out.println("→ Exportados " + count + " movimientos");
        return count;
    }
    
    /**
     * Método auxiliar para crear elementos XML con namespace
     */
    private void crearElemento(Document doc, Element parent, String nombre, String valor) {
        Element elemento = doc.createElementNS(NAMESPACE_URI, nombre);
        elemento.setTextContent(valor);
        parent.appendChild(elemento);
    }
    
    /**
     * Valida un archivo XML contra el esquema XSD
     * @param rutaXML Ruta del archivo XML a validar
     * @param rutaXSD Ruta del archivo XSD
     * @return true si el XML es válido
     */
    public boolean validarXML(String rutaXML, String rutaXSD) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new File(rutaXSD));
            Validator validator = schema.newValidator();
            
            validator.validate(new StreamSource(new File(rutaXML)));
            
            System.out.println("✓ El archivo XML es válido según el esquema XSD");
            return true;
            
        } catch (SAXException e) {
            System.err.println("✗ Error de validación XML: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("✗ Error al leer archivos: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Importa el inventario completo desde un archivo XML
     * @param rutaXML Ruta del archivo XML a importar
     * @param rutaXSD Ruta del archivo XSD para validar
     * @param limpiarAntes Si es true, limpia las tablas antes de importar
     * @return true si la importación fue exitosa
     */
    public boolean importarInventarioXML(String rutaXML, String rutaXSD, boolean limpiarAntes) {
        // Validar primero el XML
        if (!validarXML(rutaXML, rutaXSD)) {
            System.err.println("✗ No se puede importar: el XML no es válido");
            return false;
        }
        
        try {
            connection.setAutoCommit(false);
            
            if (limpiarAntes) {
                limpiarTablas();
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(rutaXML));
            doc.getDocumentElement().normalize();
            
            // Importar categorías primero (tienen que existir antes de los productos)
            importarCategoriasDesdeXML(doc);
            
            // Importar productos
            importarProductosDesdeXML(doc);
            
            // Importar movimientos
            importarMovimientosDesdeXML(doc);
            
            connection.commit();
            System.out.println("✓ Inventario importado exitosamente desde: " + rutaXML);
            return true;
            
        } catch (Exception e) {
            try {
                connection.rollback();
                System.err.println("✗ Error en importación, cambios revertidos");
            } catch (SQLException ex) {
                System.err.println("✗ Error al revertir transacción: " + ex.getMessage());
            }
            System.err.println("✗ Error al importar inventario: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Limpia las tablas de movimientos, productos y categorías
     */
    private void limpiarTablas() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM movimientos_stock");
            stmt.executeUpdate("DELETE FROM productos");
            stmt.executeUpdate("DELETE FROM categorias");
            System.out.println("→ Tablas limpiadas antes de importar");
        }
    }

    /**
     * Importa las categorías desde el documento XML
     * Usa UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) para manejar duplicados
     */
    private void importarCategoriasDesdeXML(Document doc) throws SQLException {
        NodeList categoriasLista = doc.getElementsByTagNameNS(NAMESPACE_URI, "categoria");
        
        if (categoriasLista.getLength() == 0) {
            System.out.println("→ No hay categorías para importar");
            return;
        }
        
        // UPSERT: Inserta si no existe, actualiza si existe
        String sql = "INSERT INTO categorias (id_categoria, nombre, descripcion, fecha_creacion, fecha_modificacion) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "nombre = VALUES(nombre), " +
                    "descripcion = VALUES(descripcion), " +
                    "fecha_modificacion = VALUES(fecha_modificacion)";
        
        int insertadas = 0;
        int actualizadas = 0;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < categoriasLista.getLength(); i++) {
                Element categoria = (Element) categoriasLista.item(i);
                
                String idCategoriaStr = getValorElemento(categoria, "idCategoria");
                if (idCategoriaStr == null || idCategoriaStr.isEmpty()) {
                    System.err.println("⚠ Saltando categoría sin ID en posición " + i);
                    continue;
                }
                
                pstmt.setInt(1, Integer.parseInt(idCategoriaStr));
                pstmt.setString(2, getValorElemento(categoria, "nombre"));
                
                // Descripción puede ser null
                String descripcion = getValorElemento(categoria, "descripcion");
                if (descripcion != null && !descripcion.isEmpty()) {
                    pstmt.setString(3, descripcion);
                } else {
                    pstmt.setNull(3, java.sql.Types.VARCHAR);
                }
                
                String fechaCreacionStr = getValorElemento(categoria, "fechaCreacion");
                LocalDateTime fechaCreacion = LocalDateTime.parse(fechaCreacionStr, DateTimeFormatter.ISO_DATE_TIME);
                pstmt.setTimestamp(4, Timestamp.valueOf(fechaCreacion));
                
                String fechaModStr = getValorElemento(categoria, "fechaModificacion");
                LocalDateTime fechaMod = LocalDateTime.parse(fechaModStr, DateTimeFormatter.ISO_DATE_TIME);
                pstmt.setTimestamp(5, Timestamp.valueOf(fechaMod));
                
                int affectedRows = pstmt.executeUpdate();
                
                // executeUpdate retorna 1 para INSERT, 2 para UPDATE
                if (affectedRows == 1) {
                    insertadas++;
                } else if (affectedRows == 2) {
                    actualizadas++;
                }
            }
        }
        
        System.out.println("→ Categorías: " + insertadas + " insertadas, " + actualizadas + " actualizadas");
    }
    
    /**
     * Importa los productos desde el documento XML
     * Usa UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) para manejar duplicados
     */
    private void importarProductosDesdeXML(Document doc) throws SQLException {
        NodeList productosLista = doc.getElementsByTagNameNS(NAMESPACE_URI, "producto");
        
        // UPSERT: Inserta si no existe, actualiza si existe
        String sql = "INSERT INTO productos (id_producto, nombre, categoria, precio, stock, " +
                    "fecha_creacion, fecha_modificacion) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "nombre = VALUES(nombre), " +
                    "categoria = VALUES(categoria), " +
                    "precio = VALUES(precio), " +
                    "stock = VALUES(stock), " +
                    "fecha_modificacion = VALUES(fecha_modificacion)";
        
        int insertados = 0;
        int actualizados = 0;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < productosLista.getLength(); i++) {
                Element producto = (Element) productosLista.item(i);
                
                pstmt.setInt(1, Integer.parseInt(getValorElemento(producto, "idProducto")));
                pstmt.setString(2, getValorElemento(producto, "nombre"));
                pstmt.setString(3, getValorElemento(producto, "categoria"));
                pstmt.setDouble(4, Double.parseDouble(getValorElemento(producto, "precio")));
                pstmt.setInt(5, Integer.parseInt(getValorElemento(producto, "stock")));
                
                String fechaCreacionStr = getValorElemento(producto, "fechaCreacion");
                LocalDateTime fechaCreacion = LocalDateTime.parse(fechaCreacionStr, DateTimeFormatter.ISO_DATE_TIME);
                pstmt.setTimestamp(6, Timestamp.valueOf(fechaCreacion));
                
                String fechaModStr = getValorElemento(producto, "fechaModificacion");
                LocalDateTime fechaMod = LocalDateTime.parse(fechaModStr, DateTimeFormatter.ISO_DATE_TIME);
                pstmt.setTimestamp(7, Timestamp.valueOf(fechaMod));
                
                int affectedRows = pstmt.executeUpdate();
                
                if (affectedRows == 1) {
                    insertados++;
                } else if (affectedRows == 2) {
                    actualizados++;
                }
            }
        }
        System.out.println("→ Productos: " + insertados + " insertados, " + actualizados + " actualizados");
    }
    
    /**
     * Importa los movimientos desde el documento XML
     * Usa UPSERT (INSERT ... ON DUPLICATE KEY UPDATE) para manejar duplicados
     */
    private void importarMovimientosDesdeXML(Document doc) throws SQLException {
        NodeList movimientosLista = doc.getElementsByTagNameNS(NAMESPACE_URI, "movimiento");
        
        if (movimientosLista.getLength() == 0) {
            System.out.println("→ No hay movimientos para importar");
            return;
        }
        
        // UPSERT: Inserta si no existe, actualiza si existe
        String sql = "INSERT INTO movimientos_stock (id_movimiento, id_producto, tipo_movimiento, cantidad, " +
                    "stock_anterior, stock_nuevo, motivo, fecha_movimiento, usuario) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "tipo_movimiento = VALUES(tipo_movimiento), " +
                    "cantidad = VALUES(cantidad), " +
                    "stock_anterior = VALUES(stock_anterior), " +
                    "stock_nuevo = VALUES(stock_nuevo), " +
                    "motivo = VALUES(motivo), " +
                    "usuario = VALUES(usuario)";
        
        int insertados = 0;
        int actualizados = 0;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < movimientosLista.getLength(); i++) {
                Element movimiento = (Element) movimientosLista.item(i);
                
                pstmt.setInt(1, Integer.parseInt(getValorElemento(movimiento, "idMovimiento")));
                pstmt.setInt(2, Integer.parseInt(getValorElemento(movimiento, "idProducto")));
                pstmt.setString(3, getValorElemento(movimiento, "tipoMovimiento"));
                pstmt.setInt(4, Integer.parseInt(getValorElemento(movimiento, "cantidad")));
                pstmt.setInt(5, Integer.parseInt(getValorElemento(movimiento, "stockAnterior")));
                pstmt.setInt(6, Integer.parseInt(getValorElemento(movimiento, "stockNuevo")));
                
                // Motivo puede ser null
                String motivo = getValorElemento(movimiento, "motivo");
                if (motivo != null && !motivo.isEmpty()) {
                    pstmt.setString(7, motivo);
                } else {
                    pstmt.setNull(7, java.sql.Types.VARCHAR);
                }
                
                String fechaStr = getValorElemento(movimiento, "fechaMovimiento");
                LocalDateTime fecha = LocalDateTime.parse(fechaStr, DateTimeFormatter.ISO_DATE_TIME);
                pstmt.setTimestamp(8, Timestamp.valueOf(fecha));
                
                pstmt.setString(9, getValorElemento(movimiento, "usuario"));
                
                int affectedRows = pstmt.executeUpdate();
                
                if (affectedRows == 1) {
                    insertados++;
                } else if (affectedRows == 2) {
                    actualizados++;
                }
            }
        }
        System.out.println("→ Movimientos: " + insertados + " insertados, " + actualizados + " actualizados");
    }
    
    /**
     * Obtiene el valor de un elemento hijo dentro de un elemento padre
     */
    private String getValorElemento(Element padre, String nombreElemento) {
        NodeList lista = padre.getElementsByTagNameNS(NAMESPACE_URI, nombreElemento);
        if (lista.getLength() > 0) {
            Element elemento = (Element) lista.item(0);
            return elemento.getTextContent();
        }
        return null;
    }
}