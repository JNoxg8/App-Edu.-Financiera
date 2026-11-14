
package edu.ucentral.zenfi.metasahorro.infraestructura.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AporteMetaAhorroResponse(
        Long id,
        Long metaAhorroId,
        BigDecimal monto,
        LocalDate fecha
) {
}
