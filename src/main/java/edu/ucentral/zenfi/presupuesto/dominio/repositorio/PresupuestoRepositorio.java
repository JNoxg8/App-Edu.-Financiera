package edu.ucentral.zenfi.presupuesto.dominio.repositorio;

import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;

import java.time.YearMonth;
import java.util.Optional;

/**
 * Puerto de persistencia del presupuesto. Lo implementa PresupuestoPanache.
 * Gracias a esta interfaz el servicio se prueba con @InjectMock.
 */
public interface PresupuestoRepositorio {

    Optional<Presupuesto> buscarPorMes(Long usuarioId, YearMonth mes);

    boolean existe(Long usuarioId, YearMonth mes);

    Presupuesto guardar(Presupuesto presupuesto);
}
