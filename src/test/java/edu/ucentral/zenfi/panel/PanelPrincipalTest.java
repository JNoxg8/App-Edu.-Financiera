package edu.ucentral.zenfi.panel;

import edu.ucentral.zenfi.panel.dominio.modelo.PanelPrincipal;
import edu.ucentral.zenfi.panel.dominio.modelo.ResumenDeuda;
import edu.ucentral.zenfi.panel.dominio.modelo.ResumenMeta;
import edu.ucentral.zenfi.panel.dominio.modelo.TotalesMovimientos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas unitarias del panel principal (HU015). Sin Quarkus ni base de datos. */
class PanelPrincipalTest {

    private static final YearMonth MES = YearMonth.of(2026, 11);
    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("Balance general = ingresos totales - gastos totales, con 2 decimales")
    void balance() {
        PanelPrincipal panel = PanelPrincipal.generar(MES,
                new TotalesMovimientos(bd("3000000"), bd("1250000.5"), 8),
                new TotalesMovimientos(bd("1000000"), bd("400000"), 3),
                List.of(), List.of());

        assertEquals(bd("1749999.50"), panel.balanceGeneral());
        assertEquals(bd("400000.00"), panel.gastosMesActual());
        assertTrue(panel.tieneMovimientos());
    }

    @Test
    @DisplayName("Si se gasto mas de lo que entro, el balance es negativo")
    void balanceNegativo() {
        PanelPrincipal panel = PanelPrincipal.generar(MES,
                new TotalesMovimientos(bd("500000"), bd("800000"), 2),
                TotalesMovimientos.VACIO, List.of(), List.of());

        assertEquals(bd("-300000.00"), panel.balanceGeneral());
    }

    @Test
    @DisplayName("Sin informacion: balance y gastos en 0 y listas vacias (no null)")
    void sinInformacion() {
        PanelPrincipal panel = PanelPrincipal.generar(MES, null, null, null, null);

        assertFalse(panel.tieneMovimientos());
        assertEquals(bd("0.00"), panel.balanceGeneral());
        assertEquals(bd("0.00"), panel.gastosMesActual());
        assertTrue(panel.metasActivas().isEmpty());
        assertTrue(panel.deudasPendientes().isEmpty());
    }

    @Test
    @DisplayName("Metas y deudas se ordenan de la fecha mas cercana a la mas lejana")
    void orden() {
        ResumenMeta lejana = ResumenMeta.de(1L, "Casa", bd("1000"), bd("100"), HOY.plusYears(2));
        ResumenMeta cercana = ResumenMeta.de(2L, "Viaje", bd("1000"), bd("100"), HOY.plusMonths(1));
        ResumenDeuda d1 = new ResumenDeuda(1L, "Banco", bd("500"), bd("50"), HOY.plusYears(1));
        ResumenDeuda d2 = new ResumenDeuda(2L, "Tarjeta", bd("200"), bd("80"), HOY.plusDays(10));

        PanelPrincipal panel = PanelPrincipal.generar(MES, TotalesMovimientos.VACIO,
                TotalesMovimientos.VACIO, List.of(lejana, cercana), List.of(d1, d2));

        assertEquals("Viaje", panel.metasActivas().get(0).nombre());
        assertEquals("Tarjeta", panel.deudasPendientes().get(0).acreedor());
    }

    @Test
    @DisplayName("El avance de una meta es ahorrado / objetivo (como en HU004)")
    void avanceMeta() {
        assertEquals(bd("33.33"),
                ResumenMeta.de(1L, "Moto", bd("3000"), bd("1000"), HOY).porcentaje());
        assertEquals(bd("0.00"),
                ResumenMeta.de(1L, "Moto", bd("3000"), BigDecimal.ZERO, HOY).porcentaje());
    }

    @Test
    @DisplayName("El mes es obligatorio")
    void mesObligatorio() {
        assertThrows(IllegalArgumentException.class,
                () -> PanelPrincipal.generar(null, null, null, null, null));
    }
}
