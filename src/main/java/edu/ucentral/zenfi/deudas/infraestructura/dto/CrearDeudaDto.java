package edu.ucentral.zenfi.deudas.infraestructura.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RQ-07: datos del formulario de registro de deuda.
 *
 * Es un record: inmutable, sin getters ni setters escritos a mano
 * y sin necesidad de Lombok.
 */
public record CrearDeudaDto(

        @NotBlank(message = "El nombre del acreedor es requerido")
        @Size(max = 120, message = "El acreedor no puede superar 120 caracteres")
        String acreedor,

        @NotNull(message = "El monto total es requerido")
        @DecimalMin(value = "0.01", message = "El monto total debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto total tiene un formato invalido")
        BigDecimal montoTotal,

        @NotNull(message = "La tasa de interes es requerida")
        @DecimalMin(value = "0.0", message = "La tasa de interes no puede ser negativa")
        @DecimalMax(value = "300.0", message = "La tasa de interes supera el maximo permitido")
        @Digits(integer = 3, fraction = 2, message = "La tasa de interes tiene un formato invalido")
        BigDecimal tasaInteresAnual,

        @Min(value = 1, message = "Debe haber al menos una cuota")
        @Max(value = 600, message = "El numero de cuotas es demasiado alto")
        int numeroCuotas,

        @NotNull(message = "La fecha de vencimiento es requerida")
        @FutureOrPresent(message = "La fecha de vencimiento no puede estar en el pasado")
        LocalDate fechaVencimiento
) {
}
