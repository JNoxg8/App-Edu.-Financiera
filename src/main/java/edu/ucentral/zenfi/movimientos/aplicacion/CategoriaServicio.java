package edu.ucentral.zenfi.movimientos.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.CategoriaPredefinida;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import edu.ucentral.zenfi.movimientos.dominio.repositorio.CategoriaRepositorio;
import edu.ucentral.zenfi.movimientos.dominio.repositorio.MovimientoRepositorio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/** Casos de uso de las categorias (HU009). */
@ApplicationScoped
public class CategoriaServicio {

    @Inject
    CategoriaRepositorio repositorio;

    @Inject
    MovimientoRepositorio movimientos;

    /** Predefinidas + personalizadas del usuario. tipo = null trae ambas. */
    public List<Categoria> listar(Long usuarioId, TipoMovimiento tipo) {
        return repositorio.listarDisponibles(usuarioId, tipo);
    }

    /**
     * Busca una categoria que el usuario pueda usar. Las de otro usuario
     * responden 404, igual que si no existieran: asi no se revela nada.
     */
    public Categoria obtener(Long usuarioId, Long id) {
        return repositorio.buscarPorId(id)
                .filter(c -> c.disponiblePara(usuarioId))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoria con id " + id));
    }

    /** La categoria puente con el modulo de deudas. */
    public Categoria obtenerDeudas() {
        return repositorio.buscarPredefinida(CategoriaPredefinida.DEUDAS)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontro la categoria predefinida Deudas"));
    }

    /** HU009: crear una categoria personalizada, con nombre no vacio ni repetido. */
    @Transactional
    public Categoria crear(Long usuarioId, String nombre, TipoMovimiento tipo) {
        Categoria nueva = Categoria.personalizada(nombre, tipo, usuarioId);
        verificarNombreLibre(usuarioId, nueva.getNombre(), tipo, null);
        return repositorio.crear(nueva);
    }

    @Transactional
    public Categoria renombrar(Long usuarioId, Long id, String nombre) {
        Categoria categoria = obtener(usuarioId, id);
        categoria.renombrar(nombre);   // 409 si es predefinida
        verificarNombreLibre(usuarioId, categoria.getNombre(), categoria.getTipo(), id);
        return repositorio.actualizar(categoria);
    }

    /**
     * Solo se borran categorias personalizadas y sin movimientos: borrar
     * una en uso dejaria esos ingresos o gastos sin categoria.
     */
    @Transactional
    public void eliminar(Long usuarioId, Long id) {
        Categoria categoria = obtener(usuarioId, id);
        categoria.verificarEditable();

        long enUso = movimientos.contarPorCategoria(id);
        if (enUso > 0) {
            throw new ReglaNegocioException(
                    "La categoria '" + categoria.getNombre() + "' tiene " + enUso
                            + " movimiento(s). Cambielos de categoria antes de eliminarla");
        }
        repositorio.eliminar(id);
    }

    /**
     * El nombre no se puede repetir dentro del mismo tipo, ni con las
     * predefinidas ni con las del usuario. Se compara sin mayusculas ni
     * tildes. Se permite "Otros" en gastos y en ingresos a la vez.
     */
    private void verificarNombreLibre(Long usuarioId, String nombre,
                                      TipoMovimiento tipo, Long idExcluido) {
        boolean repetido = repositorio.listarDisponibles(usuarioId, tipo).stream()
                .filter(c -> idExcluido == null || !idExcluido.equals(c.getId()))
                .anyMatch(c -> c.tieneMismoNombre(nombre));
        if (repetido) {
            throw new ReglaNegocioException(
                    "Ya existe una categoria de " + tipo.getNombre().toLowerCase()
                            + "s llamada '" + nombre + "'");
        }
    }
}
