package edu.ucentral.zenfi.perfil.infraestructura.dto;

import edu.ucentral.zenfi.perfil.dominio.modelo.Moneda;
import edu.ucentral.zenfi.perfil.dominio.modelo.NivelConocimiento;
import edu.ucentral.zenfi.perfil.dominio.modelo.Perfil;

import java.time.LocalDateTime;

/**
 * Perfil que se devuelve a Flutter. Incluye el simbolo de la moneda
 * para que la app no tenga que conocer esa tabla.
 */
public record PerfilRespuestaDto(
        Long id,
        String nombre,
        Moneda moneda,
        String simboloMoneda,
        NivelConocimiento nivel,
        LocalDateTime fechaCreacion
) {
    public static PerfilRespuestaDto desde(Perfil p) {
        return new PerfilRespuestaDto(
                p.getId(),
                p.getNombre(),
                p.getMoneda(),
                p.getMoneda().getSimbolo(),
                p.getNivel(),
                p.getFechaCreacion());
    }
}