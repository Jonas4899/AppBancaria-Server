package com.appBancaria.servicio;

import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Clase para almacenar información sobre clientes conectados al servidor.
 * Mantiene datos como el socket, la dirección IP, información del cliente,
 * hora de conexión y datos de sesión.
 */
public class ClienteConectado {
    private final Socket socket;
    private String direccionIP;
    private String informacionCliente;
    private Date horaConexion;
    private String idSesion;
    private String correoUsuario;
    
    /**
     * Constructor que inicializa un cliente conectado con su socket.
     * 
     * @param socket Socket de conexión del cliente
     */
    public ClienteConectado(Socket socket) {
        this.socket = socket;
        this.direccionIP = socket.getInetAddress().getHostAddress();
        this.horaConexion = new Date();
        this.informacionCliente = "Cliente sin identificar";
        this.idSesion = null;
        this.correoUsuario = null;
    }
    
    /**
     * Obtiene el socket del cliente conectado.
     * 
     * @return Socket del cliente
     */
    public Socket getSocket() {
        return socket;
    }
    
    /**
     * Obtiene la dirección IP del cliente.
     * 
     * @return Dirección IP como String
     */
    public String getDireccionIP() {
        return direccionIP;
    }
    
    /**
     * Obtiene la hora de conexión del cliente.
     * 
     * @return Fecha y hora de la conexión
     */
    public Date getHoraConexion() {
        return horaConexion;
    }
    
    /**
     * Obtiene la información descriptiva del cliente.
     * 
     * @return Información del cliente
     */
    public String getInformacionCliente() {
        return informacionCliente;
    }
    
    /**
     * Establece la información descriptiva del cliente.
     * 
     * @param informacionCliente Nueva información del cliente
     */
    public void setInformacionCliente(String informacionCliente) {
        this.informacionCliente = informacionCliente;
    }
    
    /**
     * Obtiene el ID de sesión del cliente.
     * 
     * @return ID de sesión
     */
    public String getIdSesion() {
        return idSesion;
    }
    
    /**
     * Establece el ID de sesión del cliente.
     * 
     * @param idSesion Nuevo ID de sesión
     */
    public void setIdSesion(String idSesion) {
        this.idSesion = idSesion;
    }
    
    /**
     * Obtiene el correo electrónico del usuario conectado.
     * 
     * @return Correo electrónico del usuario
     */
    public String getCorreoUsuario() {
        return correoUsuario;
    }
    
    /**
     * Establece el correo electrónico del usuario conectado.
     * 
     * @param correoUsuario Nuevo correo electrónico
     */
    public void setCorreoUsuario(String correoUsuario) {
        this.correoUsuario = correoUsuario;
    }
    
    /**
     * Verifica si el cliente tiene una sesión activa.
     * 
     * @return true si hay una sesión activa, false en caso contrario
     */
    public boolean isSesionActiva() {
        // Determinamos si hay una sesión activa basándonos en si hay un idSesion
        return idSesion != null;
    }
    
    /**
     * Sobrescribe el método toString para mostrar información del cliente.
     * 
     * @return String con la información formateada del cliente
     */
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return informacionCliente + " - IP: " + direccionIP + " - Conectado desde: " + sdf.format(horaConexion);
    }
}
