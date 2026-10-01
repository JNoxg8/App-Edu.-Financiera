package edu.ucentral.zenfi.movimientos.infraestructura.dto;

import edu.ucentral.zenfi.movimientos.dominio.modelo.ResumenMovimientos;
import edu.ucentral.zenfi.movimientos.dominio.modelo.ResumenMovimientos.TotalCategoria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen de un periodo: cuanto entro, cuanto salio y en que.
 * Todos los valores se calculan al armar la respuesta.
 */
public record ResumenRespuestaDto(
        LocalDate desde,
        LocalDate hasta,
        BigDecimal totalIngresos,
        BigDecimal totalGastos,
        BigDecimal balance,
        BigDecimal porcentajeGastado,
        List<TotalCategoria> ingresosPorCategoria,
        List<TotalCategoria> gastosPorCategoria
) {
    public static ResumenRespuestaDto desde(ResumenMovimientos r) {
        return new ResumenRespuestaDto(
                r.getDesde(),
                r.getHasta(),
                r.getTotalIngresos(),
                r.getTotalGastos(),
                r.balance(),
                r.porcentajeGastado(),
                r.getIngresosPorCategoria(),
                r.getGastosPorCategoria());
    }
}
