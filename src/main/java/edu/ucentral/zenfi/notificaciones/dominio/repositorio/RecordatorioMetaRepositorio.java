
package edu.ucentral.zenfi.notificaciones.dominio.repositorio;

import edu.ucentral.zenfi.notificaciones.dominio.modelo.RecordatorioMeta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecordatorioMetaRepositorio {

    RecordatorioMeta guardar(RecordatorioMeta recordatorio);

    Optional<RecordatorioMeta> buscarPorMetaAhorroId(Long metaAhorroId);

    List<RecordatorioMeta> buscarActivosVencidos(LocalDate fecha);

    void actualizar(RecordatorioMeta recordatorio);

    void eliminarPorMetaAhorroId(Long metaAhorroId);
}
