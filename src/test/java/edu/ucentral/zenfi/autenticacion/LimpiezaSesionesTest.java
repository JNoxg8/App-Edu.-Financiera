package edu.ucentral.zenfi.autenticacion;

import edu.ucentral.zenfi.autenticacion.aplicacion.AutenticacionServicio;
import edu.ucentral.zenfi.autenticacion.infraestructura.persistencia.SesionPanache;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La limpieza borra solo las sesiones vencidas. */
@QuarkusTest
class LimpiezaSesionesTest {

    @Inject
    AutenticacionServicio autenticacion;

    @Inject
    SesionPanache sesiones;

    /** Hash unico de 64 caracteres: la base H2 la comparten todas las pruebas. */
    private static String hashUnico() {
        return (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "");
    }

    @Test
    @DisplayName("Borra las sesiones vencidas y conserva las vigentes")
    void borraSoloLasVencidas() {
        String vencida = hashUnico();
        String vigente = hashUnico();
        LocalDateTime ahora = LocalDateTime.now();
        sesiones.crear(vencida, 999_001L, ahora.minusDays(1));
        sesiones.crear(vigente, 999_001L, ahora.plusDays(1));

        long borradas = autenticacion.limpiarSesionesVencidas();

        assertTrue(borradas >= 1, "debio borrar al menos la sesion vencida");
        QuarkusTransaction.requiringNew().run(() -> {
            assertEquals(0, sesiones.count("hashToken", vencida));
            assertEquals(1, sesiones.count("hashToken", vigente));
        });
    }
}
