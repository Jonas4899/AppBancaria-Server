package com.appBancaria;

import com.appBancaria.servicio.Servidor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PaginaPrincipal extends JFrame {
    private JButton iniciarButton;
    private JButton detenerButton;
    private JTextPane textPane1;
    private JPanel contentPane;
    private Servidor servidor;

    public PaginaPrincipal() {
        setContentPane(contentPane);
        setTitle("Servidor Bancario");
        setSize(350, 380);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
        iniciarButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                servidor = new Servidor();
                servidor.iniciar();
            }
        });
        detenerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                servidor.detener();
            }
        });
    }
}
