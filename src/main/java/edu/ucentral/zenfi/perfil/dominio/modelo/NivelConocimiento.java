package edu.ucentral.zenfi.perfil.dominio.modelo;

/**
 * Nivel de conocimiento financiero del usuario (HU001).
 * Lo usaran los modulos educativos para habilitar el contenido del nivel.
 */
public enum NivelConocimiento {

    BASICO("Basico"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado");

    private final String nombre;

    NivelConocimiento(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}