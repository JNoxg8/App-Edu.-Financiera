package edu.ucentral.zenfi.perfil.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.perfil.dominio.modelo.Moneda;
import edu.ucentral.zenfi.perfil.dominio.modelo.NivelConocimiento;
import edu.ucentral.zenfi.perfil.dominio.modelo.Perfil;
import edu.ucentral.zenfi.perfil.dominio.repositorio.PerfilRepositorio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Casos de uso del perfil (HU001). */
@ApplicationScoped
public class PerfilServicio {

    @Inject
    PerfilRepositorio repositorio;

    /**
     * Devuelve el perfil o lanza 404 si aun no se ha creado.
     * La app usa ese 404 para decidir si muestra el formulario de creacion.
     */
    public Perfil obtener(Long id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El perfil aun no ha sido creado"));
    }

    /**
     * Crea el perfil una sola vez. Si ya existe responde 409: asi se
     * garantiza en el servidor el criterio "el formulario solo se
     * muestra la primera vez".
     */
    public Perfil crear(Long id, String nombre, Moneda moneda,
                        NivelConocimiento nivel) {
        if (repositorio.existe(id)) {
            throw new ReglaNegocioException("El perfil ya fue creado");
        }
        Perfil perfil = Perfil.crear(id, nombre, moneda, nivel);
        return repositorio.guardar(perfil);
    }
}