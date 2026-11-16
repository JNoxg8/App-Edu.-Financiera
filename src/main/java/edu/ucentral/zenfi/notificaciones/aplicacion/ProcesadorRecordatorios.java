
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

    /**
     * Genera las notificaciones de los recordatorios cuya fecha ya llego.
     *
     * Se llama desde el scheduler (8:00 a. m.), al arrancar la aplicacion y
     * antes de listar notificaciones: en el plan gratuito de Render el
     * servidor se apaga sin trafico, asi que el cron casi nunca lo encuentra
     * despierto. Es seguro llamarlo varias veces: cada recordatorio avanza su
     * proxima fecha despues de notificar.
     */
    @Transactional
    public void procesarRecordatoriosVencidos() {

        LocalDate hoy = LocalDate.now();
        List<RecordatorioMeta> vencidos =
                recordatorioRepositorio.buscarActivosVencidos(hoy);

        for (RecordatorioMeta recordatorio : vencidos) {

            MetaAhorro meta = metaRepositorio
                    .buscarPorId(recordatorio.getMetaAhorroId())
                    .orElse(null);

            if (meta != null) {
                meta.recalcularEstado(hoy);
            }

            if (meta == null || !MetaAhorro.ACTIVA.equals(meta.getEstado())) {
                recordatorio.desactivar();
                recordatorioRepositorio.actualizar(recordatorio);
                continue;
            }

            notificacionServicio.crear(
                    "Recordatorio de ahorro",
                    "Es momento de aportar a tu meta: " + meta.getNombre(),
                    meta.getId(),
                    meta.getUsuarioId()
            );

            recordatorioServicio.avanzarProximoRecordatorio(recordatorio);
        }
    }

    /**
     * Las metas creadas antes de que existieran los recordatorios no tienen
     * uno, y la pantalla de detalle fallaria al consultarlo. Se les crea el
     * recordatorio por defecto (semanal). Es idempotente: solo crea los que
     * faltan.
     */
    @Transactional
    public void crearRecordatoriosFaltantes() {
        for (MetaAhorro meta : metaRepositorio.listar()) {
            if (recordatorioRepositorio.buscarPorMetaAhorroId(meta.getId()).isEmpty()) {
                recordatorioRepositorio.guardar(new RecordatorioMeta(meta.getId()));
            }
        }
    }
}
