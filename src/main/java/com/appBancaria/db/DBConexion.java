package com.appBancaria.db;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConexion {
    private static DBConexion instance;
    private Connection connection;
    private final Dotenv dotenv;
    
    // Variables para controlar la reconexión
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY_MS = 2000; // 2 segundos
    
    private DBConexion() {
        dotenv = Dotenv.load();
    }
    
    public static synchronized DBConexion getInstance() {
        if (instance == null) {
            instance = new DBConexion();
        }
        return instance;
    }
    
    /**
     * Obtiene una conexión a la base de datos.
     * Si la conexión está cerrada o es nula, intenta obtener una nueva.
     * @return Una conexión activa a la base de datos
     * @throws SQLException si no se puede establecer la conexión
     */
    public synchronized Connection getConnection() throws SQLException {
        // Verificar si la conexión es válida
        if (connection == null || connection.isClosed()) {
            return createNewConnection();
        }
        
        // Verificar si la conexión sigue activa
        try {
            if (!connection.isValid(3)) { // timeout de 3 segundos
                System.out.println("La conexión a la BD no es válida. Creando una nueva conexión...");
                closeConnection(); // Cerrar la conexión inválida
                return createNewConnection();
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar conexión: " + e.getMessage());
            // En caso de error al verificar, intentar crear una nueva
            return createNewConnection();
        }
        
        return connection;
    }
    
    /**
     * Limpia los prepared statements de la conexión actual
     * Útil para evitar errores como "prepared statement S_1 already exists"
     */
    public synchronized void cleanPreparedStatements() {
        if (connection != null) {
            try {
                try (Statement stmt = connection.createStatement()) {
                    // Este comando de PostgreSQL limpia todos los prepared statements
                    stmt.execute("DEALLOCATE ALL");
                }
                System.out.println("Prepared statements limpiados correctamente");
            } catch (SQLException e) {
                System.err.println("Error al limpiar prepared statements: " + e.getMessage());
            }
        }
    }
    
    /**
     * Crea una nueva conexión a la base de datos con reintentos si falla
     * @return Conexión a la base de datos
     * @throws SQLException si no se puede establecer la conexión después de los reintentos
     */
    private Connection createNewConnection() throws SQLException {
        SQLException lastException = null;
        
        for (int attempt = 0; attempt < MAX_RECONNECT_ATTEMPTS; attempt++) {
            try {
                String url = dotenv.get("DB_URL");
                String user = dotenv.get("DB_USER");
                String password = dotenv.get("DB_PASSWORD");
                
                System.out.println("Intentando conectar a la BD... (intento " + (attempt + 1) + "/" + MAX_RECONNECT_ATTEMPTS + ")");
                connection = DriverManager.getConnection(url, user, password);
                System.out.println("Conexión establecida exitosamente!");
                return connection;
            } catch (SQLException e) {
                lastException = e;
                System.err.println("Error al conectar a la BD: " + e.getMessage());
                
                if (attempt < MAX_RECONNECT_ATTEMPTS - 1) {
                    try {
                        System.out.println("Reintentando en " + RECONNECT_DELAY_MS/1000 + " segundos...");
                        Thread.sleep(RECONNECT_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Interrupción durante reconexión a la BD", ie);
                    }
                }
            }
        }
        
        // Si llegamos aquí, todos los intentos fallaron
        throw new SQLException("No se pudo conectar a la base de datos después de " + 
                              MAX_RECONNECT_ATTEMPTS + " intentos", lastException);
    }
    
    /**
     * Cierra la conexión actual a la base de datos
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Conexión a la BD cerrada correctamente");
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            } finally {
                connection = null; // Asegurar que se marque como nula incluso si hay error
            }
        }
    }
}