package edu.ucentral.zenfi.presupuesto.infraestructura.movimientos;

import edu.ucentral.zenfi.movimientos.aplicacion.MovimientoServicio;
import edu.ucentral.zenfi.presupuesto.dominio.puerto.GastosMensualesPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Adaptador de salida: implementa GastosMensualesPuerto usando el resumen
 * del modulo de movimientos (HU009).
 *
 * Es el UNICO punto de presupuesto que conoce clases de movimientos. Si
 * movimientos cambia, solo se ajusta esta clase.
 */
@ApplicationScoped
public class GastosMensualesAdaptador implements GastosMensualesPuerto {

    @Inject
    MovimientoServicio movimientos;

    @Override
    public BigDecimal totalGastos(Long usuarioId, YearMonth mes) {
        return movimientos.resumen(usuarioId, mes.atDay(1), mes.atEndOfMonth()).getTotalGastos();
    }
}
