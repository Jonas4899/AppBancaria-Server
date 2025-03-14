package com.appBancaria;

import com.appBancaria.servicio.Servidor;
import com.appBancaria.servicio.ClienteConectado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PaginaPrincipal extends JFrame {
    private JButton iniciarButton;
    private JButton detenerButton;
    private JTextArea logTextArea;
    private JTextArea clientesTextArea;
    private JLabel estadoLabel;
    private Servidor servidor;
    private Timer actualizadorClientes;
    private int puerto;

    // Colores para los botones
    private final Color COLOR_BOTON_INICIAR = new Color(46, 139, 87); // Verde oscuro
    private final Color COLOR_BOTON_DETENER = new Color(178, 34, 34); // Rojo oscuro
    private final Color COLOR_BOTON_DISABLED = new Color(150, 150, 150); // Gris
    private final Color COLOR_TEXTO_BOTON = new Color(0, 0, 0); // Negro

    // Constructor que permite especificar el puerto
    public PaginaPrincipal(int puerto) {
        this.puerto = puerto;
        inicializarUI();
    }
    
    // Constructor predeterminado
    public PaginaPrincipal() {
        this.puerto = 12345; // Puerto predeterminado
        inicializarUI();
    }
    
    private void inicializarUI() {
        // Configuración básica de la ventana
        setTitle("Servidor Bancario - Puerto: " + puerto);
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Crear componentes
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Panel superior con título y estado
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel tituloLabel = new JLabel("Panel de Control - Servidor Bancario (Puerto: " + puerto + ")", JLabel.CENTER);
        tituloLabel.setFont(new Font("Arial", Font.BOLD, 18));
        headerPanel.add(tituloLabel, BorderLayout.NORTH);

        JPanel estadoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel estadoTextoLabel = new JLabel("Estado del Servidor: ");
        estadoLabel = new JLabel("DETENIDO");
        estadoLabel.setForeground(Color.RED);
        estadoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        estadoPanel.add(estadoTextoLabel);
        estadoPanel.add(estadoLabel);
        headerPanel.add(estadoPanel, BorderLayout.CENTER);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Panel central con área de texto dividido en dos partes
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        // Área para logs del servidor
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "Log del servidor",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12)
        ));
        
        // Área para clientes conectados
        clientesTextArea = new JTextArea();
        clientesTextArea.setEditable(false);
        clientesTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        clientesTextArea.setBackground(new Color(245, 245, 245));  // Color de fondo ligeramente diferente
        JScrollPane clientesScrollPane = new JScrollPane(clientesTextArea);
        clientesScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "Clientes conectados",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12)
        ));
        
        centerPanel.add(logScrollPane);
        centerPanel.add(clientesScrollPane);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Panel inferior con botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        
        // Creación y estilización de botones
        iniciarButton = crearBotonEstilizado("Iniciar Servidor", COLOR_BOTON_INICIAR);
        detenerButton = crearBotonEstilizado("Detener Servidor", COLOR_BOTON_DETENER);
        
        // Desactivar el botón de detener inicialmente
        detenerButton.setEnabled(false);
        actualizarEstiloBoton(detenerButton, false);
        
        buttonPanel.add(iniciarButton);
        buttonPanel.add(detenerButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Configurar eventos de los botones
        iniciarButton.addActionListener(e -> iniciarServidor());
        detenerButton.addActionListener(e -> detenerServidor());

        // Redirigir la salida estándar al área de texto
        redirectSystemStreams();
        
        // Manejar el cierre de la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (servidor != null) {
                    log("Deteniendo el servidor antes de salir...");
                    servidor.detener();
                }
                if (actualizadorClientes != null) {
                    actualizadorClientes.stop();
                }
                System.exit(0);
            }
        });
        
        // Mostrar la ventana
        setVisible(true);
        
        // Iniciar el servidor automáticamente al abrir la ventana
        SwingUtilities.invokeLater(() -> {
            log("Iniciando servidor automáticamente...");
            iniciarServidor();
        });
    }

    private JButton crearBotonEstilizado(String texto, Color colorFondo) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(180, 36));
        boton.setBackground(colorFondo);
        boton.setForeground(COLOR_TEXTO_BOTON);
        boton.setFocusPainted(false);
        boton.setFont(new Font("Arial", Font.BOLD, 14));
        boton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            new EmptyBorder(5, 10, 5, 10)
        ));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Efecto hover con un MouseListener
        boton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (boton.isEnabled()) {
                    boton.setBackground(colorFondo.brighter());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (boton.isEnabled()) {
                    boton.setBackground(colorFondo);
                }
            }
        });
        
        return boton;
    }
    
    private void actualizarEstiloBoton(JButton boton, boolean habilitado) {
        if (habilitado) {
            Color color = boton == iniciarButton ? COLOR_BOTON_INICIAR : COLOR_BOTON_DETENER;
            boton.setBackground(color);
            boton.setForeground(COLOR_TEXTO_BOTON);
        } else {
            boton.setBackground(COLOR_BOTON_DISABLED);
            boton.setForeground(new Color(100, 100, 100));
        }
    }

    private void iniciarServidor() {
        logTextArea.setText(""); // Limpiar logs anteriores
        clientesTextArea.setText(""); // Limpiar lista de clientes
        log("Iniciando servidor en el puerto " + puerto + "...");
        estadoLabel.setText("INICIANDO");
        estadoLabel.setForeground(new Color(255, 150, 0));
        
        // Deshabilitar el botón de iniciar y activar el de detener
        iniciarButton.setEnabled(false);
        actualizarEstiloBoton(iniciarButton, false);
        
        // Ejecutar el servidor en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                servidor = new Servidor(puerto);
                servidor.iniciar();
                
                // Actualizar UI en el hilo de EDT
                SwingUtilities.invokeLater(() -> {
                    estadoLabel.setText("EJECUTANDO");
                    estadoLabel.setForeground(new Color(0, 150, 0));
                    
                    // Actualizar estado de los botones
                    detenerButton.setEnabled(true);
                    actualizarEstiloBoton(detenerButton, true);
                });
                
                log("Servidor iniciado correctamente en el puerto " + puerto + ". Esperando conexiones...");
                
                // Iniciar el temporizador para actualizar la lista de clientes
                iniciarActualizadorClientes();
            } catch (Exception ex) {
                log("Error al iniciar el servidor: " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    estadoLabel.setText("ERROR");
                    estadoLabel.setForeground(Color.RED);
                    
                    // Revertir estado de los botones
                    iniciarButton.setEnabled(true);
                    actualizarEstiloBoton(iniciarButton, true);
                    detenerButton.setEnabled(false);
                    actualizarEstiloBoton(detenerButton, false);
                });
            }
        }).start();
    }

    private void detenerServidor() {
        log("Deteniendo servidor...");
        estadoLabel.setText("DETENIENDO");
        estadoLabel.setForeground(new Color(255, 150, 0));
        
        // Deshabilitar el botón de detener mientras se detiene
        detenerButton.setEnabled(false);
        actualizarEstiloBoton(detenerButton, false);
        
        // Detener actualizador de clientes
        if (actualizadorClientes != null) {
            actualizadorClientes.stop();
            actualizadorClientes = null;
        }
        
        // Detener el servidor en un hilo separado
        new Thread(() -> {
            if (servidor != null) {
                servidor.detener();
            }
            
            log("Servidor detenido correctamente.");
            
            // Cerrar la aplicación completamente después de detener el servidor
            log("Cerrando la aplicación...");
            SwingUtilities.invokeLater(() -> {
                dispose(); // Cierra la ventana
                System.exit(0); // Termina la aplicación completamente
            });
        }).start();
    }
    
    private void iniciarActualizadorClientes() {
        // Temporizador que actualiza la lista de clientes conectados cada segundo
        actualizadorClientes = new Timer(1000, e -> actualizarListaClientes());
        actualizadorClientes.start();
    }
    
    private void actualizarListaClientes() {
        if (servidor != null) {
            // Obtener la lista actual de clientes conectados
            java.util.List<ClienteConectado> clientes = servidor.getClientesConectados();
            
            // Construir texto con la información de los clientes
            StringBuilder sb = new StringBuilder();
            
            // Mostrar el número de clientes conectados
            sb.append("Total de clientes conectados: ").append(clientes.size()).append("\n\n");
            
            // Formateo de fecha para mostrar la hora de conexión
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            
            // Si no hay clientes conectados
            if (clientes.isEmpty()) {
                sb.append("No hay clientes conectados actualmente.\n");
            } else {
                // Listar cada cliente conectado
                for (int i = 0; i < clientes.size(); i++) {
                    ClienteConectado cliente = clientes.get(i);
                    sb.append(i + 1).append(". ");
                    sb.append(cliente.getInformacionCliente()).append("\n");
                    sb.append("   IP: ").append(cliente.getDireccionIP()).append("\n");
                    sb.append("   Conectado desde: ").append(sdf.format(cliente.getHoraConexion())).append("\n\n");
                }
            }
            
            // Actualizar el área de texto en el EDT
            final String texto = sb.toString();
            SwingUtilities.invokeLater(() -> clientesTextArea.setText(texto));
        }
    }
    
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateTextArea(String.valueOf((char) b));
            }
            
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len));
            }
        };
        
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
    
    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(text);
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }
    
    private void log(String message) {
        System.out.println("[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + message);
    }

    public static void main(String[] args) {
        new PaginaPrincipal();
    }
}