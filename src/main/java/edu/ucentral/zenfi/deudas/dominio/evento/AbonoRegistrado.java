package edu.ucentral.zenfi.deudas.dominio.evento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento de dominio: se registro un abono a una deuda.
 *
 * El modulo de deudas lo publica sin saber quien lo escucha (patron
 * Observer con los eventos CDI). Movimientos lo escucha para anotar el
 * abono como un gasto en la categoria "Deudas" (HU009).
 */
public record AbonoRegistrado(
        Long deudaId,
        Long abonoId,
        Long usuarioId,
        String acreedor,
        BigDecimal monto,
        LocalDate fecha
) {
}
