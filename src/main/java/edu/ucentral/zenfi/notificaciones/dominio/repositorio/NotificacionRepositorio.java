

package edu.ucentral.zenfi.notificaciones.dominio.repositorio;

import edu.ucentral.zenfi.notificaciones.dominio.modelo.Notificacion;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepositorio {

    Notificacion guardar(Notificacion notificacion);

    List<Notificacion> listar();

    List<Notificacion> listarNoLeidas();

    Optional<Notificacion> buscarPorId(Long id);

    void actualizar(Notificacion notificacion);

    void desvincularMeta(Long metaAhorroId);
}

