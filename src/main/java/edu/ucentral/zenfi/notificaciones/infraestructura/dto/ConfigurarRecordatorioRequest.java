package edu.ucentral.zenfi.notificaciones.infraestructura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConfigurarRecordatorioRequest(

        @NotBlank(message = "La frecuencia es obligatoria")
        @Pattern(
                regexp = "DIARIA|SEMANAL|QUINCENAL|MENSUAL",
                message = "La frecuencia debe ser DIARIA, SEMANAL, QUINCENAL o MENSUAL"
        )
        String frecuencia,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo

) {
}
