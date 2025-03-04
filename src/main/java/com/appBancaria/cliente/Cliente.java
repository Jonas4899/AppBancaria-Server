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
    private static final String NUMERO_CUENTA_DESTINO = "586522";
    private static final double MONTO_CONSIGNACION = 23000.0;

    public static void main(String[] args) {
        // Probar login JWT, consulta de saldo y consignación
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== MENÚ DE PRUEBAS ===");
        System.out.println("1. Probar login, consulta de saldo y consignación");
        System.out.println("2. Probar consulta de saldo de otra cuenta (debería fallar)");
        System.out.print("Seleccione una opción: ");
        
        int opcion = 1;
        try {
            opcion = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            opcion = 1;
        }
        
        switch (opcion) {
            case 1:
                probarLoginYConsignacionJWT(scanner);
                break;
            case 2:
                probarConsultaSaldoAjena(scanner);
                break;
            default:
                System.out.println("Opción inválida, ejecutando prueba estándar");
                probarLoginYConsignacionJWT(scanner);
        }
        
        scanner.close();
    }

    private static void probarLoginYConsignacionJWT(Scanner scanner) {
        System.out.println("==== PRUEBA DE AUTENTICACIÓN JWT, CONSULTA DE SALDO Y CONSIGNACIÓN ====");
        
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
                String numeroCuenta = datosRespuesta.get("numeroCuenta").getAsString();
                String identificacion = datosRespuesta.get("identificacion").getAsString();
                
                System.out.println("Token JWT: " + token);
                System.out.println("ID Sesión: " + datosRespuesta.get("idSesion").getAsString());
                System.out.println("Nombre: " + datosRespuesta.get("nombre").getAsString());
                System.out.println("Correo: " + datosRespuesta.get("correo").getAsString());
                System.out.println("Número de cuenta: " + numeroCuenta);
                System.out.println("Identificación: " + identificacion);
                System.out.println("Saldo: $" + datosRespuesta.get("saldo").getAsDouble());
                
                // 2. CONSULTAR SALDO USANDO NÚMERO DE CUENTA
                System.out.println("\n=== PASO 2: CONSULTANDO SALDO USANDO NÚMERO DE CUENTA ===");
                
                // Crear nueva solicitud para consulta de saldo por número de cuenta
                Map<String, Object> datosConsultaCuenta = new HashMap<>();
                datosConsultaCuenta.put("token", token); // Usar el token obtenido en el login
                datosConsultaCuenta.put("numeroCuenta", numeroCuenta);
                
                SolicitudDTO solicitudConsultaCuenta = new SolicitudDTO();
                solicitudConsultaCuenta.setTipoOperacion("consulta_saldo");
                solicitudConsultaCuenta.setDatos(datosConsultaCuenta);
                
                String jsonConsultaCuentaRequest = gson.toJson(solicitudConsultaCuenta);
                
                System.out.println("\n=== SOLICITUD DE CONSULTA DE SALDO ENVIADA AL SERVIDOR ===");
                System.out.println(jsonConsultaCuentaRequest);
                
                // Enviar solicitud de consulta al servidor
                out.println(jsonConsultaCuentaRequest);
                
                // Recibir y procesar la respuesta de la consulta
                String jsonConsultaCuentaResponse = in.readLine();
                
                System.out.println("\n=== RESPUESTA DE CONSULTA POR NÚMERO DE CUENTA ===");
                System.out.println(jsonConsultaCuentaResponse);
                
                JsonObject respuestaConsultaCuenta = JsonParser.parseString(jsonConsultaCuentaResponse).getAsJsonObject();
                int codigoConsulta = respuestaConsultaCuenta.get("codigo").getAsInt();
                String mensajeConsulta = respuestaConsultaCuenta.get("mensaje").getAsString();
                
                System.out.println("\n=== RESULTADO DE LA CONSULTA POR NÚMERO DE CUENTA ===");
                System.out.println("Código de respuesta: " + codigoConsulta);
                System.out.println("Mensaje: " + mensajeConsulta);
                
                if (codigoConsulta == 200) {
                    JsonObject datosConsultaRespuesta = respuestaConsultaCuenta.get("datos").getAsJsonObject();
                    System.out.println("\n¡Consulta exitosa!");
                    System.out.println("Saldo: $" + datosConsultaRespuesta.get("saldo").getAsDouble());
                } else {
                    System.out.println("\n¡Error en la consulta por número de cuenta!");
                }
                
                // 3. CONSULTAR SALDO USANDO IDENTIFICACIÓN
                System.out.println("\n=== PASO 3: CONSULTANDO SALDO USANDO IDENTIFICACIÓN ===");
                
                // Crear nueva solicitud para consulta de saldo por identificación
                Map<String, Object> datosConsultaId = new HashMap<>();
                datosConsultaId.put("token", token); // Usar el token obtenido en el login
                datosConsultaId.put("identificacion", Integer.parseInt(identificacion));
                
                SolicitudDTO solicitudConsultaId = new SolicitudDTO();
                solicitudConsultaId.setTipoOperacion("consulta_saldo");
                solicitudConsultaId.setDatos(datosConsultaId);
                
                String jsonConsultaIdRequest = gson.toJson(solicitudConsultaId);
                
                System.out.println("\n=== SOLICITUD DE CONSULTA DE SALDO POR ID ENVIADA AL SERVIDOR ===");
                System.out.println(jsonConsultaIdRequest);
                
                // Enviar solicitud de consulta al servidor
                out.println(jsonConsultaIdRequest);
                
                // Recibir y procesar la respuesta de la consulta
                String jsonConsultaIdResponse = in.readLine();
                
                System.out.println("\n=== RESPUESTA DE CONSULTA POR IDENTIFICACIÓN ===");
                System.out.println(jsonConsultaIdResponse);
                
                JsonObject respuestaConsultaId = JsonParser.parseString(jsonConsultaIdResponse).getAsJsonObject();
                int codigoConsultaId = respuestaConsultaId.get("codigo").getAsInt();
                String mensajeConsultaId = respuestaConsultaId.get("mensaje").getAsString();
                
                System.out.println("\n=== RESULTADO DE LA CONSULTA POR IDENTIFICACIÓN ===");
                System.out.println("Código de respuesta: " + codigoConsultaId);
                System.out.println("Mensaje: " + mensajeConsultaId);
                
                if (codigoConsultaId == 200) {
                    JsonObject datosConsultaIdRespuesta = respuestaConsultaId.get("datos").getAsJsonObject();
                    System.out.println("\n¡Consulta por identificación exitosa!");
                    System.out.println("Saldo: $" + datosConsultaIdRespuesta.get("saldo").getAsDouble());
                } else {
                    System.out.println("\n¡Error en la consulta por identificación!");
                }
                
                // 4. REALIZAR CONSIGNACIÓN
                System.out.println("\n=== PASO 4: REALIZANDO CONSIGNACIÓN ===");
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
                System.out.println("\n¡Error en la autenticación! No se pudieron realizar las operaciones.");
            }
            
        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void probarConsultaSaldoAjena(Scanner scanner) {
        System.out.println("==== PRUEBA DE CONSULTA DE SALDO DE OTRA CUENTA (CASO NEGATIVO) ====");
        
        System.out.print("Correo electrónico: ");
        String correo = scanner.nextLine();
        
        System.out.print("Contraseña: ");
        String contrasena = scanner.nextLine();
        
        System.out.print("Número de cuenta AJENA a consultar: ");
        String numeroCuentaAjena = scanner.nextLine();
        
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
                System.out.println("Número de cuenta del usuario: " + datosRespuesta.get("numeroCuenta").getAsString());
                
                // 2. INTENTAR CONSULTAR SALDO DE CUENTA AJENA
                System.out.println("\n=== PASO 2: INTENTANDO CONSULTAR SALDO DE CUENTA AJENA ===");
                System.out.println("Intentando acceder a la cuenta: " + numeroCuentaAjena);
                
                // Crear solicitud para consulta de cuenta ajena
                Map<String, Object> datosConsultaAjena = new HashMap<>();
                datosConsultaAjena.put("token", token); // Usar el token obtenido en el login
                datosConsultaAjena.put("numeroCuenta", numeroCuentaAjena);
                
                SolicitudDTO solicitudConsultaAjena = new SolicitudDTO();
                solicitudConsultaAjena.setTipoOperacion("consulta_saldo");
                solicitudConsultaAjena.setDatos(datosConsultaAjena);
                
                String jsonConsultaAjenaRequest = gson.toJson(solicitudConsultaAjena);
                
                System.out.println("\n=== SOLICITUD DE CONSULTA ENVIADA AL SERVIDOR ===");
                System.out.println(jsonConsultaAjenaRequest);
                
                // Enviar solicitud de consulta al servidor
                out.println(jsonConsultaAjenaRequest);
                
                // Recibir y procesar la respuesta 
                String jsonConsultaAjenaResponse = in.readLine();
                
                System.out.println("\n=== RESPUESTA DEL SERVIDOR ===");
                System.out.println(jsonConsultaAjenaResponse);
                
                JsonObject respuestaConsultaAjena = JsonParser.parseString(jsonConsultaAjenaResponse).getAsJsonObject();
                int codigoConsultaAjena = respuestaConsultaAjena.get("codigo").getAsInt();
                String mensajeConsultaAjena = respuestaConsultaAjena.get("mensaje").getAsString();
                
                System.out.println("\n=== RESULTADO DE LA CONSULTA DE CUENTA AJENA ===");
                System.out.println("Código de respuesta: " + codigoConsultaAjena);
                System.out.println("Mensaje: " + mensajeConsultaAjena);
                
                if (codigoConsultaAjena == 403) {
                    System.out.println("\n¡PRUEBA EXITOSA! Se denegó correctamente el acceso a la cuenta ajena.");
                } else if (codigoConsultaAjena == 200) {
                    System.out.println("\n¡PRUEBA FALLIDA! Se permitió el acceso a una cuenta ajena.");
                    JsonObject datosConsultaRespuesta = respuestaConsultaAjena.get("datos").getAsJsonObject();
                    System.out.println("Saldo de la cuenta ajena: $" + datosConsultaRespuesta.get("saldo").getAsDouble());
                    System.out.println("Esto representa una vulnerabilidad de seguridad.");
                } else {
                    System.out.println("\nResultado inesperado con código " + codigoConsultaAjena);
                }
            } else {
                System.out.println("\n¡Error en la autenticación! No se pudo realizar la prueba.");
            }
            
        } catch (Exception e) {
            System.err.println("Error de conexión: " + e.getMessage());
            e.printStackTrace();
        }
    }
}