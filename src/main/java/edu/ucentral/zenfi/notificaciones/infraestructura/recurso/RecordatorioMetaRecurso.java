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

@Path("/metas-ahorro/{metaAhorroId}/recordatorio")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecordatorioMetaRecurso {

    @Inject
    RecordatorioMetaServicio servicio;

    @GET
    public RecordatorioMetaResponse consultar(
            @PathParam("metaAhorroId") Long metaAhorroId) {

        return convertir(servicio.consultar(metaAhorroId));
    }

    @PUT
    public RecordatorioMetaResponse configurar(
            @PathParam("metaAhorroId") Long metaAhorroId,
            @Valid ConfigurarRecordatorioRequest request) {

        RecordatorioMeta recordatorio =
                servicio.configurar(metaAhorroId, request.frecuencia());

        recordatorio = servicio.cambiarEstado(
                metaAhorroId,
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
