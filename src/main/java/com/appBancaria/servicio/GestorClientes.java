package com.appBancaria.servicio;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clase que gestiona la lista de clientes conectados al servidor.
 */
public class GestorClientes {
    private final List<ClienteConectado> clientesConectados = new CopyOnWriteArrayList<>();
    private final GestorCuentas gestorCuentas;
    private boolean running = true;
    
    /**
     * Constructor para GestorClientes
     */
    public GestorClientes(GestorCuentas gestorCuentas) {
        this.gestorCuentas = gestorCuentas;
    }
    
    /**
     * Agrega un cliente a la lista de clientes conectados
     * @param clienteConectado El cliente a agregar
     */
    public void agregarCliente(ClienteConectado clienteConectado) {
        clientesConectados.add(clienteConectado);
    }
    
    /**
     * Elimina un cliente de la lista de clientes conectados
     * @param clienteConectado El cliente a eliminar
     */
    public void removerCliente(ClienteConectado clienteConectado) {
        clientesConectados.remove(clienteConectado);
    }
    
    /**
     * Obtiene la lista de clientes conectados
     * @return Lista de clientes conectados
     */
    public List<ClienteConectado> getClientesConectados() {
        // Primero verificar las conexiones activas antes de devolver la lista
        verificarClientes();
        return new ArrayList<>(clientesConectados);
    }
    
    /**
     * Verifica si hay clientes que ya no están conectados y los elimina
     */
    public void verificarClientes() {
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
    
    /**
     * Inicia el verificador de clientes que ejecuta verificarClientes periódicamente
     */
    public void iniciarVerificadorClientes() {
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
    
    /**
     * Detiene el verificador de clientes y cierra todas las conexiones
     */
    public void detenerVerificador() {
        running = false;
    }
    
    /**
     * Cierra todas las conexiones de clientes
     * @return Número de conexiones cerradas
     */
    public int cerrarTodasLasConexiones() {
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
        
        clientesConectados.clear();
        return sesionesTotales;
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
