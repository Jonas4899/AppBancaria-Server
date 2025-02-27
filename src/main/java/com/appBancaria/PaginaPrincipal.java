package com.appBancaria;

import com.appBancaria.servicio.Servidor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class PaginaPrincipal extends JFrame {
    private JButton iniciarButton;
    private JButton detenerButton;
    private JTextPane consolaPane;
    private JPanel contentPane;
    private JLabel estadoServidor;
    private JLabel tituloServidor;
    private Servidor servidor;

    public PaginaPrincipal() {
        setContentPane(contentPane);
        setTitle("Servidor Bancario");
        setSize(350, 380);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Redirect System.out and System.err to the consolaPane
        redirectSystemStreams();
        
        estadoServidor.setText("Servidor detenido");
        
        iniciarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                consolaPane.setText(""); // Clear previous logs
                log("Iniciando servidor...");
                estadoServidor.setText("Servidor iniciándose...");
                
                // Run server in separate thread to avoid blocking UI
                new Thread(() -> {
                    servidor = new Servidor();
                    servidor.iniciar();
                    SwingUtilities.invokeLater(() -> {
                        estadoServidor.setText("Servidor ejecutándose");
                        iniciarButton.setEnabled(false);
                        detenerButton.setEnabled(true);
                    });
                }).start();
            }
        });
        
        detenerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log("Deteniendo servidor...");
                estadoServidor.setText("Servidor deteniéndose...");
                
                // Run in separate thread to avoid blocking UI
                new Thread(() -> {
                    if (servidor != null) {
                        servidor.detener();
                    }
                    SwingUtilities.invokeLater(() -> {
                        estadoServidor.setText("Servidor detenido");
                        iniciarButton.setEnabled(true);
                        detenerButton.setEnabled(false);
                    });
                }).start();
            }
        });
        
        // Initial button states
        iniciarButton.setEnabled(true);
        detenerButton.setEnabled(false);
        
        setVisible(true);
    }
    
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateTextPane(String.valueOf((char) b));
            }
            
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextPane(new String(b, off, len));
            }
        };
        
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
    
    private void updateTextPane(final String text) {
        SwingUtilities.invokeLater(() -> {
            consolaPane.setText(consolaPane.getText() + text);
            // Auto-scroll to the bottom
            consolaPane.setCaretPosition(consolaPane.getDocument().getLength());
        });
    }
    
    private void log(String message) {
        System.out.println(message);
    }
}
