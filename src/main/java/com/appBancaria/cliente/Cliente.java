package com.appBancaria.cliente;

import com.appBancaria.dto.SolicitudDTO;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Cliente {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final Gson gson = new Gson();
    
    // Constantes para la prueba de consignación
    private static final String NUMERO_CUENTA_DESTINO = "978476";
    private static final double MONTO_CONSIGNACION = 23000.0;

    public static void main(String[] args) {
        // Probar login JWT y luego consignación
        probarLoginYConsignacionJWT();
    }

    private static void probarLoginYConsignacionJWT() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("==== PRUEBA DE AUTENTICACIÓN JWT Y CONSIGNACIÓN ====");
        
        System.out.print("Correo electrónico: ");
        String correo = scanner.nextLine();
        
        System.out.print("Contraseña: ");
        String contrasena = scanner.nextLine();
        
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 1. REALIZAR LOGIN
            System.out.println("\n=== PASO 1: INICIANDO SESIÓN ===");
            
            // Preparar la solicitud de login
            Map<String, Object> datos = new HashMap<>();
            datos.put("correo", correo);
            datos.put("contrasena", contrasena);

            SolicitudDTO solicitud = new SolicitudDTO();
            solicitud.setTipoOperacion("login");
            solicitud.setDatos(datos);

            // Convertir la solicitud a JSON
            String jsonRequest = gson.toJson(solicitud);
            
            System.out.println("\n=== SOLICITUD ENVIADA AL SERVIDOR ===");
            System.out.println(jsonRequest);
            
            // Enviar solicitud de login al servidor
            out.println(jsonRequest);

            // Recibir y procesar la respuesta
            String jsonResponse = in.readLine();
            
            System.out.println("\n=== RESPUESTA DEL SERVIDOR ===");
            System.out.println(jsonResponse);
            
            // Procesar la respuesta como JSON
            JsonObject respuesta = JsonParser.parseString(jsonResponse).getAsJsonObject();
            int codigo = respuesta.get("codigo").getAsInt();
            String mensaje = respuesta.get("mensaje").getAsString();
            
            System.out.println("\n=== RESULTADO DE LA AUTENTICACIÓN ===");
            System.out.println("Código de respuesta: " + codigo);
            System.out.println("Mensaje: " + mensaje);
            
            if (codigo == 200) {
                JsonObject datosRespuesta = respuesta.get("datos").getAsJsonObject();
                
                System.out.println("\n¡Autenticación exitosa!");
                String token = datosRespuesta.get("token").getAsString();
                System.out.println("Token JWT: " + token);
                System.out.println("ID Sesión: " + datosRespuesta.get("idSesion").getAsString());
                System.out.println("Nombre: " + datosRespuesta.get("nombre").getAsString());
                System.out.println("Correo: " + datosRespuesta.get("correo").getAsString());
                System.out.println("Número de cuenta: " + datosRespuesta.get("numeroCuenta").getAsString());
                System.out.println("Saldo: $" + datosRespuesta.get("saldo").getAsDouble());
                
                // 2. REALIZAR CONSIGNACIÓN AUTOMÁTICAMENTE
                System.out.println("\n=== PASO 2: REALIZANDO CONSIGNACIÓN ===");
                System.out.println("Cuenta destino: " + NUMERO_CUENTA_DESTINO);
                System.out.println("Monto: $" + MONTO_CONSIGNACION);
                
                // Crear nueva solicitud para la consignación
                Map<String, Object> datosConsignacion = new HashMap<>();
                datosConsignacion.put("token", token); // Usar el token obtenido en el login
                datosConsignacion.put("numeroCuentaDestino", NUMERO_CUENTA_DESTINO);
                datosConsignacion.put("monto", MONTO_CONSIGNACION);
                
                SolicitudDTO solicitudConsignacion = new SolicitudDTO();
                solicitudConsignacion.setTipoOperacion("consigna_cuenta");
                solicitudConsignacion.setDatos(datosConsignacion);
                
                // Convertir la solicitud de consignación a JSON
                String jsonConsignacionRequest = gson.toJson(solicitudConsignacion);
                
                System.out.println("\n=== SOLICITUD DE CONSIGNACIÓN ENVIADA AL SERVIDOR ===");
                System.out.println(jsonConsignacionRequest);
                
                // Enviar solicitud de consignación al servidor
                out.println(jsonConsignacionRequest);
                
                // Recibir y procesar la respuesta de la consignación
                String jsonConsignacionResponse = in.readLine();
                
                System.out.println("\n=== RESPUESTA DEL SERVIDOR ===");
                System.out.println(jsonConsignacionResponse);
                
                // Procesar la respuesta de la consignación como JSON
                JsonObject respuestaConsignacion = JsonParser.parseString(jsonConsignacionResponse).getAsJsonObject();
                int codigoConsignacion = respuestaConsignacion.get("codigo").getAsInt();
                String mensajeConsignacion = respuestaConsignacion.get("mensaje").getAsString();
                
                System.out.println("\n=== RESULTADO DE LA CONSIGNACIÓN ===");
                System.out.println("Código de respuesta: " + codigoConsignacion);
                System.out.println("Mensaje: " + mensajeConsignacion);
                
                if (codigoConsignacion == 200) {
                    JsonObject datosConsignacionRespuesta = respuestaConsignacion.get("datos").getAsJsonObject();
                    System.out.println("\n¡Consignación exitosa!");
                    System.out.println("Cuenta origen: " + datosConsignacionRespuesta.get("numeroCuentaOrigen").getAsString());
                    System.out.println("Cuenta destino: " + datosConsignacionRespuesta.get("numeroCuentaDestino").getAsString());
                    System.out.println("Monto: $" + datosConsignacionRespuesta.get("monto").getAsDouble());
                    System.out.println("Saldo anterior: $" + datosConsignacionRespuesta.get("saldoAnterior").getAsDouble());
                    System.out.println("Saldo nuevo: $" + datosConsignacionRespuesta.get("saldoNuevo").getAsDouble());
                } else {
                    System.out.println("\n¡Error en la consignación!");
                }
            } else {
                System.out.println("\n¡Error en la autenticación! No se pudo realizar la consignación.");
            }
            
        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
            e.printStackTrace();
        }
        
        scanner.close();
    }
}