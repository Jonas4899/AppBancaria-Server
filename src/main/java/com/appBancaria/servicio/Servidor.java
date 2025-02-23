package com.appBancaria.servicio;

import com.appBancaria.dto.RespuestaDTO;
import com.appBancaria.dto.SolicitudDTO;
import com.appBancaria.modelo.Cliente;

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

public class Servidor {
    private final int PORT = 12345;
    private ServerSocket serverSocket;
    private boolean running = true;
    private final Gson gson = new Gson();
    private final GestorCuentas gestorCuentas = new GestorCuentas();
    
    public void iniciar() {
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
                String jsonRequest = in.readLine();
                SolicitudDTO solicitud = gson.fromJson(jsonRequest, SolicitudDTO.class);
                
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
