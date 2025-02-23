package com.appBancaria.db;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConexion {
    private static DBConexion instance;
    private Connection connection;
    private final Dotenv dotenv;
    
    private DBConexion() {
        dotenv = Dotenv.load();
    }
    
    public static synchronized DBConexion getInstance() {
        if (instance == null) {
            instance = new DBConexion();
        }
        return instance;
    }
    
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = dotenv.get("DB_URL");
            String user = dotenv.get("DB_USER");
            String password = dotenv.get("DB_PASSWORD");
            
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }
    
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}