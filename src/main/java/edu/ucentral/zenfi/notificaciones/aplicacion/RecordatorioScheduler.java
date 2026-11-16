
package edu.ucentral.zenfi.notificaciones.aplicacion;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
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

    /**
     * En Render el servidor se apaga sin trafico y se enciende con la
     * primera peticion: al arrancar se ponen al dia los recordatorios que
     * vencieron mientras estaba apagado.
     */
    void alIniciar(@Observes StartupEvent evento) {
        procesador.crearRecordatoriosFaltantes();
        procesador.procesarRecordatoriosVencidos();
    }
}

