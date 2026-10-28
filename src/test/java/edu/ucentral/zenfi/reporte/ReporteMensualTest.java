package edu.ucentral.zenfi.reporte;

import edu.ucentral.zenfi.presupuesto.dominio.modelo.EstadoPresupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import edu.ucentral.zenfi.reporte.dominio.modelo.GastoCategoria;
import edu.ucentral.zenfi.reporte.dominio.modelo.ReporteMensual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas unitarias del dominio de HU025: Java puro, sin Quarkus. */
class ReporteMensualTest {

    private static final YearMonth MES = YearMonth.of(2026, 10);

    private static final List<GastoCategoria> GASTOS = List.of(
            new GastoCategoria(1L, "Vivienda", new BigDecimal("600000.00"), new BigDecimal("60.00"), 1),
            new GastoCategoria(2L, "Alimentacion", new BigDecimal("400000.00"), new BigDecimal("40.00"), 5));

    private static Presupuesto presupuesto(String limite) {
        return new Presupuesto(1L, 1L, MES, new BigDecimal(limite));
    }

    @Test
    @DisplayName("El total gastado es la suma de las categorias")
    void totalEsLaSuma() {
        ReporteMensual reporte = ReporteMensual.generar(MES, GASTOS, null);

        assertEquals(new BigDecimal("1000000.00"), reporte.getTotalGastado());
        assertTrue(reporte.tieneGastos());
        assertEquals(2, reporte.getGastosPorCategoria().size());
    }

    @Test
    @DisplayName("Con presupuesto compara el limite contra el mismo total del desglose")
    void comparaConElPresupuesto() {
        ReporteMensual reporte = ReporteMensual.generar(MES, GASTOS, presupuesto("1250000.00"));

        EstadoPresupuesto estado = reporte.getPresupuesto().orElseThrow();
        assertEquals(reporte.getTotalGastado(), estado.totalGastado());
        assertEquals(new BigDecimal("80.00"), estado.porcentajeUtilizado());
        assertEquals(new BigDecimal("250000.00"), estado.disponible());
        assertFalse(estado.excedido());
    }

    @Test
    @DisplayName("Si el gasto supera el limite, la diferencia es negativa y se marca excedido")
    void presupuestoExcedido() {
        ReporteMensual reporte = ReporteMensual.generar(MES, GASTOS, presupuesto("800000.00"));

        EstadoPresupuesto estado = reporte.getPresupuesto().orElseThrow();
        assertEquals(new BigDecimal("-200000.00"), estado.disponible());
        assertEquals(new BigDecimal("125.00"), estado.porcentajeUtilizado());
        assertTrue(estado.excedido());
    }

    @Test
    @DisplayName("Sin presupuesto el reporte igual trae el desglose por categoria")
    void sinPresupuesto() {
        ReporteMensual reporte = ReporteMensual.generar(MES, GASTOS, null);

        assertTrue(reporte.getPresupuesto().isEmpty());
        assertEquals("Vivienda", reporte.getGastosPorCategoria().get(0).categoria());
    }

    @Test
    @DisplayName("Un mes sin gastos tiene total cero y el presupuesto al 0 %")
    void sinGastos() {
        ReporteMensual reporte = ReporteMensual.generar(MES, List.of(), presupuesto("500000.00"));

        assertFalse(reporte.tieneGastos());
        assertEquals(new BigDecimal("0.00"), reporte.getTotalGastado());
        assertEquals(new BigDecimal("0.00"), reporte.getPresupuesto().orElseThrow().porcentajeUtilizado());
    }

    @Test
    @DisplayName("Una lista nula de gastos se trata como un mes sin gastos")
    void gastosNulos() {
        ReporteMensual reporte = ReporteMensual.generar(MES, null, null);

        assertFalse(reporte.tieneGastos());
        assertEquals(new BigDecimal("0.00"), reporte.getTotalGastado());
    }

    @Test
    @DisplayName("El mes es obligatorio")
    void mesObligatorio() {
        assertThrows(IllegalArgumentException.class,
                () -> ReporteMensual.generar(null, GASTOS, null));
    }
}
