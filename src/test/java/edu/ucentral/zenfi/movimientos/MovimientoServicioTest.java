package edu.ucentral.zenfi.movimientos;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.movimientos.aplicacion.CategoriaServicio;
import edu.ucentral.zenfi.movimientos.aplicacion.MovimientoServicio;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Movimiento;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import edu.ucentral.zenfi.movimientos.dominio.puerto.PagoDeudaPuerto;
import edu.ucentral.zenfi.movimientos.dominio.repositorio.CategoriaRepositorio;
import edu.ucentral.zenfi.movimientos.dominio.repositorio.MovimientoRepositorio;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Servicios de HU009 con los repositorios y el puerto de deudas
 * SIMULADOS: no hay base de datos ni modulo de deudas de por medio.
 */
@QuarkusTest
class MovimientoServicioTest {

    private static final LocalDate HOY = LocalDate.now();

    @Inject
    MovimientoServicio servicio;

    @Inject
    CategoriaServicio categoriaServicio;

    @InjectMock
    MovimientoRepositorio repositorio;

    @InjectMock
    CategoriaRepositorio categorias;

    @InjectMock
    PagoDeudaPuerto pagoDeuda;

    private final Categoria transporte = new Categoria(1L, "Transporte", TipoMovimiento.GASTO, true, null);
    private final Categoria deudas = new Categoria(2L, "Deudas", TipoMovimiento.GASTO, true, null);
    private final Categoria ajena = new Categoria(3L, "Ajena", TipoMovimiento.GASTO, false, 99L);

    @BeforeEach
    void setup() {
        Mockito.when(categorias.buscarPorId(1L)).thenReturn(Optional.of(transporte));
        Mockito.when(categorias.buscarPorId(2L)).thenReturn(Optional.of(deudas));
        Mockito.when(categorias.buscarPorId(3L)).thenReturn(Optional.of(ajena));
        Mockito.when(repositorio.crear(any(Movimiento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        Mockito.when(repositorio.actualizar(any(Movimiento.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("Crear un gasto normal lo guarda sin tocar deudas")
    void crearGasto() {
        Movimiento m = servicio.crear(1L, TipoMovimiento.GASTO, new BigDecimal("3000"), HOY,
                "Bus", 1L, null);

        assertEquals("Transporte", m.getCategoria().getNombre());
        verify(repositorio, times(1)).crear(any(Movimiento.class));
        verify(pagoDeuda, never()).registrarAbono(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("No se puede usar la categoria personalizada de otro usuario (404)")
    void categoriaAjena() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crear(1L, TipoMovimiento.GASTO, new BigDecimal("3000"), HOY,
                        null, 3L, null));
        verify(repositorio, never()).crear(any(Movimiento.class));
    }

    @Test
    @DisplayName("Un gasto en Deudas registra el abono y devuelve el gasto que genero")
    void crearGastoDeDeuda() {
        Movimiento generado = new Movimiento(50L, TipoMovimiento.GASTO, new BigDecimal("80000.00"),
                HOY, "Abono a deuda: Banco", deudas, 1L, 7L, 500L);
        Mockito.when(pagoDeuda.registrarAbono(1L, 7L, new BigDecimal("80000"), HOY)).thenReturn(500L);
        Mockito.when(repositorio.buscarPorAbono(500L)).thenReturn(Optional.of(generado));

        Movimiento m = servicio.crear(1L, TipoMovimiento.GASTO, new BigDecimal("80000"), HOY,
                "Cuota de octubre", 2L, 7L);

        assertEquals(500L, m.getAbonoId());
        assertEquals("Cuota de octubre", m.getDescripcion());
        // El gasto lo crea el evento, no el servicio: no debe duplicarse
        verify(repositorio, never()).crear(any(Movimiento.class));
    }

    @Test
    @DisplayName("Solo la categoria Deudas acepta deudaId (409)")
    void deudaConOtraCategoria() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, TipoMovimiento.GASTO, new BigDecimal("3000"), HOY,
                        null, 1L, 7L));
        verify(pagoDeuda, never()).registrarAbono(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("Eliminar un abono lo revierte en la deuda")
    void eliminarAbono() {
        Movimiento abono = new Movimiento(50L, TipoMovimiento.GASTO, new BigDecimal("80000.00"),
                HOY, null, deudas, 1L, 7L, 500L);
        Mockito.when(repositorio.buscarPorId(50L)).thenReturn(Optional.of(abono));

        servicio.eliminar(1L, 50L);

        verify(pagoDeuda, times(1)).revertirAbono(7L, 500L);
    }

    @Test
    @DisplayName("Eliminar un gasto normal lo borra del repositorio")
    void eliminarNormal() {
        Movimiento gasto = new Movimiento(51L, TipoMovimiento.GASTO, new BigDecimal("3000.00"),
                HOY, null, transporte, 1L, null, null);
        Mockito.when(repositorio.buscarPorId(51L)).thenReturn(Optional.of(gasto));

        servicio.eliminar(1L, 51L);

        verify(repositorio, times(1)).eliminar(51L);
        verify(pagoDeuda, never()).revertirAbono(anyLong(), anyLong());
    }

    @Test
    @DisplayName("El movimiento de otro usuario responde 404")
    void movimientoAjeno() {
        Movimiento deOtro = new Movimiento(52L, TipoMovimiento.GASTO, new BigDecimal("3000.00"),
                HOY, null, transporte, 99L, null, null);
        Mockito.when(repositorio.buscarPorId(52L)).thenReturn(Optional.of(deOtro));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(1L, 52L));
    }

    @Test
    @DisplayName("No permite crear una categoria con un nombre repetido")
    void categoriaRepetida() {
        Mockito.when(categorias.listarDisponibles(1L, TipoMovimiento.GASTO))
                .thenReturn(List.of(transporte, deudas));

        assertThrows(ReglaNegocioException.class,
                () -> categoriaServicio.crear(1L, "  TRANSPORTE ", TipoMovimiento.GASTO));
        verify(categorias, never()).crear(any(Categoria.class));
    }

    @Test
    @DisplayName("No permite eliminar una categoria con movimientos")
    void eliminarCategoriaEnUso() {
        Categoria mascotas = new Categoria(10L, "Mascotas", TipoMovimiento.GASTO, false, 1L);
        Mockito.when(categorias.buscarPorId(10L)).thenReturn(Optional.of(mascotas));
        Mockito.when(repositorio.contarPorCategoria(10L)).thenReturn(3L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> categoriaServicio.eliminar(1L, 10L));

        assertEquals(true, ex.getMessage().contains("3 movimiento"));
        verify(categorias, never()).eliminar(eq(10L));
    }
}
