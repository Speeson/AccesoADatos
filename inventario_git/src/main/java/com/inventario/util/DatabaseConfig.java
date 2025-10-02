package com.inventario.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuración y gestión de conexiones a la base de datos
 */
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    // Configuración por defecto (puede ser sobrescrita por variables de entorno)
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DATABASE = "inventario_db";
    private static final String DEFAULT_USER = "inventario_user";
    private static final String DEFAULT_PASSWORD = "inventario_pass";
    
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    
    private static DatabaseConfig instance;
    private final String connectionUrl;
    private final Properties connectionProps;
    
    private DatabaseConfig() {
        // Cargar configuración desde variables de entorno o usar valores por defecto
        String host = getEnvOrDefault("DB_HOST", DEFAULT_HOST);
        String port = getEnvOrDefault("DB_PORT", DEFAULT_PORT);
        String database = getEnvOrDefault("DB_NAME", DEFAULT_DATABASE);
        String user = getEnvOrDefault("DB_USER", DEFAULT_USER);
        String password = getEnvOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);
        
        this.connectionUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
            host, port, database
        );
        
        this.connectionProps = new Properties();
        this.connectionProps.setProperty("user", user);
        this.connectionProps.setProperty("password", password);
        this.connectionProps.setProperty("useUnicode", "true");
        this.connectionProps.setProperty("characterEncoding", "UTF-8");
        this.connectionProps.setProperty("autoReconnect", "true");
        
        logger.info("Configuración de base de datos inicializada: {}:{}/{}", host, port, database);
    }
    
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    /**
     * Obtiene una nueva conexión a la base de datos
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER_CLASS);
            Connection connection = DriverManager.getConnection(connectionUrl, connectionProps);
            connection.setAutoCommit(true); // Por defecto autocommit activado
            logger.debug("Nueva conexión establecida a la base de datos");
            return connection;
        } catch (ClassNotFoundException e) {
            logger.error("Driver MySQL no encontrado", e);
            throw new SQLException("Driver MySQL no disponible", e);
        } catch (SQLException e) {
            logger.error("Error al conectar con la base de datos: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Obtiene una conexión con autocommit desactivado para transacciones
     */
    public Connection getConnectionForTransaction() throws SQLException {
        Connection connection = getConnection();
        connection.setAutoCommit(false);
        logger.debug("Conexión para transacción creada (autocommit=false)");
        return connection;
    }
    
    /**
     * Cierra una conexión de forma segura
     */
    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                logger.debug("Conexión cerrada correctamente");
            } catch (SQLException e) {
                logger.warn("Error al cerrar conexión: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Realiza rollback de una transacción de forma segura
     */
    public void rollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
                logger.debug("Rollback ejecutado correctamente");
            } catch (SQLException e) {
                logger.error("Error durante rollback: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Confirma una transacción de forma segura
     */
    public void commit(Connection connection) {
        if (connection != null) {
            try {
                connection.commit();
                logger.debug("Commit ejecutado correctamente");
            } catch (SQLException e) {
                logger.error("Error durante commit: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Verifica si la conexión a la base de datos está disponible
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5); // Timeout de 5 segundos
        } catch (SQLException e) {
            logger.error("Test de conexión fallido: {}", e.getMessage());
            return false;
        }
    }
    
    private String getEnvOrDefault(String envName, String defaultValue) {
        String value = System.getenv(envName);
        return value != null ? value : defaultValue;
    }
    
    public String getConnectionUrl() {
        return connectionUrl;
    }
}