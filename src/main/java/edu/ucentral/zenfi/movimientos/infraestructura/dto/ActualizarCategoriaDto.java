package edu.ucentral.zenfi.movimientos.infraestructura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Renombrar una categoria personalizada. El tipo no se cambia. */
public record ActualizarCategoriaDto(

        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 40, message = "El nombre de la categoria no puede superar 40 caracteres")
        String nombre
) {
}
