package com.appBancaria.servicio;

import com.appBancaria.modelo.Cliente;
import com.appBancaria.modelo.Cuenta;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GestorCuentas {
    private final String DATA_FILE = "banco_datos.json";
    private final Gson gson = new Gson();
    private volatile Map<String, Cuenta> cuentas;
    private volatile Map<String, Cliente> clientes;
    private volatile Map<String, List<String>> clienteCuentas;
    private static class DatosBanco {
        Map<String, Cuenta> cuentas;
        Map<String, Cliente> clientes;
        Map<String, List<String>> clienteCuentas;
    }

    public GestorCuentas() {
        cargarDatos();
    }
    
    private synchronized void cargarDatos() {
        try (Reader reader = new FileReader(DATA_FILE)) {
            DatosBanco datos = gson.fromJson(reader, DatosBanco.class);
            if (datos != null) {
                this.cuentas = datos.cuentas;
                this.clientes = datos.clientes;
                this.clienteCuentas = datos.clienteCuentas;
            }
        } catch (IOException e) {
            this.cuentas = new HashMap<>();
            this.clientes = new HashMap<>();
            this.clienteCuentas = new HashMap<>();
        }
    }

    public synchronized double consultarSaldo(String numeroCuenta) {
        cargarDatos(); // Reload data before operation
        Cuenta cuenta = cuentas.get(numeroCuenta);
        if (cuenta != null) {
            return cuenta.getSaldo();
        }
        throw new RuntimeException("Cuenta no encontrada");
    }

    public synchronized String crearCuenta(Cliente cliente) {
        cargarDatos();
        String idCliente = String.valueOf(cliente.getIdentificacion());
        
        // Register client if not exists
        clientes.putIfAbsent(idCliente, cliente);
        
        // Generate unique account number
        String numeroCuenta;
        do {
            numeroCuenta = String.format("%010d", new Random().nextInt(1000000000));
        } while (existeCuenta(numeroCuenta));
        
        // Create new account
        Cuenta nuevaCuenta = new Cuenta();
        nuevaCuenta.setSaldo(0.0);
        nuevaCuenta.setTitular(idCliente);
        
        cuentas.put(numeroCuenta, nuevaCuenta);
        
        // Add account to client's accounts list
        clienteCuentas.computeIfAbsent(idCliente, k -> new ArrayList<>()).add(numeroCuenta);
        
        guardarDatos();
        return numeroCuenta;
    }

    public synchronized double consultarSaldo(int identificacion) {
        cargarDatos();
        String idCliente = String.valueOf(identificacion);
        List<String> cuentasCliente = clienteCuentas.get(idCliente);
        if (cuentasCliente != null && !cuentasCliente.isEmpty()) {
            return consultarSaldo(cuentasCliente.get(0));
        }
        throw new RuntimeException("Cliente no encontrado o sin cuentas");
    }

    public synchronized List<String> obtenerCuentasCliente(int identificacion) {
        cargarDatos();
        String idCliente = String.valueOf(identificacion);
        List<String> cuentasCliente = clienteCuentas.get(idCliente);
        if (cuentasCliente != null) {
            return new ArrayList<>(cuentasCliente);
        }
        throw new RuntimeException("Cliente no encontrado");
    }

    public boolean existeCliente(int identificacion) {
        return clientes.containsKey(String.valueOf(identificacion));
    }
    
    private synchronized void guardarDatos() {
        try (Writer writer = new FileWriter(DATA_FILE)) {
            DatosBanco datos = new DatosBanco();
            datos.cuentas = this.cuentas;
            datos.clientes = this.clientes;
            datos.clienteCuentas = this.clienteCuentas;
            gson.toJson(datos, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean existeCuenta(String numeroCuenta) {
        return cuentas.containsKey(numeroCuenta);
    }
}
