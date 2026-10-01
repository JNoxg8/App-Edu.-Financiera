package edu.ucentral.zenfi.movimientos.infraestructura.dto;

import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * HU009: formulario para registrar un ingreso o gasto.
 *
 * Si algun campo obligatorio falta, Bean Validation responde 400 con el
 * detalle por campo y el movimiento no se registra (criterio de HU009).
 *
 * deudaId solo se envia cuando la categoria elegida es "Deudas": el
 * gasto se registra entonces como un abono a esa deuda.
 */
public record CrearMovimientoDto(

        @NotNull(message = "Debe seleccionar el tipo de movimiento: INGRESO o GASTO")
        TipoMovimiento tipo,

        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El monto tiene un formato invalido")
        BigDecimal monto,

        @NotNull(message = "La fecha es requerida")
        @PastOrPresent(message = "La fecha del movimiento no puede ser futura")
        LocalDate fecha,

        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
        String descripcion,

        @NotNull(message = "Debe seleccionar una categoria")
        Long categoriaId,

        Long deudaId
) {
}
