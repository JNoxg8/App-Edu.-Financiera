package edu.ucentral.zenfi.movimientos.infraestructura.persistencia;

import edu.ucentral.zenfi.movimientos.dominio.modelo.FiltroMovimientos;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Movimiento;
import edu.ucentral.zenfi.movimientos.dominio.repositorio.MovimientoRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Adaptador de salida: implementa MovimientoRepositorio con Panache y
 * traduce Movimiento (dominio) <--> MovimientoEntity (base de datos).
 */
@ApplicationScoped
public class MovimientoPanache implements MovimientoRepositorio, PanacheRepository<MovimientoEntity> {

    @Inject
    CategoriaPanache categorias;

    @Override
    @Transactional
    public Movimiento crear(Movimiento movimiento) {
        MovimientoEntity entidad = new MovimientoEntity();
        copiar(movimiento, entidad);
        entidad.usuarioId = movimiento.getUsuarioId();
        entidad.deudaId = movimiento.getDeudaId();
        entidad.abonoId = movimiento.getAbonoId();
        persist(entidad);
        return aDominio(entidad);
    }

    @Override
    @Transactional
    public Movimiento actualizar(Movimiento movimiento) {
        MovimientoEntity entidad = findById(movimiento.getId());
        if (entidad == null) {
            return null;
        }
        copiar(movimiento, entidad);
        // usuario, deuda y abono no se editan
        return aDominio(entidad);
    }

    @Override
    public Optional<Movimiento> buscarPorId(Long id) {
        return findByIdOptional(id).map(this::aDominio);
    }

    @Override
    public Optional<Movimiento> buscarPorAbono(Long abonoId) {
        return find("abonoId", abonoId).firstResultOptional().map(this::aDominio);
    }

    /**
     * Arma la consulta solo con los filtros que llegaron. Los valores van
     * siempre como parametros (:nombre), nunca pegados al texto de la
     * consulta: asi no hay riesgo de inyeccion SQL.
     */
    @Override
    public List<Movimiento> listar(FiltroMovimientos filtro) {
        StringBuilder consulta = new StringBuilder("usuarioId = :usuario");
        Parameters parametros = Parameters.with("usuario", filtro.usuarioId());

        if (filtro.tipo() != null) {
            consulta.append(" and tipo = :tipo");
            parametros.and("tipo", filtro.tipo());
        }
        if (filtro.categoriaId() != null) {
            consulta.append(" and categoria.id = :categoria");
            parametros.and("categoria", filtro.categoriaId());
        }
        if (filtro.desde() != null) {
            consulta.append(" and fecha >= :desde");
            parametros.and("desde", filtro.desde());
        }
        if (filtro.hasta() != null) {
            consulta.append(" and fecha <= :hasta");
            parametros.and("hasta", filtro.hasta());
        }
        if (filtro.texto() != null) {
            consulta.append(" and (lower(descripcion) like :texto escape '\\'"
                    + " or lower(categoria.nombre) like :texto escape '\\')");
            parametros.and("texto", "%" + escaparComodines(filtro.texto()) + "%");
        }

        return find(consulta.toString(), orden(filtro), parametros).list().stream()
                .map(this::aDominio)
                .toList();
    }

    @Override
    public long contarPorCategoria(Long categoriaId) {
        return count("categoria.id", categoriaId);
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        return deleteById(id);
    }

    @Override
    @Transactional
    public boolean eliminarPorAbono(Long abonoId) {
        return delete("abonoId", abonoId) > 0;
    }

    @Override
    @Transactional
    public int desvincularDeuda(Long deudaId) {
        return update("deudaId = null, abonoId = null where deudaId = ?1", deudaId);
    }

    // ------------------------------------------------------------------

    /**
     * El segundo criterio desempata: dos gastos del mismo dia salen del
     * mas reciente al mas antiguo (id mayor = registrado despues).
     */
    private Sort orden(FiltroMovimientos filtro) {
        Sort.Direction direccion = filtro.direccion() == FiltroMovimientos.Direccion.ASC
                ? Sort.Direction.Ascending
                : Sort.Direction.Descending;

        return switch (filtro.orden()) {
            case MONTO -> Sort.by("monto", direccion).and("fecha", Sort.Direction.Descending)
                    .and("id", Sort.Direction.Descending);
            case CATEGORIA -> Sort.by("categoria.nombre", direccion).and("fecha", Sort.Direction.Descending)
                    .and("id", Sort.Direction.Descending);
            case FECHA -> Sort.by("fecha", direccion).and("id", direccion);
        };
    }

    /** Que un "%" o "_" escrito por el usuario se busque literal y no como comodin. */
    private static String escaparComodines(String texto) {
        return texto.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private void copiar(Movimiento m, MovimientoEntity e) {
        e.tipo = m.getTipo();
        e.monto = m.getMonto();
        e.fecha = m.getFecha();
        e.descripcion = m.getDescripcion();
        e.categoria = categorias.findById(m.getCategoria().getId());
    }

    private Movimiento aDominio(MovimientoEntity e) {
        return new Movimiento(
                e.id,
                e.tipo,
                e.monto,
                e.fecha,
                e.descripcion,
                categorias.aDominio(e.categoria),
                e.usuarioId,
                e.deudaId,
                e.abonoId
        );
    }
}
