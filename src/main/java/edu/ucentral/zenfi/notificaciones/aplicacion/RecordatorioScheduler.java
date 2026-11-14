
package edu.ucentral.zenfi.notificaciones.aplicacion;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RecordatorioScheduler {

    @Inject
    ProcesadorRecordatorios procesador;

    @Scheduled(
            cron = "0 0 8 * * ?",
            timeZone = "America/Bogota"
    )

    public void ejecutarRecordatorios() {
        procesador.procesarRecordatoriosVencidos();
    }
}

