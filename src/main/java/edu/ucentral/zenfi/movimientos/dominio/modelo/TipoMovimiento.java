package edu.ucentral.zenfi.movimientos.dominio.modelo;

/** Tipo de movimiento de dinero (HU009): entra (ingreso) o sale (gasto). */
public enum TipoMovimiento {

    INGRESO("Ingreso"),
    GASTO("Gasto");

    private final String nombre;

    TipoMovimiento(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
