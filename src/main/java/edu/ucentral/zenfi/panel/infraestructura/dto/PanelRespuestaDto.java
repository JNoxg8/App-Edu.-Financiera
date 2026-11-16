package edu.ucentral.zenfi.panel.infraestructura.dto;

import edu.ucentral.zenfi.panel.dominio.modelo.GastoCategoria;
import edu.ucentral.zenfi.panel.dominio.modelo.PanelPrincipal;
import edu.ucentral.zenfi.panel.dominio.modelo.ResumenDeuda;
import edu.ucentral.zenfi.panel.dominio.modelo.ResumenMeta;
import edu.ucentral.zenfi.presupuesto.infraestructura.dto.PresupuestoRespuestaDto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que devuelve GET /panel. El mes va como texto AAAA-MM.
 *
 * "presupuesto" usa el mismo formato que GET /presupuestos, asi la app
 * reutiliza su modelo; llega en null si el mes no tiene presupuesto.
 */
public record PanelRespuestaDto(
        String mes,
        BigDecimal balanceGeneral,
        BigDecimal totalIngresos,
        BigDecimal totalGastos,
        boolean tieneMovimientos,
        BigDecimal gastosMesActual,
        List<GastoCategoria> gastosMesPorCategoria,
        PresupuestoRespuestaDto presupuesto,
        List<ResumenMeta> metasActivas,
        List<ResumenDeuda> deudasPendientes
) {

    public static PanelRespuestaDto desde(PanelPrincipal p) {
        return new PanelRespuestaDto(
                p.mes().toString(),
                p.balanceGeneral(),
                p.totalIngresos(),
                p.totalGastos(),
                p.tieneMovimientos(),
                p.gastosMesActual(),
                p.gastosMesPorCategoria(),
                p.presupuesto() == null ? null : PresupuestoRespuestaDto.desde(p.presupuesto()),
                p.metasActivas(),
                p.deudasPendientes());
    }
}
