package com.appBancaria.servicio;

import com.appBancaria.db.DBConexion;
import com.appBancaria.modelo.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class GestorCuentas {
    
    public double consultarSaldo(String numeroCuenta) throws SQLException {
        String query = "SELECT saldo FROM cuentas WHERE numero_cuenta = ?";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, numeroCuenta);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("saldo");
            } else {
                throw new SQLException("Cuenta no encontrada");
            }
        }
    }
    
    public double consultarSaldo(int identificacion) throws SQLException {
        String query = "SELECT saldo FROM cuentas WHERE cliente_id = (SELECT id FROM clientes WHERE numero_identificacion = ?)";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, identificacion);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("saldo");
            } else {
                throw new SQLException("Cliente no encontrado");
            }
        }
    }
    
    public String crearCuenta(Cliente cliente) throws SQLException {
        Connection conn = DBConexion.getInstance().getConnection();
        conn.setAutoCommit(false);
        
        try {
            String insertCliente = "INSERT INTO clientes (nombre_completo, correo_electronico, numero_identificacion, contrasena) " +
                                 "VALUES (?, ?, ?, ?) RETURNING id";
            int clienteId;
            try (PreparedStatement stmt = conn.prepareStatement(insertCliente)) {
                stmt.setString(1, cliente.getNombre());
                stmt.setString(2, cliente.getCorreo());
                stmt.setInt(3, cliente.getIdentificacion());
                stmt.setString(4, cliente.getContrasena());
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new SQLException("Failed to create client");
                }
                clienteId = rs.getInt("id");
            }

            String numeroCuenta = generarNumeroCuenta();
            String insertCuenta = "INSERT INTO cuentas (numero_cuenta, cliente_id, saldo) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertCuenta)) {
                stmt.setString(1, numeroCuenta);
                stmt.setInt(2, clienteId);  // Using the returned client ID
                stmt.setDouble(3, 0.0);
                stmt.executeUpdate();
            }
            
            conn.commit();
            return numeroCuenta;
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    private String generarNumeroCuenta() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
