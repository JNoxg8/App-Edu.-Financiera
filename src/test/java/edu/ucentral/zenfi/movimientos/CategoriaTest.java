package edu.ucentral.zenfi.movimientos;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test UNITARIO puro del modelo Categoria. */
class CategoriaTest {

    @Test
    @DisplayName("Crea una categoria personalizada limpiando los espacios")
    void personalizada() {
        Categoria c = Categoria.personalizada("  Mascotas   y  hogar ", TipoMovimiento.GASTO, 1L);

        assertEquals("Mascotas y hogar", c.getNombre());
        assertFalse(c.isPredefinida());
        assertEquals(1L, c.getUsuarioId());
    }

    @Test
    @DisplayName("El nombre no puede estar vacio")
    void nombreVacio() {
        assertThrows(ReglaNegocioException.class,
                () -> Categoria.personalizada("   ", TipoMovimiento.GASTO, 1L));
        assertThrows(ReglaNegocioException.class,
                () -> Categoria.personalizada(null, TipoMovimiento.GASTO, 1L));
    }

    @Test
    @DisplayName("Compara nombres sin importar mayusculas, tildes ni espacios")
    void mismoNombre() {
        Categoria c = new Categoria(1L, "Alimentación", TipoMovimiento.GASTO, true, null);

        assertTrue(c.tieneMismoNombre("alimentacion"));
        assertTrue(c.tieneMismoNombre("  ALIMENTACIÓN "));
        assertFalse(c.tieneMismoNombre("Alimentos"));
    }

    @Test
    @DisplayName("Una categoria predefinida no se puede renombrar")
    void predefinidaNoEditable() {
        Categoria c = new Categoria(1L, "Transporte", TipoMovimiento.GASTO, true, null);

        assertThrows(ReglaNegocioException.class, () -> c.renombrar("Movilidad"));
    }

    @Test
    @DisplayName("Solo la predefinida Deudas es la categoria puente con deudas")
    void esDeDeudas() {
        assertTrue(new Categoria(1L, "Deudas", TipoMovimiento.GASTO, true, null).esDeDeudas());
        assertFalse(new Categoria(2L, "Deudas", TipoMovimiento.GASTO, false, 1L).esDeDeudas());
    }

    @Test
    @DisplayName("Un usuario no puede usar la categoria personalizada de otro")
    void disponiblePara() {
        Categoria propia = new Categoria(5L, "Mascotas", TipoMovimiento.GASTO, false, 1L);
        Categoria predefinida = new Categoria(1L, "Salud", TipoMovimiento.GASTO, true, null);

        assertTrue(propia.disponiblePara(1L));
        assertFalse(propia.disponiblePara(2L));
        assertTrue(predefinida.disponiblePara(2L));
    }
}
