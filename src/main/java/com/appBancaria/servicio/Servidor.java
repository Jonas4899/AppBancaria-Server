package com.appBancaria.servicio;

import com.appBancaria.dto.RespuestaDTO;
import com.appBancaria.dto.SolicitudDTO;
import com.appBancaria.modelo.Cliente;
import com.appBancaria.db.DBConexion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;


public class Servidor {
    private final int PORT = 12345;
    private ServerSocket serverSocket;
    private boolean running = true;
    private final Gson gson = new Gson();
    private final GestorCuentas gestorCuentas = new GestorCuentas();
    
    // Lista thread-safe para mantener los clientes conectados
    private final List<ClienteConectado> clientesConectados = new CopyOnWriteArrayList<>();
    
    // Clase para almacenar información sobre clientes conectados
    public static class ClienteConectado {
        private final Socket socket;
        private String direccionIP;
        private String informacionCliente;
        private Date horaConexion;
        private String idSesion;
        private String correoUsuario;
        
        public ClienteConectado(Socket socket) {
            this.socket = socket;
            this.direccionIP = socket.getInetAddress().getHostAddress();
            this.horaConexion = new Date();
            this.informacionCliente = "Cliente sin identificar";
            this.idSesion = null;
            this.correoUsuario = null;
        }
        
        public Socket getSocket() {
            return socket;
        }
        
        public String getDireccionIP() {
            return direccionIP;
        }
        
        public Date getHoraConexion() {
            return horaConexion;
        }
        
        public String getInformacionCliente() {
            return informacionCliente;
        }
        
        public void setInformacionCliente(String informacionCliente) {
            this.informacionCliente = informacionCliente;
        }
        
        public String getIdSesion() {
            return idSesion;
        }
        
        public void setIdSesion(String idSesion) {
            this.idSesion = idSesion;
        }
        
        public String getCorreoUsuario() {
            return correoUsuario;
        }
        
        public void setCorreoUsuario(String correoUsuario) {
            this.correoUsuario = correoUsuario;
        }
        
