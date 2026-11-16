package edu.ucentral.zenfi.common.infraestructura;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Antes de existir las cuentas, todo pertenecia al usuario fijo 1. Las
 * deudas, movimientos y presupuestos ya guardaban usuario_id = 1, pero las
 * metas y las notificaciones no tenian dueno: al arrancar se les asigna el
 * usuario 1, que es la primera cuenta que se registre.
 *
 * Es idempotente: solo toca las filas sin dueno, asi que despues de la
 * primera vez no cambia nada.
 */
@ApplicationScoped
public class MigracionDatosSinUsuario {

    static final long USUARIO_ORIGINAL = 1L;

    @Inject
    EntityManager em;

    @Transactional
    void alIniciar(@Observes StartupEvent evento) {
        int metas = em.createQuery(
                        "update MetaAhorro set usuarioId = :usuario where usuarioId is null")
                .setParameter("usuario", USUARIO_ORIGINAL)
                .executeUpdate();
        int notificaciones = em.createQuery(
                        "update Notificacion set usuarioId = :usuario where usuarioId is null")
                .setParameter("usuario", USUARIO_ORIGINAL)
                .executeUpdate();
        if (metas + notificaciones > 0) {
            Log.infof("Datos sin dueno asignados al usuario %d: %d metas, %d notificaciones",
                    USUARIO_ORIGINAL, metas, notificaciones);
        }
    }
}
