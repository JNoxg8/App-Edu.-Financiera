package edu.ucentral.zenfi.presupuesto;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.EstadoPresupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas unitarias del dominio: sin Quarkus ni base de datos. */
class PresupuestoTest {

    /** Mes "actual" fijo: el dominio no depende del reloj del servidor. */
    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    private static Presupuesto presupuesto(String limite) {
        return Presupuesto.crear(1L, OCTUBRE, new BigDecimal(limite), OCTUBRE);
    }

    // ------------------------------------------------------------------
    // Crear (HU011)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Crea el presupuesto del mes actual con el limite en 2 decimales")
    void crearValido() {
        Presupuesto p = presupuesto("1000000");

        assertEquals(1L, p.getUsuarioId());
        assertEquals(OCTUBRE, p.getMes());
        assertEquals(new BigDecimal("1000000.00"), p.getMontoLimite());
    }

    @Test
    @DisplayName("Acepta un mes futuro")
    void mesFuturo() {
        Presupuesto p = Presupuesto.crear(1L, YearMonth.of(2027, 1), BigDecimal.TEN, OCTUBRE);

        assertEquals(YearMonth.of(2027, 1), p.getMes());
    }

    @Test
    @DisplayName("Rechaza un mes que ya paso")
    void mesPasado() {
        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> Presupuesto.crear(1L, YearMonth.of(2026, 9), BigDecimal.TEN, OCTUBRE));

        assertTrue(error.getMessage().contains("2026-09"));
    }

    @Test
    @DisplayName("Exige el mes")
    void mesObligatorio() {
        assertThrows(ReglaNegocioException.class,
                () -> Presupuesto.crear(1L, null, BigDecimal.TEN, OCTUBRE));
    }

    @Test
    @DisplayName("Rechaza un monto limite vacio, cero o negativo")
    void montoInvalido() {
        assertThrows(ReglaNegocioException.class,
                () -> Presupuesto.crear(1L, OCTUBRE, null, OCTUBRE));
        assertThrows(ReglaNegocioException.class,
                () -> Presupuesto.crear(1L, OCTUBRE, BigDecimal.ZERO, OCTUBRE));
        assertThrows(ReglaNegocioException.class,
                () -> Presupuesto.crear(1L, OCTUBRE, new BigDecimal("-500"), OCTUBRE));
    }

    // ------------------------------------------------------------------
    // Estado: lo gastado frente al limite (HU011)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Ejemplo de RQ-11: limite 1.000.000 y 750.000 gastados = 75%")
    void porcentajeUtilizado() {
        EstadoPresupuesto estado = presupuesto("1000000").estado(new BigDecimal("750000"));

        assertEquals(new BigDecimal("750000.00"), estado.totalGastado());
        assertEquals(new BigDecimal("75.00"), estado.porcentajeUtilizado());
        assertEquals(new BigDecimal("250000.00"), estado.disponible());
        assertFalse(estado.excedido());
    }

    @Test
    @DisplayName("Sin gastos el porcentaje es 0 (tambien si el total llega null)")
    void sinGastos() {
        EstadoPresupuesto estado = presupuesto("500000").estado(null);

        assertEquals(new BigDecimal("0.00"), estado.totalGastado());
        assertEquals(new BigDecimal("0.00"), estado.porcentajeUtilizado());
        assertEquals(new BigDecimal("500000.00"), estado.disponible());
    }

    @Test
    @DisplayName("Gastar exactamente el limite es 100% pero NO es exceso")
    void exactamenteElLimite() {
        EstadoPresupuesto estado = presupuesto("800000").estado(new BigDecimal("800000"));

        assertEquals(new BigDecimal("100.00"), estado.porcentajeUtilizado());
        assertEquals(new BigDecimal("0.00"), estado.disponible());
        assertFalse(estado.excedido());
    }

    @Test
    @DisplayName("Al superar el limite pasa de 100%, el disponible es negativo y queda excedido")
    void limiteExcedido() {
        EstadoPresupuesto estado = presupuesto("1000000").estado(new BigDecimal("1150000"));

        assertEquals(new BigDecimal("115.00"), estado.porcentajeUtilizado());
        assertEquals(new BigDecimal("-150000.00"), estado.disponible());
        assertTrue(estado.excedido());
    }

    @Test
    @DisplayName("El porcentaje se redondea a 2 decimales (HALF_UP)")
    void redondeo() {
        // 100 / 300 = 33.333... -> 33.33 ; 200 / 300 = 66.666... -> 66.67
        assertEquals(new BigDecimal("33.33"),
                presupuesto("300").estado(new BigDecimal("100")).porcentajeUtilizado());
        assertEquals(new BigDecimal("66.67"),
                presupuesto("300").estado(new BigDecimal("200")).porcentajeUtilizado());
    }
}
