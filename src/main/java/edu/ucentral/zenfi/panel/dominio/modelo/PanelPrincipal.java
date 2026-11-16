package edu.ucentral.zenfi.panel.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/**
 * HU015: resumen del estado financiero que se ve al abrir la app.
 *
 * Igual que el reporte de HU025, no repite calculos de otros modulos:
 * los totales vienen de movimientos (HU009), el avance de las metas de
 * HU004 y el saldo de las deudas de HU007. Aqui solo se juntan y se
 * calcula el balance general. Nada de esto se guarda.
 *
 * @param balanceGeneral  ingresos totales - gastos totales (puede ser negativo)
 * @param gastosMesActual gastos del mes, incluidos los abonos a deudas
 */
public record PanelPrincipal(YearMonth mes,
                             BigDecimal balanceGeneral,
                             BigDecimal totalIngresos,
                             BigDecimal totalGastos,
                             boolean tieneMovimientos,
                             BigDecimal gastosMesActual,
                             List<ResumenMeta> metasActivas,
                             List<ResumenDeuda> deudasPendientes) {

    private static final int ESCALA = 2;

    /**
     * @param historico totales de todos los movimientos del usuario
     * @param delMes    totales del mes actual
     */
    public static PanelPrincipal generar(YearMonth mes, TotalesMovimientos historico,
                                         TotalesMovimientos delMes,
                                         List<ResumenMeta> metasActivas,
                                         List<ResumenDeuda> deudasPendientes) {
        if (mes == null) {
            throw new IllegalArgumentException("El mes del panel es obligatorio");
        }
        TotalesMovimientos total = historico == null ? TotalesMovimientos.VACIO : historico;
        TotalesMovimientos mesActual = delMes == null ? TotalesMovimientos.VACIO : delMes;

        // La meta con la fecha limite mas cercana primero; lo mismo con las deudas
        List<ResumenMeta> metas = metasActivas == null ? List.of() : metasActivas.stream()
                .sorted(Comparator.comparing(ResumenMeta::fechaLimite))
                .toList();
        List<ResumenDeuda> deudas = deudasPendientes == null ? List.of() : deudasPendientes.stream()
                .sorted(Comparator.comparing(ResumenDeuda::fechaVencimiento))
                .toList();

        return new PanelPrincipal(
                mes,
                escala(total.ingresos().subtract(total.gastos())),
                escala(total.ingresos()),
                escala(total.gastos()),
                total.cantidad() > 0,
                escala(mesActual.gastos()),
                metas,
                deudas);
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
