package edu.ucentral.zenfi.perfil;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.perfil.aplicacion.PerfilServicio;
import edu.ucentral.zenfi.perfil.dominio.modelo.Moneda;
import edu.ucentral.zenfi.perfil.dominio.modelo.NivelConocimiento;
import edu.ucentral.zenfi.perfil.dominio.modelo.Perfil;
import edu.ucentral.zenfi.perfil.dominio.repositorio.PerfilRepositorio;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Servicio con el repositorio simulado: no hay base de datos. */
@QuarkusTest
class PerfilServicioTest {

    @Inject
    PerfilServicio servicio;

    @InjectMock
    PerfilRepositorio repositorio;

    @Test
    @DisplayName("Consultar un perfil que no existe lanza 404")
    void obtenerInexistente() {
        Mockito.when(repositorio.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtener(1L));
    }

    @Test
    @DisplayName("Crear un perfil nuevo lo guarda")
    void crearNuevo() {
        Mockito.when(repositorio.existe(1L)).thenReturn(false);
        Mockito.when(repositorio.guardar(any(Perfil.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Perfil creado = servicio.crear(1L, "Ana", Moneda.EUR, NivelConocimiento.AVANZADO);

        assertEquals("Ana", creado.getNombre());
        assertEquals(Moneda.EUR, creado.getMoneda());
        verify(repositorio, times(1)).guardar(any(Perfil.class));
    }

    @Test
    @DisplayName("No permite crear el perfil dos veces (409) y no guarda nada")
    void crearDuplicado() {
        Mockito.when(repositorio.existe(1L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(1L, "Ana", Moneda.COP, NivelConocimiento.BASICO));
        verify(repositorio, never()).guardar(any(Perfil.class));
    }
}