
package edu.ucentral.zenfi.notificaciones.aplicacion;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
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
    public Notificacion crear(String titulo, String mensaje, Long metaAhorroId) {

        Notificacion notificacion = new Notificacion(
                titulo,
                mensaje,
                metaAhorroId
        );

        return repositorio.guardar(notificacion);
    }

    public List<Notificacion> listar() {
        return repositorio.listar();
    }

    public List<Notificacion> listarNoLeidas() {
        return repositorio.listarNoLeidas();
    }

    @Transactional
    public Notificacion marcarComoLeida(Long id) {

        Notificacion notificacion = repositorio.buscarPorId(id)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La notificación no existe"
                ));

        notificacion.marcarComoLeida();

        repositorio.actualizar(notificacion);

        return notificacion;
    }
}

