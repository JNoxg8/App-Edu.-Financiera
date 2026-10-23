package edu.ucentral.zenfi.deudas;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.deudas.aplicacion.DeudaServicio;
import edu.ucentral.zenfi.deudas.dominio.modelo.Abono;
import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;
import edu.ucentral.zenfi.deudas.dominio.repositorio.DeudaRepositorio;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test del servicio con el repositorio SIMULADO (@InjectMock).
 *
 * No hay base de datos de por medio. Esto es posible unicamente
 * porque el servicio depende de la interfaz DeudaRepositorio y no
 * de la implementacion concreta con Panache.
 */
@QuarkusTest
class DeudaServicioTest {

    @Inject
    DeudaServicio servicio;

    @InjectMock
    DeudaRepositorio repositorio;

    private Deuda deudaPrueba;

    @BeforeEach
    void setup() {
        deudaPrueba = new Deuda(1L, "Banco Demo", new BigDecimal("1000000"),
                BigDecimal.ZERO, 10, LocalDate.now().plusYears(1), 1L, null);
    }

    @Test
    @DisplayName("Crear delega en el repositorio")
    void crear() {
        Mockito.when(repositorio.crear(any(Deuda.class))).thenReturn(deudaPrueba);

        Deuda resultado = servicio.crear(deudaPrueba);

        assertEquals("Banco Demo", resultado.getAcreedor());
        verify(repositorio, times(1)).crear(any(Deuda.class));
    }

    @Test
    @DisplayName("Obtener lanza 404 cuando la deuda no existe")
    void obtenerInexistente() {
        Mockito.when(repositorio.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(99L));
    }

    @Test
    @DisplayName("Listar devuelve las deudas del usuario")
    void listar() {
        Mockito.when(repositorio.listarPorUsuario(1L)).thenReturn(List.of(deudaPrueba));

        List<Deuda> resultado = servicio.listar(1L);

        assertEquals(1, resultado.size());
        assertEquals("Banco Demo", resultado.get(0).getAcreedor());
    }

    @Test
    @DisplayName("Registrar abono valida y persiste")
    void registrarAbono() {
        Mockito.when(repositorio.buscarPorId(1L)).thenReturn(Optional.of(deudaPrueba));

        servicio.registrarAbono(1L, new BigDecimal("250000"), LocalDate.now());

        verify(repositorio, times(1)).agregarAbono(anyLong(), any(Abono.class));
    }

    @Test
    @DisplayName("Eliminar una deuda inexistente lanza 404")
    void eliminarInexistente() {
        Mockito.when(repositorio.eliminar(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.eliminar(99L));
    }



}
