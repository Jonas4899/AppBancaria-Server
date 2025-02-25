package com.appBancaria.servicio;

import com.appBancaria.db.DBConexion;
import com.appBancaria.modelo.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

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

    public Map<String, Object> consignarCuenta(String numCuentaOrigen, String numCuentaDestino, double monto) throws SQLException {
        String queryDestino = "SELECT saldo FROM cuentas WHERE numero_cuenta = ?";
        String updateDestino = "UPDATE cuentas SET saldo = saldo + ? WHERE numero_cuenta = ?";
        Map<String, Object> resultado = new HashMap<>();
        
        Connection conn = DBConexion.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);
            
            // Verificar que la cuenta de destino exista
            double saldoAnterior = 0.0;
            double saldoNuevo = 0.0;
            try (PreparedStatement stmtDestino = conn.prepareStatement(queryDestino)) {
                stmtDestino.setString(1, numCuentaDestino);
                ResultSet rsDestino = stmtDestino.executeQuery();
                if (!rsDestino.next()) {
                    throw new SQLException("Cuenta de destino no encontrada");
                }
                saldoAnterior = rsDestino.getDouble("saldo");
            }
            
            // Actualizar el saldo de la cuenta de destino
            try (PreparedStatement stmtUpdateDestino = conn.prepareStatement(updateDestino)) {
                stmtUpdateDestino.setDouble(1, monto);
                stmtUpdateDestino.setString(2, numCuentaDestino);
                stmtUpdateDestino.executeUpdate();
            }
            
            // Obtener el saldo actualizado
            try (PreparedStatement stmtDestino = conn.prepareStatement(queryDestino)) {
                stmtDestino.setString(1, numCuentaDestino);
                ResultSet rsDestino = stmtDestino.executeQuery();
                if (rsDestino.next()) {
                    saldoNuevo = rsDestino.getDouble("saldo");
                }
            }
            
            conn.commit();
            
            // Preparar la respuesta
            resultado.put("exito", true);
            resultado.put("mensaje", "Consignación exitosa");
            resultado.put("saldoAnterior", saldoAnterior);
            resultado.put("saldoNuevo", saldoNuevo);
            resultado.put("monto", monto);
            resultado.put("numeroCuentaDestino", numCuentaDestino);
            
            return resultado;
        } catch (SQLException e) {
            conn.rollback();
            resultado.put("exito", false);
            resultado.put("mensaje", "Error en la consignación: " + e.getMessage());
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    
    private String generarNumeroCuenta() throws SQLException {
        String numeroCuenta;
        boolean existe;

        String query = "SELECT COUNT(*) FROM cuentas WHERE numero_cuenta = ?";

        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            do {
                numeroCuenta = String.valueOf((int) (Math.random() * 900000) + 100000);
                stmt.setString(1, numeroCuenta);
                ResultSet rs = stmt.executeQuery();
                rs.next();
                existe = rs.getInt(1) > 0;
            } while (existe);
        }

        return numeroCuenta;
    }
}
