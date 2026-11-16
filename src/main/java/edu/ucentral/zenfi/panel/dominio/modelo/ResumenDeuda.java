package edu.ucentral.zenfi.panel.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una deuda pendiente con su saldo actual, como la muestra el panel (HU015). */
public record ResumenDeuda(Long id, String acreedor, BigDecimal saldoPendiente,
                           BigDecimal porcentajePagado, LocalDate fechaVencimiento) {
}
