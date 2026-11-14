

package edu.ucentral.zenfi.notificaciones.infraestructura.persistencia;

import edu.ucentral.zenfi.notificaciones.dominio.modelo.RecordatorioMeta;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.RecordatorioMetaRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RecordatorioMetaRepositorioPanache
        implements RecordatorioMetaRepositorio, PanacheRepository<RecordatorioMeta> {

    @Override
    public RecordatorioMeta guardar(RecordatorioMeta recordatorio) {
        persist(recordatorio);
        return recordatorio;
    }

    @Override
    public Optional<RecordatorioMeta> buscarPorMetaAhorroId(Long metaAhorroId) {
        return find("metaAhorroId", metaAhorroId).firstResultOptional();
    }

    @Override
    public List<RecordatorioMeta> buscarActivosVencidos(LocalDate fecha) {
        return find("activo = true and proximoRecordatorio <= ?1", fecha).list();
    }

    @Override
    public void actualizar(RecordatorioMeta recordatorio) {
        // Hibernate gestiona los cambios de entidades administradas
        // dentro de una transacción.
    }

    @Override
    public void eliminarPorMetaAhorroId(Long metaAhorroId) {
        delete("metaAhorroId", metaAhorroId);
    }
}
