package com.appBancaria.servicio;

import com.appBancaria.dto.RespuestaDTO;
import com.appBancaria.dto.SolicitudDTO;
import com.appBancaria.modelo.Cliente;
import com.appBancaria.db.DBConexion;

import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Servidor {
    private final int PORT = 12345;
    private ServerSocket serverSocket;
    private boolean running = true;
    private final Gson gson = new Gson();
    private final GestorCuentas gestorCuentas = new GestorCuentas();
    
    public void iniciar() {
        try {
            // Test database connection before starting the server
            DBConexion.getInstance().getConnection();
            log("Database connection established successfully!");
            
            iniciarServidor();
        } catch (SQLException e) {
            logError("Failed to connect to database: " + e.getMessage());
            return; // Don't start server if database connection fails
        }
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
                        // Create a new thread for each client
                        new Thread(new ClientHandler(clientSocket)).start();
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

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
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
                    // Process client request
                    RespuestaDTO respuesta = procesarSolicitud();
                    
                    // Send response back to client
                    out.println(gson.toJson(respuesta));
                }
                
            } catch (IOException e) {
                log("Client disconnected: " + clientSocket.getInetAddress());
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException e) {
                    logError("Error closing client connection: " + e.getMessage());
                }
            }
        }

        public RespuestaDTO procesarSolicitud() {
            try {
                // In procesarSolicitud method
                String jsonRequest = in.readLine();
                if (jsonRequest == null) {
                    // Handle null request
                    RespuestaDTO errorResponse = new RespuestaDTO();
                    errorResponse.setCodigo(400);
                    errorResponse.setMensaje("Empty request received");
                    return errorResponse;
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
                        try {
                            Map<String, Object> datosCliente = solicitud.getDatos();
                            Cliente nuevoCliente = new Cliente();
                            nuevoCliente.setNombre((String) datosCliente.get("nombre"));
                            nuevoCliente.setIdentificacion(((Number) datosCliente.get("identificacion")).intValue());
                            nuevoCliente.setCorreo((String) datosCliente.get("correo"));
                            nuevoCliente.setContrasena((String) datosCliente.get("contrasena"));
                            
                            String numeroCuenta = gestorCuentas.crearCuenta(nuevoCliente);
                            
                            respuesta.setCodigo(201);
                            respuesta.setMensaje("Cuenta creada exitosamente");
                            Map<String, Object> datos = new HashMap<>();
                            datos.put("numeroCuenta", numeroCuenta);
                            datos.put("titular", nuevoCliente.getNombre());
                            respuesta.setDatos(datos);
                        } catch (Exception e) {
                            respuesta.setCodigo(400);
                            respuesta.setMensaje("Error al crear cuenta: " + e.getMessage());
                        }
                        break;

                    case "consigna_cuenta":
                        try {
                            Map<String, Object> datosConsignacion = solicitud.getDatos();
                            String idSesion = (String) datosConsignacion.get("idSesion");
                            String numeroCuentaDestino = (String) datosConsignacion.get("numeroCuentaDestino");
                            double monto = ((Number) datosConsignacion.get("monto")).doubleValue();

                            Map<String, Object> resultadoConsignacion = gestorCuentas.consignarCuenta(
                                idSesion, 
                                numeroCuentaDestino, 
                                monto
                            );

                            respuesta.setCodigo(200);
                            respuesta.setMensaje("Consignación procesada");
                            respuesta.setDatos(resultadoConsignacion);
                        } catch (Exception e) {
                            respuesta.setCodigo(400);
                            respuesta.setMensaje("Error en la consignación: " + e.getMessage());
                            Map<String, Object> datosError = new HashMap<>();
                            datosError.put("error", e.getMessage());
                            respuesta.setDatos(datosError);
                        }
                        break;

                    case "login":
                        try {
                            String correo = (String) solicitud.getDatos().get("correo");
                            String contrasena = (String) solicitud.getDatos().get("contrasena");

                            if (gestorCuentas.verificarSesionActiva(correo)) {
                                respuesta.setCodigo(400);
                                respuesta.setMensaje("Usuario ya tiene una sesión activa");
                            } else {
                                // Usar el nuevo método para obtener toda la información del cliente
                                Map<String, Object> datosCliente = gestorCuentas.autenticarYObtenerInformacionCliente(correo, contrasena);
                                if (datosCliente != null) {
                                    respuesta.setCodigo(200);
                                    respuesta.setMensaje("Autenticación exitosa");
                                    respuesta.setDatos(datosCliente);
                                } else {
                                    respuesta.setCodigo(401);
                                    respuesta.setMensaje("Correo o contraseña incorrectos");
                                }
                            }
                        } catch (Exception e) {
                            respuesta.setCodigo(500);
                            respuesta.setMensaje("Error en la autenticación: " + e.getMessage());
                        }
                        break;

                    case "logout":
                        try {
                            String correo = (String) solicitud.getDatos().get("correo");
                            String idSesion = (String) solicitud.getDatos().get("idSesion");
                            gestorCuentas.cerrarSesion(correo, idSesion);
                            respuesta.setCodigo(200);
                            respuesta.setMensaje("Sesión cerrada exitosamente");
                        } catch (Exception e) {
                            respuesta.setCodigo(500);
                            respuesta.setMensaje("Error al cerrar sesión: " + e.getMessage());
                        }
                        break;

                    case "historial_transacciones":
                        try {
                            String idSesion = (String) solicitud.getDatos().get("idSesion");
                            Map<String, Object> historial = gestorCuentas.obtenerHistorialTransacciones(idSesion);
                            respuesta.setCodigo(200);
                            respuesta.setMensaje("Historial de transacciones obtenido exitosamente");
                            respuesta.setDatos(historial);
                        } catch (Exception e) {
                            respuesta.setCodigo(500);
                            respuesta.setMensaje("Error al obtener historial de transacciones: " + e.getMessage());
                        }
                        break;

                    case "obtener_informacion_cliente":
                        try {
                            String idSesion = (String) solicitud.getDatos().get("idSesion");
                            Map<String, Object> informacionCliente = gestorCuentas.obtenerInformacionCliente(idSesion);
                            respuesta.setCodigo(200);
                            respuesta.setMensaje("Información del cliente obtenida exitosamente");
                            respuesta.setDatos(informacionCliente);
                        } catch (Exception e) {
                            respuesta.setCodigo(500);
                            respuesta.setMensaje("Error al obtener información del cliente: " + e.getMessage());
                        }
                        break;

                    default:
                        respuesta.setCodigo(400);
                        respuesta.setMensaje("Operación no soportada");
                        log("Unsupported operation: " + solicitud.getTipoOperacion());
                        break;
                }
                
                return respuesta;
            } catch (IOException e) {
                RespuestaDTO errorResponse = new RespuestaDTO();
                errorResponse.setCodigo(500);
                errorResponse.setMensaje("Error procesando la solicitud: " + e.getMessage());
                logError("Error processing request: " + e.getMessage());
                return errorResponse;
            }
        }
    }

    public void detener() {
        try {
            log("Stopping server...");
            running = false;
            if (serverSocket != null) serverSocket.close();
            DBConexion.getInstance().closeConnection();  // Close DB connection when server stops
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
}
