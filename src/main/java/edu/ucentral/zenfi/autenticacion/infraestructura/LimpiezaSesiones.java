package edu.ucentral.zenfi.autenticacion.infraestructura;

import edu.ucentral.zenfi.autenticacion.aplicacion.AutenticacionServicio;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Borra las sesiones vencidas una vez al dia y al arrancar. En el plan
 * gratuito de Render el servidor se apaga sin trafico y el cron casi nunca
 * lo encuentra despierto, por eso tambien se limpia al iniciar.
 */
@ApplicationScoped
public class LimpiezaSesiones {

    @Inject
    AutenticacionServicio autenticacion;

    @Scheduled(cron = "0 0 3 * * ?", timeZone = "America/Bogota")
    void limpiarCadaDia() {
        limpiar();
    }

    void alIniciar(@Observes StartupEvent evento) {
        limpiar();
    }

    private void limpiar() {
        long borradas = autenticacion.limpiarSesionesVencidas();
        if (borradas > 0) {
            Log.infof("Sesiones vencidas eliminadas: %d", borradas);
        }
    }
}
