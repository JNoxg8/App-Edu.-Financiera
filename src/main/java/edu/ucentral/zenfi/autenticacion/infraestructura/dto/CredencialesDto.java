package edu.ucentral.zenfi.autenticacion.infraestructura.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Correo y contrasena. Aqui solo se exige que lleguen; el formato del
 * correo y el largo de la contrasena los valida el dominio (Cuenta) al
 * registrarse, para que el inicio de sesion no revele esas reglas.
 */
public record CredencialesDto(
        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contrasena es obligatoria")
        String contrasena
) {
}
