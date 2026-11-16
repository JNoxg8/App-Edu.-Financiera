package edu.ucentral.zenfi.panel.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Avance de una meta de ahorro activa, como lo muestra el panel (HU015). */
public record ResumenMeta(Long id, String nombre, BigDecimal montoObjetivo,
                          BigDecimal montoAhorrado, BigDecimal porcentaje,
                          LocalDate fechaLimite) {

    private static final BigDecimal CIEN = new BigDecimal("100");

    /** El porcentaje se calcula igual que en HU004: ahorrado / objetivo. */
    public static ResumenMeta de(Long id, String nombre, BigDecimal montoObjetivo,
                                 BigDecimal montoAhorrado, LocalDate fechaLimite) {
        BigDecimal porcentaje = montoObjetivo.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : montoAhorrado.multiply(CIEN).divide(montoObjetivo, 2, RoundingMode.HALF_UP);
        return new ResumenMeta(id, nombre, montoObjetivo, montoAhorrado, porcentaje, fechaLimite);
    }
}
