package edu.ucentral.zenfi.panel.dominio.modelo;

import java.math.BigDecimal;

/**
 * Ingresos y gastos de un periodo, ya sumados por el modulo de movimientos.
 *
 * @param cantidad numero de movimientos del periodo (0 = no hay informacion)
 */
public record TotalesMovimientos(BigDecimal ingresos, BigDecimal gastos, int cantidad) {

    public static final TotalesMovimientos VACIO =
            new TotalesMovimientos(BigDecimal.ZERO, BigDecimal.ZERO, 0);
}
