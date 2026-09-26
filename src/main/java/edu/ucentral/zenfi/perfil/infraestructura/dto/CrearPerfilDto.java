package edu.ucentral.zenfi.perfil.infraestructura.dto;

import edu.ucentral.zenfi.perfil.dominio.modelo.Moneda;
import edu.ucentral.zenfi.perfil.dominio.modelo.NivelConocimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos para crear el perfil (HU001). */
public record CrearPerfilDto(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotNull(message = "Debe seleccionar una moneda de preferencia")
        Moneda moneda,

        @NotNull(message = "Debe seleccionar un nivel de conocimiento")
        NivelConocimiento nivel
) {
}