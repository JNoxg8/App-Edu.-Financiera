package edu.ucentral.zenfi.perfil.dominio.modelo;

/**
 * Monedas de preferencia disponibles (HU001).
 *
 * La moneda solo define como se MUESTRAN los montos (simbolo y formato).
 * No convierte valores entre divisas.
 */
public enum Moneda {

    COP("$", "Peso colombiano"),
    USD("US$", "Dolar estadounidense"),
    EUR("€", "Euro");

    private final String simbolo;
    private final String nombre;

    Moneda(String simbolo, String nombre) {
        this.simbolo = simbolo;
        this.nombre = nombre;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public String getNombre() {
        return nombre;
    }
}