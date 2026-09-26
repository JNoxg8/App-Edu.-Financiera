package edu.ucentral.zenfi.deudas.infraestructura.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AbonoRespuestaDto(
        Long id,
        BigDecimal monto,
        LocalDate fecha
) {
}
