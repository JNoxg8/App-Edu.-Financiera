package edu.ucentral.zenfi.reporte.infraestructura.movimientos;

import edu.ucentral.zenfi.movimientos.aplicacion.MovimientoServicio;
import edu.ucentral.zenfi.reporte.dominio.modelo.GastoCategoria;
import edu.ucentral.zenfi.reporte.dominio.puerto.GastosPorCategoriaPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.YearMonth;
import java.util.List;

/**
 * Adaptador de salida: implementa GastosPorCategoriaPuerto con el resumen
 * del modulo de movimientos (HU009), que ya agrupa los gastos por
 * categoria, calcula su porcentaje y cuenta los abonos a deudas.
 *
 * Es el UNICO punto del reporte que conoce clases de movimientos.
 */
@ApplicationScoped
public class GastosPorCategoriaAdaptador implements GastosPorCategoriaPuerto {

    @Inject
    MovimientoServicio movimientos;

    @Override
    public List<GastoCategoria> gastosPorCategoria(Long usuarioId, YearMonth mes) {
        return movimientos.resumen(usuarioId, mes.atDay(1), mes.atEndOfMonth())
                .getGastosPorCategoria().stream()
                .map(t -> new GastoCategoria(t.categoriaId(), t.categoria(), t.total(),
                        t.porcentaje(), t.cantidadMovimientos()))
                .toList();
    }
}
