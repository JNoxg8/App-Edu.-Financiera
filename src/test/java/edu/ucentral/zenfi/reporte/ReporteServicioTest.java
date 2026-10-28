package edu.ucentral.zenfi.reporte;

import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import edu.ucentral.zenfi.reporte.aplicacion.ReporteServicio;
import edu.ucentral.zenfi.reporte.dominio.modelo.GastoCategoria;
import edu.ucentral.zenfi.reporte.dominio.modelo.ReporteMensual;
import edu.ucentral.zenfi.reporte.dominio.puerto.GastosPorCategoriaPuerto;
import edu.ucentral.zenfi.reporte.dominio.puerto.PresupuestoMesPuerto;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Servicio con los dos puertos simulados: no se tocan los modulos de
 * movimientos ni de presupuesto.
 */
@QuarkusTest
class ReporteServicioTest {

    private static final YearMonth MES = YearMonth.of(2026, 10);

    @Inject
    ReporteServicio servicio;

    @InjectMock
    GastosPorCategoriaPuerto gastos;

    @InjectMock
    PresupuestoMesPuerto presupuestos;

    @Test
    @DisplayName("Junta los gastos por categoria con el presupuesto del mes")
    void conPresupuesto() {
        Mockito.when(gastos.gastosPorCategoria(1L, MES)).thenReturn(List.of(
                new GastoCategoria(3L, "Transporte", new BigDecimal("300000.00"),
                        new BigDecimal("100.00"), 4)));
        Mockito.when(presupuestos.buscar(1L, MES)).thenReturn(Optional.of(
                new Presupuesto(9L, 1L, MES, new BigDecimal("1000000.00"))));

        ReporteMensual reporte = servicio.generar(1L, MES);

        assertEquals(new BigDecimal("300000.00"), reporte.getTotalGastado());
        assertEquals(new BigDecimal("30.00"),
                reporte.getPresupuesto().orElseThrow().porcentajeUtilizado());
    }

    @Test
    @DisplayName("Un mes sin presupuesto no es un error: el reporte llega sin comparacion")
    void sinPresupuesto() {
        Mockito.when(gastos.gastosPorCategoria(1L, MES)).thenReturn(List.of());
        Mockito.when(presupuestos.buscar(1L, MES)).thenReturn(Optional.empty());

        ReporteMensual reporte = servicio.generar(1L, MES);

        assertTrue(reporte.getPresupuesto().isEmpty());
        assertFalse(reporte.tieneGastos());
    }
}
