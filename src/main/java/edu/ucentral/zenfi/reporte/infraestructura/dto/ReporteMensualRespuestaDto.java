package edu.ucentral.zenfi.reporte.infraestructura.dto;

import edu.ucentral.zenfi.presupuesto.infraestructura.dto.PresupuestoRespuestaDto;
import edu.ucentral.zenfi.reporte.dominio.modelo.GastoCategoria;
import edu.ucentral.zenfi.reporte.dominio.modelo.ReporteMensual;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reporte mensual que se devuelve a Flutter.
 *
 * "presupuesto" usa el mismo formato que GET /presupuestos, asi la app
 * reutiliza su modelo; llega en null si el mes no tiene presupuesto.
 * "tieneGastos" evita que la app tenga que deducirlo de la lista.
 */
public record ReporteMensualRespuestaDto(
        String mes,
        BigDecimal totalGastado,
        boolean tieneGastos,
        PresupuestoRespuestaDto presupuesto,
        List<GastoCategoria> gastosPorCategoria
) {
    public static ReporteMensualRespuestaDto desde(ReporteMensual r) {
        return new ReporteMensualRespuestaDto(
                r.getMes().toString(),
                r.getTotalGastado(),
                r.tieneGastos(),
                r.getPresupuesto().map(PresupuestoRespuestaDto::desde).orElse(null),
                r.getGastosPorCategoria());
    }
}
