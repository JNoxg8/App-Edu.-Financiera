
package edu.ucentral.zenfi.metasahorro.infraestructura.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearAporteMetaAhorroRequest(

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El aporte debe ser mayor que cero")
        BigDecimal monto,

        LocalDate fecha

) {
}
