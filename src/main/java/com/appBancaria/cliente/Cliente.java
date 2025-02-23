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
}