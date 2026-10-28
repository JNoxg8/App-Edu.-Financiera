package edu.ucentral.zenfi.presupuesto.infraestructura.persistencia;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.repositorio.PresupuestoRepositorio;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.time.YearMonth;
import java.util.Optional;

/**
 * Adaptador de salida: implementa PresupuestoRepositorio con Panache y
 * traduce Presupuesto (dominio) <--> PresupuestoEntity (base de datos).
 */
@ApplicationScoped
public class PresupuestoPanache implements PresupuestoRepositorio, PanacheRepository<PresupuestoEntity> {

    private static final String POR_MES = "usuarioId = :usuario and anio = :anio and mes = :mes";

    @Override
    public Optional<Presupuesto> buscarPorMes(Long usuarioId, YearMonth mes) {
        return find(POR_MES, parametros(usuarioId, mes)).firstResultOptional().map(this::aDominio);
    }

    @Override
    public boolean existe(Long usuarioId, YearMonth mes) {
        return count(POR_MES, parametros(usuarioId, mes)) > 0;
    }

    /**
     * El servicio ya revisa que el mes no tenga presupuesto, pero entre esa
     * revision y el insert puede colarse otra peticion. En ese caso la
     * restriccion UNIQUE falla al hacer flush y se responde 409 en vez de 500.
     */
    @Override
    @Transactional
    public Presupuesto guardar(Presupuesto presupuesto) {
        PresupuestoEntity entidad = new PresupuestoEntity();
        entidad.usuarioId = presupuesto.getUsuarioId();
        entidad.anio = presupuesto.getMes().getYear();
        entidad.mes = presupuesto.getMes().getMonthValue();
        entidad.montoLimite = presupuesto.getMontoLimite();
        try {
            persistAndFlush(entidad);
        } catch (PersistenceException e) {
            throw new ReglaNegocioException("Ya existe un presupuesto para " + presupuesto.getMes());
        }
        return aDominio(entidad);
    }

    private static Parameters parametros(Long usuarioId, YearMonth mes) {
        return Parameters.with("usuario", usuarioId)
                .and("anio", mes.getYear())
                .and("mes", mes.getMonthValue());
    }

    private Presupuesto aDominio(PresupuestoEntity e) {
        return new Presupuesto(e.id, e.usuarioId, YearMonth.of(e.anio, e.mes), e.montoLimite);
    }
}
