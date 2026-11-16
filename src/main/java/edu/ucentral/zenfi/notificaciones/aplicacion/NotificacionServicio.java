
package edu.ucentral.zenfi.notificaciones.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.notificaciones.dominio.modelo.Notificacion;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.NotificacionRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class NotificacionServicio {

    @Inject
    NotificacionRepositorio repositorio;

    @Transactional
    public Notificacion crear(String titulo, String mensaje, Long metaAhorroId, Long usuarioId) {

        Notificacion notificacion = new Notificacion(
                titulo,
                mensaje,
                metaAhorroId,
                usuarioId
        );

        return repositorio.guardar(notificacion);
    }

    public List<Notificacion> listar(Long usuarioId) {
        return repositorio.listarPorUsuario(usuarioId);
    }

    public List<Notificacion> listarNoLeidas(Long usuarioId) {
        return repositorio.listarNoLeidasPorUsuario(usuarioId);
    }

    @Transactional
    public Notificacion marcarComoLeida(Long usuarioId, Long id) {

        // La notificacion de otro usuario responde 404, como si no existiera
        Notificacion notificacion = repositorio.buscarPorId(id)
                .filter(n -> n.perteneceA(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La notificación no existe"
                ));

        notificacion.marcarComoLeida();

        repositorio.actualizar(notificacion);

        return notificacion;
    }
}

