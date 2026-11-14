
package edu.ucentral.zenfi.notificaciones.infraestructura.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecordatorioMetaResponse(
        Long id,
        Long metaAhorroId,
        String frecuencia,
        LocalDate proximoRecordatorio,
        boolean activo,
        LocalDateTime fechaCreacion
) {
}

