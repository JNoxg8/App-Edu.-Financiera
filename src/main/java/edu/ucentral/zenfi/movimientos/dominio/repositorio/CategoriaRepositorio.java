package edu.ucentral.zenfi.movimientos.dominio.repositorio;

import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.CategoriaPredefinida;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de las categorias. Lo implementa CategoriaPanache. */
public interface CategoriaRepositorio {

    Categoria crear(Categoria categoria);

    Categoria actualizar(Categoria categoria);

    Optional<Categoria> buscarPorId(Long id);

    Optional<Categoria> buscarPredefinida(CategoriaPredefinida predefinida);

    /**
     * Predefinidas + personalizadas del usuario. Primero las predefinidas
     * en su orden de fabrica y despues las del usuario por nombre.
     *
     * @param tipo null para traer las de ambos tipos
     */
    List<Categoria> listarDisponibles(Long usuarioId, TipoMovimiento tipo);

    boolean eliminar(Long id);
}
