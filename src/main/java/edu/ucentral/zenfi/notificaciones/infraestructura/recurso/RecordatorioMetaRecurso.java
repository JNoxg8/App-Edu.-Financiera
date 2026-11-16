package edu.ucentral.zenfi.notificaciones.infraestructura.recurso;

import edu.ucentral.zenfi.notificaciones.aplicacion.RecordatorioMetaServicio;
import edu.ucentral.zenfi.notificaciones.dominio.modelo.RecordatorioMeta;
import edu.ucentral.zenfi.notificaciones.infraestructura.dto.ConfigurarRecordatorioRequest;
import edu.ucentral.zenfi.notificaciones.infraestructura.dto.RecordatorioMetaResponse;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/metas-ahorro/{metaAhorroId}/recordatorio")
@Tag(name = "Recordatorios de metas", description = "Frecuencia y activacion del recordatorio de cada meta")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecordatorioMetaRecurso {

    @Inject
    RecordatorioMetaServicio servicio;

    @GET
    @Operation(summary = "Consultar el recordatorio de una meta")
    public RecordatorioMetaResponse consultar(
            @PathParam("metaAhorroId") Long metaAhorroId) {

        return convertir(servicio.consultar(metaAhorroId));
    }

    @PUT
    @Operation(summary = "Cambiar la frecuencia o activar/desactivar el recordatorio")
    public RecordatorioMetaResponse configurar(
            @PathParam("metaAhorroId") Long metaAhorroId,
            @Valid ConfigurarRecordatorioRequest request) {

        RecordatorioMeta recordatorio = servicio.configurar(
                metaAhorroId,
                request.frecuencia(),
                request.activo()
        );

        return convertir(recordatorio);
    }

    private RecordatorioMetaResponse convertir(RecordatorioMeta recordatorio) {
        return new RecordatorioMetaResponse(
                recordatorio.getId(),
                recordatorio.getMetaAhorroId(),
                recordatorio.getFrecuencia(),
                recordatorio.getProximoRecordatorio(),
                recordatorio.isActivo(),
                recordatorio.getFechaCreacion()
        );
    }
}
