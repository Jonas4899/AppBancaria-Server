package com.appBancaria;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Determinar el puerto del servidor
        int puerto = 12345; // Puerto predeterminado
        
        // Verificar si se proporcionó un puerto como argumento
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
                if (puerto <= 0 || puerto > 65535) {
                    System.err.println("El puerto debe estar entre 1 y 65535. Usando puerto predeterminado: 12345");
                    puerto = 12345;
                } else {
                    System.out.println("Iniciando servidor en el puerto: " + puerto);
                }
            } catch (NumberFormatException e) {
                System.err.println("Puerto inválido: " + args[0] + ". Debe ser un número entero. Usando puerto predeterminado: 12345");
            }
        } else {
            System.out.println("No se especificó un puerto. Usando puerto predeterminado: 12345");
        }
        
        // Variable final para usar dentro de lambda
        final int puertofinal = puerto;
        
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("No se pudo establecer el look and feel: " + e.getMessage());
            }
            new PaginaPrincipal(puertofinal);
        });
    }
}