package edu.ucentral.zenfi.metasahorro.infraestructura.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearMetaAhorroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotNull(message = "El monto objetivo es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto objetivo debe ser mayor que 0")
        BigDecimal montoObjetivo,

        @NotNull(message = "La fecha límite es obligatoria")
        LocalDate fechaLimite,

        @NotNull(message = "El monto inicial es obligatorio")
        @DecimalMin(value = "0.00", inclusive = true, message = "El monto inicial no puede ser negativo")
        BigDecimal montoInicial
) {
}
