package com.appBancaria.servicio;

import com.appBancaria.db.DBConexion;
import com.appBancaria.modelo.Cliente;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class GestorCuentas {
    
    public double consultarSaldo(String numeroCuenta) throws SQLException {
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
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
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
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
        System.out.println("Iniciando proceso de creación de cuenta para " + cliente.getNombre());
        
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
        // Obtenemos una nueva conexión específicamente para esta operación
        Connection conn = null;
        String numeroCuenta = null;
        boolean autoCommitOriginal = true;
        
        try {
            System.out.println("Solicitando conexión a la base de datos...");
            conn = DBConexion.getInstance().getConnection();
            
            if (conn == null) {
                System.err.println("Error: La conexión es nula");
                throw new SQLException("No se pudo establecer conexión con la base de datos");
            }
            
            // Guardamos el estado original del autoCommit
            autoCommitOriginal = conn.getAutoCommit();
            
            // Desactivamos el autocommit para la transacción
            System.out.println("Desactivando autoCommit...");
            conn.setAutoCommit(false);
            
            // Verificar si la conexión está activa
            if (conn.isClosed()) {
                System.err.println("Error: La conexión está cerrada");
                throw new SQLException("La conexión a la base de datos está cerrada");
            }
            
            // Verificamos primero si ya existe un cliente con ese correo o identificación
            System.out.println("Verificando si el cliente ya existe...");
            String checkExistingQuery = "SELECT COUNT(*) FROM clientes WHERE correo_electronico = ? OR numero_identificacion = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkExistingQuery);
            checkStmt.setString(1, cliente.getCorreo());
            checkStmt.setInt(2, cliente.getIdentificacion());
            ResultSet checkRs = checkStmt.executeQuery();
            checkRs.next();
            int count = checkRs.getInt(1);
            checkRs.close();
            checkStmt.close();
            
            if (count > 0) {
                throw new SQLException("Ya existe un cliente con ese correo o número de identificación");
            }
            
            // Insertar cliente en la tabla clientes
            System.out.println("Insertando nuevo cliente en la base de datos...");
            String insertCliente = "INSERT INTO clientes (nombre_completo, correo_electronico, numero_identificacion, contrasena) " +
                                   "VALUES (?, ?, ?, ?) RETURNING id";
            PreparedStatement stmtCliente = conn.prepareStatement(insertCliente);
            stmtCliente.setString(1, cliente.getNombre());
            stmtCliente.setString(2, cliente.getCorreo());
            stmtCliente.setInt(3, cliente.getIdentificacion());
            stmtCliente.setString(4, cliente.getContrasena());
            
            ResultSet rsCliente = stmtCliente.executeQuery();
            int clienteId = 0;
            if (rsCliente.next()) {
                clienteId = rsCliente.getInt("id");
                System.out.println("Cliente insertado con ID: " + clienteId);
            } else {
                throw new SQLException("No se pudo crear el cliente en la base de datos");
            }
            rsCliente.close();
            stmtCliente.close();
            
            // Generar número de cuenta único
            System.out.println("Generando número de cuenta...");
            numeroCuenta = String.valueOf(100000 + (int)(Math.random() * 900000));
            
            // Verificar que el número de cuenta no exista
            String checkCuentaQuery = "SELECT COUNT(*) FROM cuentas WHERE numero_cuenta = ?";
            PreparedStatement checkCuentaStmt = conn.prepareStatement(checkCuentaQuery);
            checkCuentaStmt.setString(1, numeroCuenta);
            ResultSet checkCuentaRs = checkCuentaStmt.executeQuery();
            checkCuentaRs.next();
            int cuentaCount = checkCuentaRs.getInt(1);
            checkCuentaRs.close();
            checkCuentaStmt.close();
            
            if (cuentaCount > 0) {
                // En el improbable caso de que ya exista, generamos otro
                numeroCuenta = String.valueOf(100000 + (int)(Math.random() * 900000));
            }
            
            // Insertar nueva cuenta
            System.out.println("Insertando nueva cuenta con número: " + numeroCuenta);
            String insertCuenta = "INSERT INTO cuentas (numero_cuenta, cliente_id, saldo) VALUES (?, ?, ?)";
            PreparedStatement stmtCuenta = conn.prepareStatement(insertCuenta);
            stmtCuenta.setString(1, numeroCuenta);
            stmtCuenta.setInt(2, clienteId);
            stmtCuenta.setDouble(3, 0.0); // Saldo inicial
            stmtCuenta.executeUpdate();
            stmtCuenta.close();
            
            // Confirmar la transacción
            System.out.println("Confirmando transacción...");
            conn.commit();
            System.out.println("Transacción completada - Cuenta creada exitosamente");
            
            return numeroCuenta;
            
        } catch (SQLException e) {
            System.err.println("Error durante la creación de cuenta: " + e.getMessage());
            
            // Intentar hacer rollback
            if (conn != null) {
                try {
                    System.out.println("Haciendo rollback de la transacción...");
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
            
            throw e;
            
        } finally {
            // Restaurar el autoCommit a su estado original
            if (conn != null && !conn.isClosed()) {
                try {
                    conn.setAutoCommit(autoCommitOriginal);
                    System.out.println("AutoCommit restaurado a: " + autoCommitOriginal);
                } catch (SQLException e) {
                    System.err.println("Error al restaurar autoCommit: " + e.getMessage());
                }
            }
        }
    }

    public Map<String, Object> consignarCuenta(String idSesion, String numCuentaDestino, double monto) throws SQLException {
        String queryOrigen = "SELECT c.numero_cuenta FROM cuentas c " +
                             "JOIN clientes cl ON c.cliente_id = cl.id " +
                             "WHERE cl.id_sesion = ?";
        String queryDestino = "SELECT saldo FROM cuentas WHERE numero_cuenta = ?";
        String updateDestino = "UPDATE cuentas SET saldo = saldo + ? WHERE numero_cuenta = ?";
        String insertTransaccion = "INSERT INTO transacciones (tipo_transaccion, monto, cuenta_origen_id, cuenta_destino_id) VALUES (?, ?, (SELECT id FROM cuentas WHERE numero_cuenta = ?), (SELECT id FROM cuentas WHERE numero_cuenta = ?))";
        Map<String, Object> resultado = new HashMap<>();
        
        Connection conn = null;
        PreparedStatement stmtOrigen = null;
        PreparedStatement stmtDestino = null;
        PreparedStatement stmtUpdateDestino = null;
        PreparedStatement stmtInsertTransaccion = null;
        ResultSet rsOrigen = null;
        ResultSet rsDestino = null;
        
        try {
            // Limpiar prepared statements antes de comenzar
            DBConexion.getInstance().cleanPreparedStatements();
            
            conn = DBConexion.getInstance().getConnection();
            conn.setAutoCommit(false);
            
            // Obtener cuenta de origen usando el ID de sesión
            String numCuentaOrigen;
            stmtOrigen = conn.prepareStatement(queryOrigen);
            stmtOrigen.setString(1, idSesion);
            rsOrigen = stmtOrigen.executeQuery();
            if (!rsOrigen.next()) {
                throw new SQLException("No se encontró una cuenta asociada a la sesión activa");
            }
            numCuentaOrigen = rsOrigen.getString("numero_cuenta");
            
            // Verificar que la cuenta de destino exista
            double saldoAnterior = 0.0;
            double saldoNuevo = 0.0;
            stmtDestino = conn.prepareStatement(queryDestino);
            stmtDestino.setString(1, numCuentaDestino);
            rsDestino = stmtDestino.executeQuery();
            if (!rsDestino.next()) {
                throw new SQLException("Cuenta de destino no encontrada");
            }
            saldoAnterior = rsDestino.getDouble("saldo");
            
            // Actualizar el saldo de la cuenta de destino
            stmtUpdateDestino = conn.prepareStatement(updateDestino);
            stmtUpdateDestino.setDouble(1, monto);
            stmtUpdateDestino.setString(2, numCuentaDestino);
            stmtUpdateDestino.executeUpdate();
            
            // Cerrar el ResultSet y el PreparedStatement anteriores
            if (rsDestino != null) rsDestino.close();
            if (stmtDestino != null) stmtDestino.close();
            
            // Obtener el saldo actualizado
            stmtDestino = conn.prepareStatement(queryDestino);
            stmtDestino.setString(1, numCuentaDestino);
            rsDestino = stmtDestino.executeQuery();
            if (rsDestino.next()) {
                saldoNuevo = rsDestino.getDouble("saldo");
            }
            
            // Insertar registro en la tabla de transacciones
            stmtInsertTransaccion = conn.prepareStatement(insertTransaccion);
            stmtInsertTransaccion.setString(1, "consignacion");
            stmtInsertTransaccion.setDouble(2, monto);
            stmtInsertTransaccion.setString(3, numCuentaOrigen);
            stmtInsertTransaccion.setString(4, numCuentaDestino);
            stmtInsertTransaccion.executeUpdate();
            
            conn.commit();
            
            // Preparar la respuesta
            resultado.put("exito", true);
            resultado.put("mensaje", "Consignación exitosa");
            resultado.put("saldoAnterior", saldoAnterior);
            resultado.put("saldoNuevo", saldoNuevo);
            resultado.put("monto", monto);
            resultado.put("numeroCuentaDestino", numCuentaDestino);
            resultado.put("numeroCuentaOrigen", numCuentaOrigen);
            
            return resultado;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error durante rollback: " + ex.getMessage());
                }
            }
            resultado.put("exito", false);
            resultado.put("mensaje", "Error en la consignación: " + e.getMessage());
            throw e;
        } finally {
            // Cerrar todos los recursos en orden inverso
            if (rsDestino != null) try { rsDestino.close(); } catch (SQLException e) { }
            if (rsOrigen != null) try { rsOrigen.close(); } catch (SQLException e) { }
            if (stmtInsertTransaccion != null) try { stmtInsertTransaccion.close(); } catch (SQLException e) { }
            if (stmtUpdateDestino != null) try { stmtUpdateDestino.close(); } catch (SQLException e) { }
            if (stmtDestino != null) try { stmtDestino.close(); } catch (SQLException e) { }
            if (stmtOrigen != null) try { stmtOrigen.close(); } catch (SQLException e) { }
            
            // Restaurar autoCommit
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.err.println("Error restaurando autoCommit: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Autentica a un usuario y genera un token JWT si las credenciales son correctas
     * @param correo Correo del usuario
     * @param contrasena Contraseña del usuario
     * @return Mapa con el token JWT y otros datos de sesión, o null si la autenticación falla
     * @throws SQLException si ocurre un error con la base de datos
     */
    public Map<String, Object> autenticarUsuario(String correo, String contrasena) throws SQLException {
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
        String query = "SELECT id FROM clientes WHERE correo_electronico = ? AND contrasena = ?";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, correo);
            stmt.setString(2, contrasena);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int clienteId = rs.getInt("id");
                
                // Generar JWT y sessionId
                Map<String, String> tokenInfo = JWTUtil.generarToken(clienteId);
                String jwt = tokenInfo.get("token");
                String sessionId = tokenInfo.get("sessionId");
                
                // Actualizar el id_sesion en la base de datos
                actualizarIdSesion(clienteId, sessionId);
                
                // Crear respuesta con token JWT
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("idUsuario", clienteId);
                respuesta.put("token", jwt);
                respuesta.put("idSesion", sessionId);
                
                // Limpiar prepared statements después de la autenticación exitosa
                DBConexion.getInstance().cleanPreparedStatements();
                
                return respuesta;
            } else {
                return null;
            }
        }
    }

    /**
     * Actualiza el id_sesion de un cliente en la base de datos
     * @param clienteId ID del cliente
     * @param sessionId ID de sesión
     * @throws SQLException si ocurre un error con la base de datos
     */
    private void actualizarIdSesion(int clienteId, String sessionId) throws SQLException {
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
        String update = "UPDATE clientes SET id_sesion = ? WHERE id = ?";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(update)) {
            
            stmt.setString(1, sessionId);
            stmt.setInt(2, clienteId);
            stmt.executeUpdate();
        }
    }

    public void cerrarSesion(String correo, String idSesion) throws SQLException {
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
        String update = "UPDATE clientes SET id_sesion = NULL WHERE correo_electronico = ? AND id_sesion = ?";
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
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
        String query = "SELECT id_sesion FROM clientes WHERE correo_electronico = ?";
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, correo);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Si id_sesion no es null, entonces hay una sesión activa
                return rs.getString("id_sesion") != null;
            } else {
                return false;
            }
        }
    }

    public Map<String, Object> obtenerHistorialTransacciones(String idSesion) throws SQLException {
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
        String queryCliente = "SELECT id FROM clientes WHERE id_sesion = ?";
        String queryTransacciones = "SELECT t.tipo_transaccion, t.fecha_hora, t.monto, c.numero_cuenta AS cuenta_origen, " +
                                    "c2.numero_cuenta AS cuenta_destino, cl.numero_identificacion AS identificacion_origen " +
                                    "FROM transacciones t " +
                                    "JOIN cuentas c ON t.cuenta_origen_id = c.id " +
                                    "JOIN cuentas c2 ON t.cuenta_destino_id = c2.id " +
                                    "JOIN clientes cl ON c.cliente_id = cl.id " +
                                    "WHERE c2.cliente_id = ? " +
                                    "ORDER BY t.fecha_hora ASC";
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
    
    /**
     * Obtiene la información completa de un cliente a partir de su ID de sesión
     * @param idSesion ID de sesión del cliente
     * @return Mapa con la información del cliente
     * @throws SQLException si ocurre un error con la base de datos
     */
    public Map<String, Object> obtenerInformacionCliente(String idSesion) throws SQLException {
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
        String query = "SELECT cl.id, cl.nombre_completo, cl.correo_electronico, cl.numero_identificacion, " +
                      "c.numero_cuenta, c.saldo " +
                      "FROM clientes cl " +
                      "JOIN cuentas c ON cl.id = c.cliente_id " +
                      "WHERE cl.id_sesion = ?";
                      
        Map<String, Object> informacion = new HashMap<>();
        
        try (Connection conn = DBConexion.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, idSesion);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                informacion.put("idSesion", idSesion);
                informacion.put("nombre", rs.getString("nombre_completo"));
                informacion.put("correo", rs.getString("correo_electronico"));
                informacion.put("identificacion", rs.getString("numero_identificacion"));
                informacion.put("numeroCuenta", rs.getString("numero_cuenta"));
                informacion.put("saldo", rs.getDouble("saldo"));
                return informacion;
            } else {
                throw new SQLException("No se encontró la información del cliente con el ID de sesión proporcionado.");
            }
        }
    }
    
    /**
     * Método que combina la autenticación y obtención de información del cliente
     * @param correo correo electrónico del cliente
     * @param contrasena contraseña del cliente
     * @return Mapa con la información completa del cliente o null si la autenticación falla
     * @throws SQLException si ocurre un error con la base de datos
     */
    public Map<String, Object> autenticarYObtenerInformacionCliente(String correo, String contrasena) throws SQLException {
        // Limpiar prepared statements antes de comenzar
        DBConexion.getInstance().cleanPreparedStatements();
        
        Map<String, Object> infoAutenticacion = autenticarUsuario(correo, contrasena);
        if (infoAutenticacion != null) {
            // Limpiar prepared statements entre operaciones
            DBConexion.getInstance().cleanPreparedStatements();
            
            String idSesion = (String) infoAutenticacion.get("idSesion");
            Map<String, Object> infoCliente = obtenerInformacionCliente(idSesion);
            
            // Agregar el token JWT a la información del cliente
            infoCliente.put("token", infoAutenticacion.get("token"));
            
            return infoCliente;
        } else {
            return null;
        }
    }
}
