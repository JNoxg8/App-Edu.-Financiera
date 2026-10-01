package edu.ucentral.zenfi.metasahorro.infraestructura.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MetaAhorroResponse(
        Long id,
        String nombre,
        BigDecimal montoObjetivo,
        LocalDate fechaLimite,
        BigDecimal montoInicial,
        BigDecimal montoAcumulado,
        String estado
) {
}
