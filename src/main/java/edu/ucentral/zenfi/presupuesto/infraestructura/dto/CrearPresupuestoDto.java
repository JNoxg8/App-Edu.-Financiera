package edu.ucentral.zenfi.presupuesto.infraestructura.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * RQ-11: datos del formulario de presupuesto mensual.
 *
 * El mes llega como texto "AAAA-MM" (por ejemplo "2026-10") y se valida
 * con @Pattern: asi un mes mal escrito responde 400 con el detalle del
 * campo, igual que los demas errores de validacion.
 */
public record CrearPresupuestoDto(

        @NotBlank(message = "El mes es requerido")
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "El mes debe tener el formato AAAA-MM")
        String mes,

        @NotNull(message = "El monto limite es requerido")
        @DecimalMin(value = "0.01", message = "El monto limite debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto limite tiene un formato invalido")
        BigDecimal montoLimite
) {
}
