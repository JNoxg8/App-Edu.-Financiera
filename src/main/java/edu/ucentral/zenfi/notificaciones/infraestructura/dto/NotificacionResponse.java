
package edu.ucentral.zenfi.notificaciones.infraestructura.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        String titulo,
        String mensaje,
        boolean leida,
        LocalDateTime fechaCreacion,
        Long metaAhorroId
) {
}
