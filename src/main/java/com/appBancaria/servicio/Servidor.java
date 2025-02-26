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
            System.out.println("Database connection established successfully!");
            
            iniciarServidor();
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            return; // Don't start server if database connection fails
        }
    }

    private void iniciarServidor() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(PORT);
                    System.out.println("Server started on port " + PORT);

                    while (running) {
                        Socket clientSocket = serverSocket.accept();
                        // Create a new thread for each client
                        new Thread(new ClientHandler(clientSocket)).start();
                    }
                } catch (IOException ex) {
                    if (running) {
                        ex.printStackTrace();
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
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
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
                System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            } finally {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
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
                System.out.println(solicitud.getTipoOperacion());
                
                RespuestaDTO respuesta = new RespuestaDTO();
                
                switch (solicitud.getTipoOperacion().toLowerCase()) {

                    case "ping":
                        // Nueva implementación para responder al ping
                        respuesta.setCodigo(200);
                        respuesta.setMensaje("pong");
                        // Puedes incluir un mapa vacío o datos adicionales si lo deseas
                        respuesta.setDatos(new HashMap<>());
                        break;

                    case "consulta_saldo":
                        try {
                            double saldo;
                            if (solicitud.getDatos().containsKey("numeroCuenta")) {
                                String numeroCuenta = (String) solicitud.getDatos().get("numeroCuenta");
                                saldo = gestorCuentas.consultarSaldo(numeroCuenta);
                                respuesta.setCodigo(200);
                                respuesta.setMensaje("Consulta de saldo exitosa");
                                Map<String, Object> datos = new HashMap<>();
                                datos.put("saldo", saldo);
                                respuesta.setDatos(datos);
                            } else if (solicitud.getDatos().containsKey("identificacion")) {
                                int identificacion = ((Number) solicitud.getDatos().get("identificacion")).intValue();
                                saldo = gestorCuentas.consultarSaldo(identificacion);
                                respuesta.setCodigo(200);
                                respuesta.setMensaje("Consulta de saldo exitosa");
                                Map<String, Object> datos = new HashMap<>();
                                datos.put("saldo", saldo);
                                respuesta.setDatos(datos);
                            }
                        } catch (Exception e) {
                            respuesta.setCodigo(400);
                            respuesta.setMensaje("Error: " + e.getMessage());
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
                                String idSesion = gestorCuentas.autenticarUsuario(correo, contrasena);
                                if (idSesion != null) {
                                    respuesta.setCodigo(200);
                                    respuesta.setMensaje("Autenticación exitosa");
                                    Map<String, Object> datos = new HashMap<>();
                                    datos.put("idSesion", idSesion);
                                    datos.put("correo", correo);  // Adding the email to the response data
                                    respuesta.setDatos(datos);
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

                    default:
                        respuesta.setCodigo(400);
                        respuesta.setMensaje("Operación no soportada");
                        break;
                }
                
                return respuesta;
            } catch (IOException e) {
                RespuestaDTO errorResponse = new RespuestaDTO();
                errorResponse.setCodigo(500);
                errorResponse.setMensaje("Error procesando la solicitud: " + e.getMessage());
                return errorResponse;
            }
        }
    }

    public void detener() {
        try {
            running = false;
            if (serverSocket != null) serverSocket.close();
            DBConexion.getInstance().closeConnection();  // Close DB connection when server stops
            System.out.println("\nApagando servidor... ;(");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
