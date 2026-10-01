package edu.ucentral.zenfi.movimientos.dominio.modelo;

/**
 * Categorias que trae la app de fabrica (HU009).
 *
 * Se insertan en la tabla al arrancar (ver CategoriaSemilla) y el
 * usuario no las puede editar ni borrar. El orden de este enum es el
 * orden en que aparecen en la app: "Otros" queda al final a proposito.
 *
 * DEUDAS es especial: es el puente con el modulo de deudas. Todo abono
 * a una deuda se registra automaticamente como un gasto en esta
 * categoria, porque pagar una deuda es dinero que sale del bolsillo.
 */
public enum CategoriaPredefinida {

    // ---------------- Gastos ----------------
    ALIMENTACION("Alimentación", TipoMovimiento.GASTO),
    TRANSPORTE("Transporte", TipoMovimiento.GASTO),
    ENTRETENIMIENTO("Entretenimiento", TipoMovimiento.GASTO),
    VIVIENDA("Vivienda", TipoMovimiento.GASTO),
    SERVICIOS("Servicios públicos", TipoMovimiento.GASTO),
    SALUD("Salud", TipoMovimiento.GASTO),
    EDUCACION("Educación", TipoMovimiento.GASTO),
    DEUDAS("Deudas", TipoMovimiento.GASTO),
    OTROS_GASTOS("Otros", TipoMovimiento.GASTO),

    // ---------------- Ingresos ----------------
    SALARIO("Salario", TipoMovimiento.INGRESO),
    VENTAS("Ventas", TipoMovimiento.INGRESO),
    INVERSIONES("Inversiones", TipoMovimiento.INGRESO),
    REGALOS("Regalos", TipoMovimiento.INGRESO),
    OTROS_INGRESOS("Otros", TipoMovimiento.INGRESO);

    private final String nombre;
    private final TipoMovimiento tipo;

    CategoriaPredefinida(String nombre, TipoMovimiento tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }
}
