package edu.ucentral.zenfi.movimientos;

import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Movimiento;
import edu.ucentral.zenfi.movimientos.dominio.modelo.ResumenMovimientos;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test UNITARIO puro de los calculos del resumen. */
class ResumenMovimientosTest {

    private static final LocalDate HOY = LocalDate.now();

    private final Categoria salario = new Categoria(1L, "Salario", TipoMovimiento.INGRESO, true, null);
    private final Categoria comida = new Categoria(2L, "Alimentación", TipoMovimiento.GASTO, true, null);
    private final Categoria deudas = new Categoria(3L, "Deudas", TipoMovimiento.GASTO, true, null);

    private Movimiento mov(TipoMovimiento tipo, String monto, Categoria c) {
        return new Movimiento(null, tipo, new BigDecimal(monto), HOY, null, c, 1L, null, null);
    }

    @Test
    @DisplayName("Calcula totales, balance y porcentaje gastado; los abonos cuentan como gasto")
    void totales() {
        ResumenMovimientos r = ResumenMovimientos.calcular(null, null, List.of(
                mov(TipoMovimiento.INGRESO, "2000000", salario),
                mov(TipoMovimiento.GASTO, "300000", comida),
                mov(TipoMovimiento.GASTO, "100000", comida),
                mov(TipoMovimiento.GASTO, "600000", deudas)));

        assertEquals(new BigDecimal("2000000.00"), r.getTotalIngresos());
        assertEquals(new BigDecimal("1000000.00"), r.getTotalGastos());
        assertEquals(new BigDecimal("1000000.00"), r.balance());
        assertEquals(new BigDecimal("50.00"), r.porcentajeGastado());
    }

    @Test
    @DisplayName("Agrupa los gastos por categoria, de mayor a menor")
    void porCategoria() {
        ResumenMovimientos r = ResumenMovimientos.calcular(null, null, List.of(
                mov(TipoMovimiento.GASTO, "300000", comida),
                mov(TipoMovimiento.GASTO, "100000", comida),
                mov(TipoMovimiento.GASTO, "600000", deudas)));

        List<ResumenMovimientos.TotalCategoria> gastos = r.getGastosPorCategoria();
        assertEquals(2, gastos.size());

        assertEquals("Deudas", gastos.get(0).categoria());
        assertEquals(new BigDecimal("60.00"), gastos.get(0).porcentaje());

        assertEquals("Alimentación", gastos.get(1).categoria());
        assertEquals(new BigDecimal("400000.00"), gastos.get(1).total());
        assertEquals(2, gastos.get(1).cantidadMovimientos());
    }

    @Test
    @DisplayName("Sin movimientos todo queda en cero y no divide entre cero")
    void vacio() {
        ResumenMovimientos r = ResumenMovimientos.calcular(null, null, List.of());

        assertEquals(new BigDecimal("0.00"), r.balance());
        assertEquals(new BigDecimal("0.00"), r.porcentajeGastado());
        assertEquals(0, r.getGastosPorCategoria().size());
    }

    @Test
    @DisplayName("Si se gasta mas de lo que entra, el balance es negativo")
    void balanceNegativo() {
        ResumenMovimientos r = ResumenMovimientos.calcular(null, null, List.of(
                mov(TipoMovimiento.INGRESO, "100000", salario),
                mov(TipoMovimiento.GASTO, "150000", comida)));

        assertEquals(new BigDecimal("-50000.00"), r.balance());
        assertEquals(new BigDecimal("150.00"), r.porcentajeGastado());
    }
}
