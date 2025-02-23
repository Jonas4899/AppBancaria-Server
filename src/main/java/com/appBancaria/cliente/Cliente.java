package com.appBancaria.cliente;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Example: Create account request
            /*
            String createAccountRequest = "{"
                + "\"tipoOperacion\": \"crear_cuenta\","
                + "\"datos\": {"
                + "\"nombre\": \"Jonathan Salcedo\","
                + "\"identificacion\": 1000795892,"
                + "\"correo\": \"jsalcedod@ucentral.edu.com\","
                + "\"contrasena\": \"Yoasobi21\""
                + "}"
                + "}";
            */
            // To check balance by account number
            /*
            String checkBalanceRequest = "{"
                    + "\"tipoOperacion\": \"consulta_saldo\","
                    + "\"datos\": {"
                    + "\"numeroCuenta\": \"0436036640\""
                    + "}"
                    + "}";
            */
            // To check balance by identification (uncomment to use)

            String checkBalanceRequest = "{"
                    + "\"tipoOperacion\": \"consulta_saldo\","
                    + "\"datos\": {"
                    + "\"identificacion\": 1000795892"
                    + "}"
                    + "}";

            // Send request to server
            System.out.println("Sending request: " + checkBalanceRequest);
            out.println(checkBalanceRequest);

            // Read response from server
            String response = in.readLine();
            System.out.println("Server response: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}