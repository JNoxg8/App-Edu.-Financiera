package edu.ucentral.zenfi.presupuesto.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.EstadoPresupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.puerto.GastosMensualesPuerto;
import edu.ucentral.zenfi.presupuesto.dominio.repositorio.PresupuestoRepositorio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Capa de aplicacion de HU011: coordina los casos de uso.
 *
 * Igual que en deudas, aqui no hay calculos: el porcentaje utilizado lo
 * calcula el dominio (EstadoPresupuesto) y el total gastado lo aporta
 * movimientos a traves del puerto GastosMensualesPuerto.
 */
@ApplicationScoped
public class PresupuestoServicio {

    @Inject
    PresupuestoRepositorio repositorio;

    @Inject
    GastosMensualesPuerto gastos;

    /**
     * RQ-11: definir el presupuesto de un mes. Solo puede existir uno por
     * mes; la tabla tambien lo garantiza con una restriccion UNIQUE.
     */
    @Transactional
    public EstadoPresupuesto crear(Long usuarioId, YearMonth mes, BigDecimal montoLimite) {
        // El dominio valida mes, monto y que el mes no haya pasado
        Presupuesto presupuesto = Presupuesto.crear(usuarioId, mes, montoLimite, YearMonth.now());

        if (repositorio.existe(usuarioId, mes)) {
            throw new ReglaNegocioException("Ya existe un presupuesto para " + mes);
        }

        Presupuesto guardado = repositorio.guardar(presupuesto);
        return guardado.estado(gastos.totalGastos(usuarioId, mes));
    }

    /** Busca el presupuesto del mes o lanza 404 si no se ha definido. */
    public Presupuesto obtener(Long usuarioId, YearMonth mes) {
        return repositorio.buscarPorMes(usuarioId, mes)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay un presupuesto definido para " + mes));
    }

    /**
     * RQ-11: presupuesto del mes con lo gastado y el porcentaje utilizado.
     * Se recalcula en cada consulta, asi refleja cualquier movimiento
     * registrado, modificado o eliminado.
     */
    public EstadoPresupuesto consultar(Long usuarioId, YearMonth mes) {
        Presupuesto presupuesto = obtener(usuarioId, mes);
        return presupuesto.estado(gastos.totalGastos(usuarioId, mes));
    }
}
