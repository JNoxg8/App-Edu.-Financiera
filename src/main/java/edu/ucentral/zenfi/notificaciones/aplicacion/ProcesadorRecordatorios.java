
package edu.ucentral.zenfi.notificaciones.aplicacion;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import edu.ucentral.zenfi.notificaciones.dominio.modelo.RecordatorioMeta;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.RecordatorioMetaRepositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class ProcesadorRecordatorios {

    @Inject
    RecordatorioMetaRepositorio recordatorioRepositorio;

    @Inject
    MetaAhorroRepositorio metaRepositorio;

    @Inject
    NotificacionServicio notificacionServicio;

    @Inject
    RecordatorioMetaServicio recordatorioServicio;

    @Transactional
    public void procesarRecordatoriosVencidos() {

        List<RecordatorioMeta> vencidos =
                recordatorioRepositorio.buscarActivosVencidos(LocalDate.now());

        for (RecordatorioMeta recordatorio : vencidos) {

            MetaAhorro meta = metaRepositorio
                    .buscarPorId(recordatorio.getMetaAhorroId())
                    .orElse(null);

            if (meta == null || !"ACTIVA".equals(meta.getEstado())) {
                recordatorio.desactivar();
                recordatorioRepositorio.actualizar(recordatorio);
                continue;
            }

            notificacionServicio.crear(
                    "Recordatorio de ahorro",
                    "Es momento de aportar a tu meta: " + meta.getNombre(),
                    meta.getId()
            );

            recordatorioServicio.avanzarProximoRecordatorio(recordatorio);
        }
    }
}
