package edu.ucentral.zenfi.movimientos.infraestructura.persistencia;

import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.CategoriaPredefinida;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import edu.ucentral.zenfi.movimientos.dominio.repositorio.CategoriaRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Adaptador: implementa el puerto CategoriaRepositorio con Panache. */
@ApplicationScoped
public class CategoriaPanache implements CategoriaRepositorio, PanacheRepository<CategoriaEntity> {

    @Override
    @Transactional
    public Categoria crear(Categoria categoria) {
        CategoriaEntity entidad = new CategoriaEntity();
        entidad.nombre = categoria.getNombre();
        entidad.tipo = categoria.getTipo();
        entidad.predefinida = categoria.isPredefinida();
        entidad.usuarioId = categoria.getUsuarioId();
        persist(entidad);
        return aDominio(entidad);
    }

    @Override
    @Transactional
    public Categoria actualizar(Categoria categoria) {
        CategoriaEntity entidad = findById(categoria.getId());
        if (entidad == null) {
            return null;
        }
        // Solo el nombre es editable
        entidad.nombre = categoria.getNombre();
        return aDominio(entidad);
    }

    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    public Optional<Categoria> buscarPredefinida(CategoriaPredefinida predefinida) {
        return find("predefinida = true and tipo = ?1 and nombre = ?2",
                predefinida.getTipo(), predefinida.getNombre())
                .firstResultOptional()
                .map(this::aDominio);
    }

    @Override
    public List<Categoria> listarDisponibles(Long usuarioId, TipoMovimiento tipo) {
        String consulta = "(predefinida = true or usuarioId = :usuario)";
        Parameters parametros = Parameters.with("usuario", usuarioId);
        if (tipo != null) {
            consulta += " and tipo = :tipo";
            parametros = parametros.and("tipo", tipo);
        }
        return find(consulta, parametros).list().stream()
                .map(this::aDominio)
                .sorted(ORDEN_EN_PANTALLA)
                .toList();
    }

    /**
     * Predefinidas primero, en el orden en que se sembraron (id), que es
     * el orden del enum CategoriaPredefinida. Despues las personalizadas
     * por nombre.
     */
    private static final Comparator<Categoria> ORDEN_EN_PANTALLA =
            Comparator.comparing((Categoria c) -> !c.isPredefinida())
                    .thenComparing(c -> c.isPredefinida() ? c.getId() : 0L)
                    .thenComparing(c -> c.getNombre().toLowerCase(Locale.ROOT))
                    .thenComparing(Categoria::getId);

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return deleteById(id);
    }

    Categoria aDominio(CategoriaEntity e) {
        return new Categoria(e.id, e.nombre, e.tipo, e.predefinida, e.usuarioId);
    }
}
