package edu.ucentral.zenfi.perfil;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.perfil.dominio.modelo.Moneda;
import edu.ucentral.zenfi.perfil.dominio.modelo.NivelConocimiento;
import edu.ucentral.zenfi.perfil.dominio.modelo.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Pruebas unitarias del dominio: sin Quarkus ni base de datos. */
class PerfilTest {

    @Test
    @DisplayName("Crea el perfil con los datos de HU001 y quita espacios al nombre")
    void crearPerfilValido() {
        Perfil perfil = Perfil.crear(1L, "  Ana Gomez  ", Moneda.COP, NivelConocimiento.BASICO);

        assertEquals(1L, perfil.getId());
        assertEquals("Ana Gomez", perfil.getNombre());
        assertEquals(Moneda.COP, perfil.getMoneda());
        assertEquals(NivelConocimiento.BASICO, perfil.getNivel());
        assertNotNull(perfil.getFechaCreacion());
    }

    @Test
    @DisplayName("Rechaza un nombre vacio o con solo espacios")
    void nombreVacio() {
        assertThrows(ReglaNegocioException.class,
                () -> Perfil.crear(1L, "   ", Moneda.COP, NivelConocimiento.BASICO));
        assertThrows(ReglaNegocioException.class,
                () -> Perfil.crear(1L, null, Moneda.COP, NivelConocimiento.BASICO));
    }

    @Test
    @DisplayName("Rechaza un nombre de mas de 80 caracteres")
    void nombreMuyLargo() {
        String largo = "a".repeat(81);
        assertThrows(ReglaNegocioException.class,
                () -> Perfil.crear(1L, largo, Moneda.COP, NivelConocimiento.BASICO));
    }

    @Test
    @DisplayName("Exige seleccionar moneda y nivel")
    void monedaYNivelObligatorios() {
        assertThrows(ReglaNegocioException.class,
                () -> Perfil.crear(1L, "Ana", null, NivelConocimiento.BASICO));
        assertThrows(ReglaNegocioException.class,
                () -> Perfil.crear(1L, "Ana", Moneda.USD, null));
    }

    @Test
    @DisplayName("Cada moneda expone su simbolo")
    void simbolosDeMoneda() {
        assertEquals("$", Moneda.COP.getSimbolo());
        assertEquals("US$", Moneda.USD.getSimbolo());
        assertEquals("€", Moneda.EUR.getSimbolo());
    }
}