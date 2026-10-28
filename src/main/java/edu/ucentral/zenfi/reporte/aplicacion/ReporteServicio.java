package edu.ucentral.zenfi.reporte.aplicacion;

import edu.ucentral.zenfi.reporte.dominio.modelo.ReporteMensual;
import edu.ucentral.zenfi.reporte.dominio.puerto.GastosPorCategoriaPuerto;
import edu.ucentral.zenfi.reporte.dominio.puerto.PresupuestoMesPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.YearMonth;

/**
 * Capa de aplicacion de HU025: pide los datos a los puertos y deja que
 * el dominio (ReporteMensual) arme el reporte.
 */
@ApplicationScoped
public class ReporteServicio {

    @Inject
    GastosPorCategoriaPuerto gastos;

    @Inject
    PresupuestoMesPuerto presupuestos;

    /**
     * RQ-25: reporte de cualquier mes, pasado, actual o futuro. Un mes sin
     * presupuesto o sin gastos tambien tiene reporte; la app decide que
     * mensaje mostrar en cada caso.
     */
    public ReporteMensual generar(Long usuarioId, YearMonth mes) {
        return ReporteMensual.generar(mes,
                gastos.gastosPorCategoria(usuarioId, mes),
                presupuestos.buscar(usuarioId, mes).orElse(null));
    }
}
