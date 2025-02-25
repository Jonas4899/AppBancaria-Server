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
        testConsultaSaldo();
        // testConsignarCuenta();
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
            datos.put("monto", 50555);

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