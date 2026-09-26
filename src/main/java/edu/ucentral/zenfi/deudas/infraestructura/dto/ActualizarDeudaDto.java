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
 * RQ-22: datos editables de una deuda.
 *
 * El montoTotal NO esta aqui a proposito: el requisito solo permite
 * cambiar acreedor, tasa, numero de cuotas y fecha de vencimiento.
 * El capital prestado es un hecho historico que no se modifica.
 */
public record ActualizarDeudaDto(

        @NotBlank(message = "El nombre del acreedor es requerido")
        @Size(max = 120, message = "El acreedor no puede superar 120 caracteres")
        String acreedor,

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