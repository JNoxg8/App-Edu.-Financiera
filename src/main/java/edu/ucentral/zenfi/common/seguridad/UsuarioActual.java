package edu.ucentral.zenfi.common.seguridad;

import edu.ucentral.zenfi.common.excepcion.NoAutenticadoException;
import jakarta.enterprise.context.RequestScoped;

/**
 * El usuario que hizo la peticion actual. Lo llena FiltroAutenticacion a
 * partir del token; los recursos lo inyectan en lugar de un usuario fijo.
 *
 * @RequestScoped: hay una instancia por peticion HTTP, asi dos usuarios
 * atendidos al mismo tiempo nunca comparten el valor.
 */
@RequestScoped
public class UsuarioActual {

    private Long id;

    public void establecer(Long id) {
        this.id = id;
    }

    /** Id del usuario autenticado. Lanza 401 si la peticion no lo tiene. */
    public Long id() {
        if (id == null) {
            throw new NoAutenticadoException("Debe iniciar sesion");
        }
        return id;
    }
}