        public boolean isSesionActiva() {
            // Ahora determinamos si hay una sesión activa basándonos en si hay un idSesion
            return idSesion != null;
        }
        
        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            return informacionCliente + " - IP: " + direccionIP + " - Conectado desde: " + sdf.format(horaConexion);
        }
    }
    
    // Método para obtener la lista de clientes conectados
    public List<ClienteConectado> getClientesConectados() {
        // Primero verificar las conexiones activas antes de devolver la lista
        verificarClientes();
        return new ArrayList<>(clientesConectados);
    }
    
    // Método para verificar qué clientes siguen conectados
    private void verificarClientes() {
        // Usar Iterator para evitar ConcurrentModificationException
        Iterator<ClienteConectado> iterator = clientesConectados.iterator();
        while (iterator.hasNext()) {
            ClienteConectado cliente = iterator.next();
            Socket socket = cliente.getSocket();
            
            // Verificar si el socket está cerrado o no está conectado
            if (socket.isClosed() || !socket.isConnected() || socket.isInputShutdown()) {
                log("Detectado cliente desconectado durante verificación: " + cliente.getDireccionIP());
                
                // Verificar si el cliente tenía una sesión activa
                if (cliente.isSesionActiva() && cliente.getCorreoUsuario() != null && cliente.getIdSesion() != null) {
                    try {
                        // Cerrar la sesión en la base de datos
                        log("Cerrando sesión de usuario desconectado: " + cliente.getCorreoUsuario());
                        gestorCuentas.cerrarSesion(cliente.getCorreoUsuario(), cliente.getIdSesion());
                        log("Sesión cerrada exitosamente para: " + cliente.getCorreoUsuario());
                    } catch (SQLException e) {
                        logError("Error al cerrar sesión de cliente desconectado: " + e.getMessage());
                    }
                }
                
                clientesConectados.remove(cliente);
                log("Cliente removido de la lista en verificarClientes: " + cliente.getDireccionIP());
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    logError("Error cerrando socket: " + e.getMessage());
                }
            }
        }
    }
    
    public void iniciar() {
        try {
            // Test database connection before starting the server
            DBConexion.getInstance().getConnection();
            log("Database connection established successfully!");
            
            iniciarServidor();
            
            // Iniciar temporizador para verificar clientes periódicamente
            iniciarVerificadorClientes();
        } catch (SQLException e) {
            logError("Failed to connect to database: " + e.getMessage());
            return; // Don't start server if database connection fails
        }
    }

    // Método que inicia un temporizador para verificar las conexiones
    private void iniciarVerificadorClientes() {
        Thread verificador = new Thread(() -> {
            while (running) {
                try {
                    // Verificar cada 5 segundos
                    Thread.sleep(5000);
                    verificarClientes();
                } catch (InterruptedException e) {
                    if (running) {
                        logError("Verificador de clientes interrumpido: " + e.getMessage());
                    }
                    break;
                }
            }
        });
        verificador.setDaemon(true);
        verificador.start();
        log("Verificador de clientes iniciado correctamente");
    }

    private void iniciarServidor() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(PORT);
                    log("Server started on port " + PORT);
                    log("Waiting for client connections...");

                    while (running) {
                        Socket clientSocket = serverSocket.accept();
                        // Crear el objeto ClienteConectado y añadirlo a la lista
                        ClienteConectado clienteConectado = new ClienteConectado(clientSocket);
                        clientesConectados.add(clienteConectado);
                        
                        // Create a new thread for each client
                        new Thread(new ClientHandler(clientSocket, clienteConectado)).start();
                    }
                } catch (IOException ex) {
                    if (running) {
                        logError("Server error: " + ex.getMessage());
                        ex.printStackTrace();
                    } else {
                        log("Server socket closed");
                    }
                }
            }
        }).start();
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private ClienteConectado clienteConectado;

        public ClientHandler(Socket socket, ClienteConectado clienteConectado) {
            this.clientSocket = socket;
            this.clienteConectado = clienteConectado;
        }

        @Override
        public void run() {
            try {
                log("New client connected: " + clientSocket.getInetAddress());
                
                // Initialize input/output streams
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                
                // Keep processing requests while the connection is alive
                while (!clientSocket.isClosed()) {
                    try {
                        // Process client request
                        RespuestaDTO respuesta = procesarSolicitud();
                        
                        // Si la respuesta es null, significa que el cliente se ha desconectado
                        if (respuesta == null) {
                            log("Cliente desconectado o error de lectura: " + clientSocket.getInetAddress());
                            break;
                        }
                        
                        // Send response back to client
                        out.println(gson.toJson(respuesta));
                        
                    } catch (SocketException se) {
                        // Si hay un problema con el socket, rompemos el bucle
                        log("Socket error con cliente " + clientSocket.getInetAddress() + ": " + se.getMessage());
                        break;
                    } catch (IOException ioe) {
                        // Si hay un error de I/O, rompemos el bucle
                        log("Error I/O con cliente " + clientSocket.getInetAddress() + ": " + ioe.getMessage());
                        break;
                    } catch (Exception e) {
                        // Log del error pero continuamos el bucle para otras solicitudes
                        logError("Error procesando solicitud del cliente " + clientSocket.getInetAddress() + ": " + e.getMessage());
                        // Si el error es grave, podríamos enviar una respuesta de error al cliente
                        RespuestaDTO errorResponse = new RespuestaDTO();
                        errorResponse.setCodigo(500);
                        errorResponse.setMensaje("Error interno del servidor: " + e.getMessage());
                        out.println(gson.toJson(errorResponse));
                    }
                }
                
            } catch (IOException e) {
                log("Error de conexión con cliente: " + clientSocket.getInetAddress());
                logError("Detalles: " + e.getMessage());
            } finally {
                try {
                    // Cerrar sesión si el cliente tenía una sesión activa
                    if (clienteConectado.isSesionActiva() && clienteConectado.getCorreoUsuario() != null && clienteConectado.getIdSesion() != null) {
                        try {
                            log("Cerrando sesión de cliente desconectado: " + clienteConectado.getCorreoUsuario());
                            gestorCuentas.cerrarSesion(clienteConectado.getCorreoUsuario(), clienteConectado.getIdSesion());
                            log("Sesión cerrada exitosamente para: " + clienteConectado.getCorreoUsuario());
                        } catch (SQLException ex) {
                            logError("Error al cerrar sesión en cierre de cliente: " + ex.getMessage());
                        }
                    }
                    
                    // Eliminar el cliente de la lista cuando se desconecta
                    clientesConectados.remove(clienteConectado);
                    log("Cliente removido de la lista de conexiones activas: " + clienteConectado.getDireccionIP());
                    
                    if (out != null) out.close();
                    if (in != null) in.close();
                    if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
                } catch (IOException e) {
                    logError("Error closing client connection: " + e.getMessage());
                }
            }
        }

        public RespuestaDTO procesarSolicitud() throws IOException {
            // Leer la solicitud del cliente
            String jsonRequest = in.readLine();
            
            // Si jsonRequest es null, el cliente se ha desconectado
            if (jsonRequest == null) {
                return null;
            }

            SolicitudDTO solicitud = gson.fromJson(jsonRequest, SolicitudDTO.class);
            if (solicitud == null || solicitud.getTipoOperacion() == null) {
                // Handle invalid request format
                RespuestaDTO errorResponse = new RespuestaDTO();
                errorResponse.setCodigo(400);
                errorResponse.setMensaje("Invalid request format");
                return errorResponse;
            }
            log("Request received: " + solicitud.getTipoOperacion());
            
            RespuestaDTO respuesta = new RespuestaDTO();
            
            switch (solicitud.getTipoOperacion().toLowerCase()) {

                case "ping":
                    // Nueva implementación para responder al ping
                    respuesta.setCodigo(200);
                    respuesta.setMensaje("pong");
                    // Puedes incluir un mapa vacío o datos adicionales si lo deseas
                    respuesta.setDatos(new HashMap<>());
                    log("Ping request processed successfully");
                    break;

                case "consulta_saldo":
                    try {
                        // Verificar el token JWT primero
                        String token = (String) solicitud.getDatos().get("token");
                        Map<String, Object> tokenInfo = validarYObtenerInfoToken(token);
                        
                        if (tokenInfo == null) {
                            respuesta.setCodigo(401);
                            respuesta.setMensaje("Token inválido, expirado o sesión no coincidente");
                            log("Intento de consulta de saldo con token inválido");
                            return respuesta;
                        }
                        
                        String sessionId = (String) tokenInfo.get("sessionId");
                        
                        // Ahora consultamos el saldo usando el sessionId extraído del token
                        double saldo;
                        if (solicitud.getDatos().containsKey("numeroCuenta")) {
                            String numeroCuenta = (String) solicitud.getDatos().get("numeroCuenta");
                            log("Processing saldo request for account: " + numeroCuenta);
                            saldo = gestorCuentas.consultarSaldo(numeroCuenta);
                            respuesta.setCodigo(200);
                            respuesta.setMensaje("Consulta de saldo exitosa");
                            Map<String, Object> datos = new HashMap<>();
                            datos.put("saldo", saldo);
                            respuesta.setDatos(datos);
                            log("Saldo request successful for account: " + numeroCuenta);
                        } else if (solicitud.getDatos().containsKey("identificacion")) {
                            int identificacion = ((Number) solicitud.getDatos().get("identificacion")).intValue();
                            log("Processing saldo request for ID: " + identificacion);
                            saldo = gestorCuentas.consultarSaldo(identificacion);
                            respuesta.setCodigo(200);
                            respuesta.setMensaje("Consulta de saldo exitosa");
                            Map<String, Object> datos = new HashMap<>();
                            datos.put("saldo", saldo);
                            respuesta.setDatos(datos);
                            log("Saldo request successful for ID: " + identificacion);
                        }
                    } catch (Exception e) {
                        respuesta.setCodigo(400);
                        respuesta.setMensaje("Error: " + e.getMessage());
                        logError("Error processing saldo request: " + e.getMessage());
                    }
                    break;

                case "crear_cuenta":
                case "registrar_usuario":
                    try {
                        Map<String, Object> datosCliente = solicitud.getDatos();
                        Cliente nuevoCliente = new Cliente();
                        nuevoCliente.setNombre((String) datosCliente.get("nombre"));
                        nuevoCliente.setIdentificacion(((Number) datosCliente.get("identificacion")).intValue());
                        nuevoCliente.setCorreo((String) datosCliente.get("correo"));
                        nuevoCliente.setContrasena((String) datosCliente.get("contrasena"));
                        
                        log("Attempting to create account for: " + nuevoCliente.getNombre() + " with email: " + nuevoCliente.getCorreo());
                        String numeroCuenta = gestorCuentas.crearCuenta(nuevoCliente);
                        
                        respuesta.setCodigo(201);
                        respuesta.setMensaje("Cuenta creada exitosamente");
                        Map<String, Object> datos = new HashMap<>();
                        datos.put("numeroCuenta", numeroCuenta);
                        datos.put("titular", nuevoCliente.getNombre());
                        respuesta.setDatos(datos);
                        log("Account successfully created for: " + nuevoCliente.getNombre() + " with account number: " + numeroCuenta);
                    } catch (Exception e) {
                        respuesta.setCodigo(400);
                        respuesta.setMensaje("Error al crear cuenta: " + e.getMessage());
                        logError("Error creating account: " + e.getMessage());
                    }
                    break;

                case "consigna_cuenta":
                    try {
                        Map<String, Object> datosConsignacion = solicitud.getDatos();
                        
                        // Verificar el token JWT
                        String token = (String) datosConsignacion.get("token");
                        Map<String, Object> tokenInfo = validarYObtenerInfoToken(token);
                        
                        if (tokenInfo == null) {
                            respuesta.setCodigo(401);
                            respuesta.setMensaje("Token inválido, expirado o sesión no coincidente");
                            log("Intento de consignación con token inválido");
                            return respuesta;
                        }
                        
                        String sessionId = (String) tokenInfo.get("sessionId");
                        String numeroCuentaDestino = (String) datosConsignacion.get("numeroCuentaDestino");
                        double monto = ((Number) datosConsignacion.get("monto")).doubleValue();

                        // Usar el sessionId extraído del token para realizar la consignación
                        Map<String, Object> resultadoConsignacion = gestorCuentas.consignarCuenta(
                            sessionId, 
                            numeroCuentaDestino, 
                            monto
                        );

                        respuesta.setCodigo(200);
                        respuesta.setMensaje("Consignación procesada");
                        respuesta.setDatos(resultadoConsignacion);
                        log("Consignación exitosa desde sessionId: " + sessionId);
                    } catch (Exception e) {
                        respuesta.setCodigo(400);
                        respuesta.setMensaje("Error en la consignación: " + e.getMessage());
                        Map<String, Object> datosError = new HashMap<>();
                        datosError.put("error", e.getMessage());
                        respuesta.setDatos(datosError);
                        logError("Error en consignación: " + e.getMessage());
                    }
                    break;

                case "login":
                    try {
                        String correo = (String) solicitud.getDatos().get("correo");
                        String contrasena = (String) solicitud.getDatos().get("contrasena");

                        // Ya no bloqueamos el login si hay una sesión activa
                        // Simplemente autenticamos y generamos un nuevo token
                        
                        // Usar el método de autenticación con JWT
                        Map<String, Object> infoCliente = gestorCuentas.autenticarYObtenerInformacionCliente(correo, contrasena);
                        if (infoCliente != null) {
                            // Actualizar la información del cliente en el objeto ClienteConectado
                            String nombreCliente = (String) infoCliente.get("nombre");
                            String idSesion = (String) infoCliente.get("idSesion");
                            
                            // Guardar información de sesión en el ClienteConectado
                            clienteConectado.setInformacionCliente(nombreCliente + " (" + correo + ")");
                            clienteConectado.setCorreoUsuario(correo);
                            clienteConectado.setIdSesion(idSesion);
                            
                            respuesta.setCodigo(200);
                            respuesta.setMensaje("Autenticación exitosa");
                            respuesta.setDatos(infoCliente);
                            
                            log("Usuario autenticado exitosamente con JWT: " + nombreCliente);
                            
                            // Si había una sesión activa anterior, ha sido sobrescrita automáticamente
                            // en el método actualizarIdSesion de GestorCuentas
                            boolean teniaSessionActiva = gestorCuentas.verificarSesionActiva(correo);
                            if (teniaSessionActiva) {
                                log("Se ha invalidado una sesión anterior del usuario: " + correo);
                            }
                        } else {
                            respuesta.setCodigo(401);
                            respuesta.setMensaje("Correo o contraseña incorrectos");
                            log("Intento de autenticación fallido para: " + correo);
                        }
                    } catch (Exception e) {
                        respuesta.setCodigo(500);
                        respuesta.setMensaje("Error en la autenticación: " + e.getMessage());
                        logError("Error durante la autenticación: " + e.getMessage());
                    }
                    break;

                case "logout":
                    try {
                        String correo = (String) solicitud.getDatos().get("correo");
                        
                        // Verificar el token JWT
                        String token = (String) solicitud.getDatos().get("token");
                        Map<String, Object> tokenInfo = validarYObtenerInfoToken(token);
                        
                        if (tokenInfo == null) {
                            respuesta.setCodigo(401);
                            respuesta.setMensaje("Token inválido, expirado o sesión no coincidente");
                            log("Intento de logout con token inválido");
                            return respuesta;
                        }
                        
                        String sessionId = (String) tokenInfo.get("sessionId");
                        
                        // Cerrar sesión usando el correo y sessionId
                        gestorCuentas.cerrarSesion(correo, sessionId);
                        
                        // Actualizar la información del cliente cuando cierra sesión
                        clienteConectado.setInformacionCliente("Cliente sin identificar (sesión cerrada)");
                        
                        clienteConectado.setIdSesion(null);
                        
                        respuesta.setCodigo(200);
                        respuesta.setMensaje("Sesión cerrada exitosamente");
                        log("Sesión cerrada para usuario: " + correo);
                    } catch (Exception e) {
                        respuesta.setCodigo(500);
                        respuesta.setMensaje("Error al cerrar sesión: " + e.getMessage());
                        logError("Error al cerrar sesión: " + e.getMessage());
                    }
                    break;

                case "historial_transacciones":
                    try {
                        // Verificar el token JWT
                        String token = (String) solicitud.getDatos().get("token");
                        Map<String, Object> tokenInfo = validarYObtenerInfoToken(token);
                        
                        if (tokenInfo == null) {
                            respuesta.setCodigo(401);
                            respuesta.setMensaje("Token inválido, expirado o sesión no coincidente");
                            log("Intento de obtener historial con token inválido");
                            return respuesta;
                        }
                        
                        String sessionId = (String) tokenInfo.get("sessionId");
                        
                        // Usar el sessionId extraído del token
                        Map<String, Object> historial = gestorCuentas.obtenerHistorialTransacciones(sessionId);
                        respuesta.setCodigo(200);
                        respuesta.setMensaje("Historial de transacciones obtenido exitosamente");
                        respuesta.setDatos(historial);
                        log("Historial de transacciones obtenido para sessionId: " + sessionId);
                    } catch (Exception e) {
                        respuesta.setCodigo(500);
                        respuesta.setMensaje("Error al obtener historial de transacciones: " + e.getMessage());
                        logError("Error al obtener historial: " + e.getMessage());
                    }
                    break;

                case "obtener_informacion_cliente":
                    try {
                        // Verificar el token JWT
                        String token = (String) solicitud.getDatos().get("token");
                        Map<String, Object> tokenInfo = validarYObtenerInfoToken(token);
                        
                        if (tokenInfo == null) {
                            respuesta.setCodigo(401);
                            respuesta.setMensaje("Token inválido, expirado o sesión no coincidente");
                            log("Intento de obtener información de cliente con token inválido");
                            return respuesta;
                        }
                        
                        String sessionId = (String) tokenInfo.get("sessionId");
                        
                        // Usar el sessionId extraído del token
                        Map<String, Object> informacionCliente = gestorCuentas.obtenerInformacionCliente(sessionId);
                        respuesta.setCodigo(200);
                        respuesta.setMensaje("Información del cliente obtenida exitosamente");
                        respuesta.setDatos(informacionCliente);
                        log("Información de cliente obtenida para sessionId: " + sessionId);
                    } catch (Exception e) {
                        respuesta.setCodigo(500);
                        respuesta.setMensaje("Error al obtener información del cliente: " + e.getMessage());
                        logError("Error al obtener información del cliente: " + e.getMessage());
                    }
                    break;

                case "validar_token":
                    try {
                        String token = (String) solicitud.getDatos().get("token");
                        Map<String, Object> tokenInfo = validarYObtenerInfoToken(token);
                        
                        if (tokenInfo != null) {
                            String sessionId = (String) tokenInfo.get("sessionId");
                            // Información del cliente ya se obtuvo en validarYObtenerInfoToken()
                            Map<String, Object> infoCliente = (Map<String, Object>) tokenInfo.get("infoCliente");
                            
                            respuesta.setCodigo(200);
                            respuesta.setMensaje("Token válido");
                            respuesta.setDatos(infoCliente);
                            log("Token JWT validado exitosamente");
                        } else {
                            respuesta.setCodigo(401);
                            respuesta.setMensaje("Token inválido o expirado");
                            log("Intento de validar token JWT inválido");
                        }
                    } catch (Exception e) {
                        respuesta.setCodigo(500);
                        respuesta.setMensaje("Error al validar token: " + e.getMessage());
                        logError("Error al validar token JWT: " + e.getMessage());
                    }
                    break;

                default:
                    respuesta.setCodigo(400);
                    respuesta.setMensaje("Operación no soportada");
                    log("Unsupported operation: " + solicitud.getTipoOperacion());
                    break;
            }
            
            return respuesta;
        }
    }

    public void detener() {
        try {
            log("Stopping server...");
            running = false;
            
            // Ya no cerraremos las sesiones activas en la base de datos
            log("Cerrando conexiones de clientes...");
            int sesionesTotales = 0;
            
            for (ClienteConectado cliente : clientesConectados) {
                sesionesTotales++;
                
                // Cerrar el socket del cliente
                try {
                    Socket socket = cliente.getSocket();
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    logError("Error al cerrar socket de cliente durante apagado: " + e.getMessage());
                }
            }
            
            log("Total de conexiones: " + sesionesTotales);
            clientesConectados.clear();
            
            // Cerrar el socket del servidor
            if (serverSocket != null) serverSocket.close();
            
            // Cerrar la conexión a la base de datos
            DBConexion.getInstance().closeConnection();
            log("Database connection closed");
            log("Server stopped");
        } catch (IOException e) {
            logError("Error stopping server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Helper methods for logging with timestamps
    private void log(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("[" + sdf.format(new Date()) + "] " + message);
    }
    
    private void logError(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.err.println("[" + sdf.format(new Date()) + "] ERROR: " + message);
    }

    /**
     * Valida un token JWT y obtiene la información del token y del cliente asociado
     * @param token El token JWT a validar
     * @return Un mapa con la información del token y del cliente, o null si el token es inválido
     */
    private Map<String, Object> validarYObtenerInfoToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                logError("Token JWT nulo o vacío");
                return null;
            }
            
            // Validar el token con JWTUtil (verifica firma y expiración)
            Map<String, Object> tokenInfo = JWTUtil.validarToken(token);
            
            if (tokenInfo == null) {
                logError("Token JWT inválido o expirado");
                return null;
            }
            
            // Extraer el sessionId del token
            String sessionId = (String) tokenInfo.get("sessionId");
            
            try {
                // Verificar que el sessionId existe en la base de datos (coincidencia con id_sesion)
                Map<String, Object> infoCliente = gestorCuentas.obtenerInformacionCliente(sessionId);
                
                if (infoCliente == null) {
                    logError("No se encontró una sesión activa para el sessionId: " + sessionId);
                    return null;
                }
                
                // Si todo está correcto, agregar la información del cliente al resultado
                tokenInfo.put("infoCliente", infoCliente);
                
                return tokenInfo;
            } catch (SQLException e) {
                logError("Error al validar el sessionId: " + e.getMessage());
                return null;
            }
        } catch (Exception e) {
            logError("Error al validar el token JWT: " + e.getMessage());
            return null;
        }
    }
}
