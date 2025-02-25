package com.appBancaria.servicio;

import com.appBancaria.db.DBConexion;
import com.appBancaria.modelo.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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

    public Map<String, Object> consignarCuenta(String numCuentaOrigen, String numCuentaDestino, double monto) throws SQLException {
        String queryDestino = "SELECT saldo FROM cuentas WHERE numero_cuenta = ?";
        String updateDestino = "UPDATE cuentas SET saldo = saldo + ? WHERE numero_cuenta = ?";
        String insertTransaccion = "INSERT INTO transacciones (tipo_transaccion, monto, cuenta_origen_id, cuenta_destino_id) VALUES (?, ?, (SELECT id FROM cuentas WHERE numero_cuenta = ?), (SELECT id FROM cuentas WHERE numero_cuenta = ?))";
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
            
            // Insertar registro en la tabla de transacciones
            try (PreparedStatement stmtInsertTransaccion = conn.prepareStatement(insertTransaccion)) {
                stmtInsertTransaccion.setString(1, "consignacion");
                stmtInsertTransaccion.setDouble(2, monto);
                stmtInsertTransaccion.setString(3, numCuentaOrigen);
                stmtInsertTransaccion.setString(4, numCuentaDestino);
                stmtInsertTransaccion.executeUpdate();
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

    public String autenticarUsuario(String correo, String contrasena) throws SQLException {
        String query = "SELECT id FROM clientes WHERE correo_electronico = ? AND contrasena = ?";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, correo);
            stmt.setString(2, contrasena);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int clienteId = rs.getInt("id");
                String idSesion = iniciarSesion(clienteId);
                return idSesion;
            } else {
                return null;
            }
        }
    }

    private String iniciarSesion(int clienteId) throws SQLException {
        String update = "UPDATE clientes SET sesion_activa = true, id_sesion = ? WHERE id = ?";
        String idSesion = UUID.randomUUID().toString();
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(update)) {
            
            stmt.setString(1, idSesion);
            stmt.setInt(2, clienteId);
            stmt.executeUpdate();
        }
        return idSesion;
    }
    /*
    public void cerrarSesion(String correo) throws SQLException {
        String update = "UPDATE clientes SET sesion_activa = false, id_sesion = NULL WHERE correo_electronico = ?";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(update)) {
            
            stmt.setString(1, correo);
            stmt.executeUpdate();
        }
    } */

    public void cerrarSesion(String correo, String idSesion) throws SQLException {
        String update = "UPDATE clientes SET sesion_activa = false, id_sesion = NULL WHERE correo_electronico = ? AND id_sesion = ?";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(update)) {
            
            stmt.setString(1, correo);
            stmt.setString(2, idSesion);
            int rowsUpdated = stmt.executeUpdate();
            
            if (rowsUpdated == 0) {
                throw new SQLException("No se encontró una sesión activa para el correo proporcionado.");
            }
        }
    }

    public boolean verificarSesionActiva(String correo) throws SQLException {
        String query = "SELECT sesion_activa FROM clientes WHERE correo_electronico = ?";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, correo);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBoolean("sesion_activa");
            } else {
                return false;
            }
        }
    }

    public Map<String, Object> obtenerHistorialTransacciones(String idSesion) throws SQLException {
        String queryCliente = "SELECT id FROM clientes WHERE id_sesion = ?";
        String queryTransacciones = "SELECT t.tipo_transaccion, t.fecha_hora, t.monto, c.numero_cuenta AS cuenta_origen, " +
                                    "c2.numero_cuenta AS cuenta_destino, cl.numero_identificacion AS identificacion_origen " +
                                    "FROM transacciones t " +
                                    "JOIN cuentas c ON t.cuenta_origen_id = c.id " +
                                    "JOIN cuentas c2 ON t.cuenta_destino_id = c2.id " +
                                    "JOIN clientes cl ON c.cliente_id = cl.id " +
                                    "WHERE c2.cliente_id = ?";
        Map<String, Object> historial = new HashMap<>();
        
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmtCliente = conn.prepareStatement(queryCliente);
             PreparedStatement stmtTransacciones = conn.prepareStatement(queryTransacciones)) {
            
            // Obtener el ID del cliente a partir del ID de sesión
            stmtCliente.setString(1, idSesion);
            ResultSet rsCliente = stmtCliente.executeQuery();
            if (!rsCliente.next()) {
                throw new SQLException("No se encontró un cliente con el ID de sesión proporcionado.");
            }
            int clienteId = rsCliente.getInt("id");
            
            // Obtener las transacciones en las que el cliente es el destinatario
            stmtTransacciones.setInt(1, clienteId);
            ResultSet rsTransacciones = stmtTransacciones.executeQuery();
            
            while (rsTransacciones.next()) {
                Map<String, Object> transaccion = new HashMap<>();
                transaccion.put("tipo_transaccion", rsTransacciones.getString("tipo_transaccion"));
                transaccion.put("fecha_hora", rsTransacciones.getTimestamp("fecha_hora"));
                transaccion.put("monto", rsTransacciones.getDouble("monto"));
                transaccion.put("cuenta_origen", rsTransacciones.getString("cuenta_origen"));
                transaccion.put("cuenta_destino", rsTransacciones.getString("cuenta_destino"));
                transaccion.put("identificacion_origen", rsTransacciones.getString("identificacion_origen"));
                historial.put(String.valueOf(rsTransacciones.getRow()), transaccion);
            }
        }
        return historial;
    }
}
