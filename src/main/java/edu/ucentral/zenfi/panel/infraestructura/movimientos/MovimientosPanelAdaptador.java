package edu.ucentral.zenfi.panel.infraestructura.movimientos;

import edu.ucentral.zenfi.movimientos.aplicacion.MovimientoServicio;
import edu.ucentral.zenfi.movimientos.dominio.modelo.ResumenMovimientos;
import edu.ucentral.zenfi.panel.dominio.modelo.TotalesMovimientos;
import edu.ucentral.zenfi.panel.dominio.puerto.MovimientosPanelPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.util.stream.Stream;

/**
 * Adaptador de salida: usa el resumen de HU009, que ya suma ingresos y
 * gastos (los abonos a deudas cuentan como gasto).
 */
@ApplicationScoped
public class MovimientosPanelAdaptador implements MovimientosPanelPuerto {

    @Inject
    MovimientoServicio movimientos;

    @Override
    public TotalesMovimientos totales(Long usuarioId, LocalDate desde, LocalDate hasta) {
        ResumenMovimientos resumen = movimientos.resumen(usuarioId, desde, hasta);
        int cantidad = Stream.concat(resumen.getIngresosPorCategoria().stream(),
                        resumen.getGastosPorCategoria().stream())
                .mapToInt(ResumenMovimientos.TotalCategoria::cantidadMovimientos)
                .sum();
        return new TotalesMovimientos(resumen.getTotalIngresos(), resumen.getTotalGastos(), cantidad);
    }
}
