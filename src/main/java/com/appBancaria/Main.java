package com.appBancaria;

import com.appBancaria.servicio.Servidor;

public class Main {
    public static void main(String[] args) {
        Servidor servidor = new Servidor();
        servidor.iniciar();
    }
}