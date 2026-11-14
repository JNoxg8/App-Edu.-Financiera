
package edu.ucentral.zenfi.notificaciones.infraestructura.persistencia;

import edu.ucentral.zenfi.notificaciones.dominio.modelo.Notificacion;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.NotificacionRepositorio;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class NotificacionRepositorioPanache
        implements NotificacionRepositorio, PanacheRepository<Notificacion> {

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        persist(notificacion);
        return notificacion;
    }

    @Override
    public List<Notificacion> listar() {
        return find("order by fechaCreacion desc").list();
    }

    @Override
    public List<Notificacion> listarNoLeidas() {
        return find("leida = false order by fechaCreacion desc").list();
    }

    @Override
    public Optional<Notificacion> buscarPorId(Long id) {
        return findByIdOptional(id);
    }

    @Override
    public void actualizar(Notificacion notificacion) {
        // Hibernate gestiona los cambios de entidades administradas
        // dentro de una transacción.
    }

    @Override
    public void desvincularMeta(Long metaAhorroId) {
        List<Notificacion> notificaciones =
                find("metaAhorroId", metaAhorroId).list();

        for (Notificacion notificacion : notificaciones) {
            notificacion.desvincularMeta();
        }
    }
}
