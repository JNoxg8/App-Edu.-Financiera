
package edu.ucentral.zenfi.notificaciones.infraestructura.recurso;

import edu.ucentral.zenfi.notificaciones.aplicacion.NotificacionServicio;
import edu.ucentral.zenfi.notificaciones.dominio.modelo.Notificacion;
import edu.ucentral.zenfi.notificaciones.infraestructura.dto.NotificacionResponse;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
public class NotificacionRecurso {

    @Inject
    NotificacionServicio servicio;

    @GET
    public List<NotificacionResponse> listar() {
        return servicio.listar()
                .stream()
                .map(this::convertir)
                .toList();
    }

    @GET
    @Path("/no-leidas")
    public List<NotificacionResponse> listarNoLeidas() {
        return servicio.listarNoLeidas()
                .stream()
                .map(this::convertir)
                .toList();
    }

    @PATCH
    @Path("/{id}/leida")
    public NotificacionResponse marcarComoLeida(@PathParam("id") Long id) {
        return convertir(servicio.marcarComoLeida(id));
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

