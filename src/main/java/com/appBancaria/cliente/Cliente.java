package com.appBancaria.cliente;

import com.appBancaria.dto.SolicitudDTO;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

public class Cliente {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        crearCuenta("Juan Perez", 12345678, "juan.perez@example.com", "password123");
        // testHistorialTransacciones();
    }

    private static void testHistorialTransacciones() {
        String correo = "yecid@ejemplo.com";
        String contrasena = "344";
        String idSesion = login(correo, contrasena);
        if (idSesion != null) {
            solicitarHistorialTransacciones(idSesion);
            logout(correo, idSesion);
        }
    }

    private static void crearCuenta(String nombre, int identificacion, String correo, String contrasena) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Crear el objeto SolicitudDTO
            Map<String, Object> datos = new HashMap<>();
            datos.put("nombre", nombre);
            datos.put("identificacion", identificacion);
            datos.put("correo", correo);
            datos.put("contrasena", contrasena);

            SolicitudDTO solicitud = new SolicitudDTO();
            solicitud.setTipoOperacion("crear_cuenta");
            solicitud.setDatos(datos);

            // Convertir a JSON usando Gson
            String jsonRequest = gson.toJson(solicitud);

            // Enviar solicitud al servidor
            System.out.println("Sending request: " + jsonRequest);
            out.println(jsonRequest);

            // Leer respuesta del servidor
            String response = in.readLine();
            System.out.println("Server response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String login(String correo, String contrasena) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Map<String, Object> datos = new HashMap<>();
            datos.put("correo", correo);
            datos.put("contrasena", contrasena);

            SolicitudDTO solicitud = new SolicitudDTO();
            solicitud.setTipoOperacion("login");
            solicitud.setDatos(datos);

            String jsonRequest = gson.toJson(solicitud);
            out.println(jsonRequest);

            String response = in.readLine();
            System.out.println("Login response: " + response);

            com.google.gson.JsonObject jsonResponse = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
            int codigo = jsonResponse.get("codigo").getAsInt();
            if (codigo == 200) {
                return jsonResponse.get("datos").getAsJsonObject().get("idSesion").getAsString();
            } else {
                System.out.println("Login failed: " + jsonResponse.get("mensaje").getAsString());
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void solicitarHistorialTransacciones(String idSesion) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Map<String, Object> datos = new HashMap<>();
            datos.put("idSesion", idSesion);

            SolicitudDTO solicitud = new SolicitudDTO();
            solicitud.setTipoOperacion("historial_transacciones");
            solicitud.setDatos(datos);

            String jsonRequest = gson.toJson(solicitud);
            out.println(jsonRequest);

            String response = in.readLine();
            System.out.println("Historial response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void logout(String correo, String idSesion) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Map<String, Object> datos = new HashMap<>();
            datos.put("correo", correo);
            datos.put("idSesion", idSesion);

            SolicitudDTO solicitud = new SolicitudDTO();
            solicitud.setTipoOperacion("logout");
            solicitud.setDatos(datos);

            String jsonRequest = gson.toJson(solicitud);
            out.println(jsonRequest);

            String response = in.readLine();
            System.out.println("Logout response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testConsultaSaldo() {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Create request using objects instead of raw JSON string
            Map<String, Object> datos = new HashMap<>();
            datos.put("identificacion", 1000795892);

            SolicitudDTO solicitud = new SolicitudDTO();
            solicitud.setTipoOperacion("consulta_saldo");
            solicitud.setDatos(datos);

            // Convert to JSON using Gson
            String jsonRequest = gson.toJson(solicitud);

            // Send request to server
            System.out.println("Sending request: " + jsonRequest);
            out.println(jsonRequest);

            // Read response from server
            String response = in.readLine();
            System.out.println("Server response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testConsignarCuenta() {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Create request for consignarCuenta
            Map<String, Object> datos = new HashMap<>();
            datos.put("numeroCuentaOrigen", "FB11A3F5");
            datos.put("numeroCuentaDestino", "9C630EB9");
            datos.put("monto", 666);

            SolicitudDTO solicitud = new SolicitudDTO();
            solicitud.setTipoOperacion("consigna_cuenta");
            solicitud.setDatos(datos);

            // Convert to JSON using Gson
            String jsonRequest = gson.toJson(solicitud);

            // Send request to server
            System.out.println("Sending consignment request: " + jsonRequest);
            out.println(jsonRequest);

            // Read response from server
            String response = in.readLine();
            System.out.println("Server response: " + response);
            
            // Parse and display the response in a more readable format
            System.out.println("\n--- TRANSACTION RESULT ---");
            com.google.gson.JsonObject jsonResponse = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
            int codigo = jsonResponse.get("codigo").getAsInt();
            String mensaje = jsonResponse.get("mensaje").getAsString();
            
            System.out.println("Status code: " + codigo);
            System.out.println("Message: " + mensaje);
            
            if (codigo == 200) {
                com.google.gson.JsonObject datos2 = jsonResponse.get("datos").getAsJsonObject();
                System.out.println("Transaction success: " + datos2.get("exito").getAsBoolean());
                System.out.println("Previous balance: " + datos2.get("saldoAnterior").getAsDouble());
                System.out.println("New balance: " + datos2.get("saldoNuevo").getAsDouble());
                System.out.println("Amount transferred: " + datos2.get("monto").getAsDouble());
                System.out.println("Destination account: " + datos2.get("numeroCuentaDestino").getAsString());
            }
            System.out.println("-------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}