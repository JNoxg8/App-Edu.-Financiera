
package edu.ucentral.zenfi.notificaciones.infraestructura.recurso;

import edu.ucentral.zenfi.notificaciones.aplicacion.NotificacionServicio;
import edu.ucentral.zenfi.notificaciones.aplicacion.ProcesadorRecordatorios;
import edu.ucentral.zenfi.notificaciones.dominio.modelo.Notificacion;
import edu.ucentral.zenfi.notificaciones.infraestructura.dto.NotificacionResponse;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import edu.ucentral.zenfi.common.seguridad.UsuarioActual;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

@Path("/notificaciones")
@Tag(name = "Notificaciones", description = "Avisos generados por los recordatorios de metas")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "token")
public class NotificacionRecurso {

    /** Usuario de la peticion: lo identifica FiltroAutenticacion con el token. */
    @Inject
    UsuarioActual usuarioActual;

    @Inject
    NotificacionServicio servicio;

    @Inject
    ProcesadorRecordatorios procesador;

    /**
     * Antes de listar se generan los recordatorios pendientes: asi la
     * notificacion aparece cuando el usuario abre la app, aunque el
     * servidor estuviera apagado a la hora del scheduler.
     */
    @GET
    @Operation(summary = "Listar notificaciones (las mas recientes primero)")
    public List<NotificacionResponse> listar() {
        procesador.procesarRecordatoriosVencidos();
        return servicio.listar(usuarioActual.id())
                .stream()
                .map(this::convertir)
                .toList();
    }

    @GET
    @Path("/no-leidas")
    @Operation(summary = "Listar las notificaciones no leidas")
    public List<NotificacionResponse> listarNoLeidas() {
        procesador.procesarRecordatoriosVencidos();
        return servicio.listarNoLeidas(usuarioActual.id())
                .stream()
                .map(this::convertir)
                .toList();
    }

    @PATCH
    @Path("/{id}/leida")
    @Operation(summary = "Marcar una notificacion como leida")
    public NotificacionResponse marcarComoLeida(@PathParam("id") Long id) {
        return convertir(servicio.marcarComoLeida(usuarioActual.id(), id));
    }

    private NotificacionResponse convertir(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.isLeida(),
                notificacion.getFechaCreacion(),
                notificacion.getMetaAhorroId()
        );
    }
}

