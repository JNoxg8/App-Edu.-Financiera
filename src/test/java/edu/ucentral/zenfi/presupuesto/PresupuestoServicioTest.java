package edu.ucentral.zenfi.presupuesto;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.presupuesto.aplicacion.PresupuestoServicio;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.EstadoPresupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.puerto.GastosMensualesPuerto;
import edu.ucentral.zenfi.presupuesto.dominio.repositorio.PresupuestoRepositorio;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Servicio con el repositorio y el puerto de movimientos simulados:
 * no hay base de datos ni se toca el modulo de movimientos.
 */
@QuarkusTest
class PresupuestoServicioTest {

    private static final YearMonth MES_ACTUAL = YearMonth.now();

    @Inject
    PresupuestoServicio servicio;

    @InjectMock
    PresupuestoRepositorio repositorio;

    @InjectMock
    GastosMensualesPuerto gastos;

    @Test
    @DisplayName("Crear un presupuesto lo guarda y devuelve lo ya gastado en el mes")
    void crearNuevo() {
        Mockito.when(repositorio.existe(1L, MES_ACTUAL)).thenReturn(false);
        Mockito.when(repositorio.guardar(any(Presupuesto.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        Mockito.when(gastos.totalGastos(1L, MES_ACTUAL)).thenReturn(new BigDecimal("200000"));

        EstadoPresupuesto estado = servicio.crear(1L, MES_ACTUAL, new BigDecimal("1000000"));

        assertEquals(new BigDecimal("200000.00"), estado.totalGastado());
        assertEquals(new BigDecimal("20.00"), estado.porcentajeUtilizado());
        verify(repositorio, times(1)).guardar(any(Presupuesto.class));
    }

    @Test
    @DisplayName("No permite dos presupuestos para el mismo mes (409) y no guarda nada")
    void crearDuplicado() {
        Mockito.when(repositorio.existe(1L, MES_ACTUAL)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, MES_ACTUAL, new BigDecimal("1000000")));
        verify(repositorio, never()).guardar(any(Presupuesto.class));
    }

    @Test
    @DisplayName("Un mes que ya paso se rechaza antes de consultar el repositorio")
    void crearMesPasado() {
        YearMonth mesPasado = MES_ACTUAL.minusMonths(1);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, mesPasado, new BigDecimal("1000000")));
        verify(repositorio, never()).existe(any(), any());
        verify(repositorio, never()).guardar(any(Presupuesto.class));
    }

    @Test
    @DisplayName("Consultar un mes sin presupuesto lanza 404")
    void consultarInexistente() {
        Mockito.when(repositorio.buscarPorMes(1L, MES_ACTUAL)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.consultar(1L, MES_ACTUAL));
    }

    @Test
    @DisplayName("Consultar recalcula el porcentaje con los gastos actuales del mes")
    void consultarRecalcula() {
        Presupuesto guardado = new Presupuesto(7L, 1L, MES_ACTUAL, new BigDecimal("1000000.00"));
        Mockito.when(repositorio.buscarPorMes(1L, MES_ACTUAL)).thenReturn(Optional.of(guardado));
        Mockito.when(gastos.totalGastos(1L, MES_ACTUAL))
                .thenReturn(new BigDecimal("750000"))
                .thenReturn(new BigDecimal("1150000"));

        EstadoPresupuesto antes = servicio.consultar(1L, MES_ACTUAL);
        EstadoPresupuesto despues = servicio.consultar(1L, MES_ACTUAL);

        assertEquals(new BigDecimal("75.00"), antes.porcentajeUtilizado());
        assertEquals(new BigDecimal("115.00"), despues.porcentajeUtilizado());
        assertEquals(true, despues.excedido());
    }
}
