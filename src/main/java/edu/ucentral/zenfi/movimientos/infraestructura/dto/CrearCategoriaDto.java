package edu.ucentral.zenfi.movimientos.infraestructura.dto;

import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HU009: crear una categoria personalizada. */
public record CrearCategoriaDto(

        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 40, message = "El nombre de la categoria no puede superar 40 caracteres")
        String nombre,

        @NotNull(message = "Debe indicar si la categoria es de INGRESO o de GASTO")
        TipoMovimiento tipo
) {
}
