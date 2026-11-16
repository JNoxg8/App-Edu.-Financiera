package edu.ucentral.zenfi.panel.infraestructura.dto;

import edu.ucentral.zenfi.panel.dominio.modelo.PanelPrincipal;
import edu.ucentral.zenfi.panel.dominio.modelo.ResumenDeuda;
import edu.ucentral.zenfi.panel.dominio.modelo.ResumenMeta;

import java.math.BigDecimal;
import java.util.List;

/** Lo que devuelve GET /panel. El mes va como texto AAAA-MM. */
public record PanelRespuestaDto(
        String mes,
        BigDecimal balanceGeneral,
        BigDecimal totalIngresos,
        BigDecimal totalGastos,
        boolean tieneMovimientos,
        BigDecimal gastosMesActual,
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
                p.metasActivas(),
                p.deudasPendientes());
    }
}
