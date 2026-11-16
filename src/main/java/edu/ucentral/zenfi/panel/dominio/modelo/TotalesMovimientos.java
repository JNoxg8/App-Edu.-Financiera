package edu.ucentral.zenfi.panel.dominio.modelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ingresos y gastos de un periodo, ya sumados por el modulo de movimientos.
 *
 * @param cantidad           numero de movimientos del periodo (0 = no hay informacion)
 * @param gastosPorCategoria de la categoria con mas gasto a la de menos
 */
public record TotalesMovimientos(BigDecimal ingresos, BigDecimal gastos, int cantidad,
                                 List<GastoCategoria> gastosPorCategoria) {

    public static final TotalesMovimientos VACIO =
            new TotalesMovimientos(BigDecimal.ZERO, BigDecimal.ZERO, 0, List.of());

    /** Cuando no se necesita el desglose (por ejemplo, el balance de siempre). */
    public TotalesMovimientos(BigDecimal ingresos, BigDecimal gastos, int cantidad) {
        this(ingresos, gastos, cantidad, List.of());
    }
}
