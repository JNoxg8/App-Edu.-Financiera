package edu.ucentral.zenfi.autenticacion.dominio.repositorio;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Sesiones abiertas. Se guarda el HASH del token, no el token: si alguien
 * lee la base de datos no puede usar esos valores para hacerse pasar por
 * un usuario.
 */
public interface SesionRepositorio {

    void crear(String hashToken, Long cuentaId, LocalDateTime expira);

    /** Cuenta de la sesion, si existe y no ha vencido. */
    Optional<Long> cuentaDeSesionVigente(String hashToken, LocalDateTime ahora);

    void eliminar(String hashToken);

    /** Borra las sesiones que vencieron antes de {@code ahora}; devuelve cuantas. */
    long eliminarVencidas(LocalDateTime ahora);
}
