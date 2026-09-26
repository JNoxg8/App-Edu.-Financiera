package edu.ucentral.zenfi.deudas.infraestructura.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos para registrar un abono. La fecha es opcional: si falta se usa hoy. */
public record CrearAbonoDto(

        @NotNull(message = "El monto del abono es requerido")
        @DecimalMin(value = "0.01", message = "El abono debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto del abono tiene un formato invalido")
        BigDecimal monto,

        @PastOrPresent(message = "La fecha del abono no puede ser futura")
        LocalDate fecha
) {
}
