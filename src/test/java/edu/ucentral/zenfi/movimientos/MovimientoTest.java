package edu.ucentral.zenfi.movimientos;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Movimiento;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test UNITARIO puro del modelo Movimiento: sin Quarkus ni base de datos. */
class MovimientoTest {

    private static final LocalDate HOY = LocalDate.now();

    private final Categoria transporte = new Categoria(1L, "Transporte", TipoMovimiento.GASTO, true, null);
    private final Categoria salario = new Categoria(2L, "Salario", TipoMovimiento.INGRESO, true, null);
    private final Categoria deudas = new Categoria(3L, "Deudas", TipoMovimiento.GASTO, true, null);

    private Movimiento gasto(String monto) {
        return Movimiento.registrar(TipoMovimiento.GASTO, new BigDecimal(monto), HOY,
                "Bus", transporte, 1L);
    }

    private Movimiento abono() {
        return new Movimiento(10L, TipoMovimiento.GASTO, new BigDecimal("50000.00"), HOY,
                "Abono a deuda: Banco", deudas, 1L, 7L, 99L);
    }

    @Test
    @DisplayName("Registra un gasto valido con el monto en dos decimales")
    void registrarGasto() {
        Movimiento m = gasto("2500");

        assertEquals(TipoMovimiento.GASTO, m.getTipo());
        assertEquals(new BigDecimal("2500.00"), m.getMonto());
        assertEquals("Transporte", m.getCategoria().getNombre());
        assertFalse(m.esAbonoDeDeuda());
    }

    @Test
    @DisplayName("El monto debe ser mayor a cero")
    void montoInvalido() {
        assertThrows(ReglaNegocioException.class, () -> gasto("0"));
        assertThrows(ReglaNegocioException.class, () -> gasto("-10"));
    }

    @Test
    @DisplayName("Tipo, fecha y categoria son obligatorios")
    void camposObligatorios() {
        BigDecimal monto = new BigDecimal("1000");

        assertThrows(ReglaNegocioException.class,
                () -> Movimiento.registrar(null, monto, HOY, null, transporte, 1L));
        assertThrows(ReglaNegocioException.class,
                () -> Movimiento.registrar(TipoMovimiento.GASTO, monto, null, null, transporte, 1L));
        assertThrows(ReglaNegocioException.class,
                () -> Movimiento.registrar(TipoMovimiento.GASTO, monto, HOY, null, null, 1L));
    }

    @Test
    @DisplayName("La descripcion es opcional: vacia se guarda como null")
    void descripcionOpcional() {
        Movimiento m = Movimiento.registrar(TipoMovimiento.INGRESO, new BigDecimal("100"),
                HOY, "   ", salario, 1L);

        assertNull(m.getDescripcion());
    }

    @Test
    @DisplayName("No se puede usar una categoria de gastos en un ingreso")
    void categoriaDeOtroTipo() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> Movimiento.registrar(TipoMovimiento.INGRESO, new BigDecimal("100"),
                        HOY, null, transporte, 1L));

        assertTrue(ex.getMessage().contains("Transporte"));
    }

    @Test
    @DisplayName("La fecha no puede ser futura")
    void fechaFutura() {
        assertThrows(ReglaNegocioException.class,
                () -> Movimiento.registrar(TipoMovimiento.GASTO, new BigDecimal("100"),
                        HOY.plusDays(1), null, transporte, 1L));
    }

    @Test
    @DisplayName("Un gasto en Deudas no se puede registrar sin indicar la deuda")
    void deudasSinDeuda() {
        assertThrows(ReglaNegocioException.class,
                () -> Movimiento.registrar(TipoMovimiento.GASTO, new BigDecimal("100"),
                        HOY, null, deudas, 1L));
    }

    @Test
    @DisplayName("Un abono genera un gasto en Deudas enlazado a la deuda y al abono")
    void desdeAbono() {
        Movimiento m = Movimiento.desdeAbono(1L, 7L, 99L, "Banco Demo",
                new BigDecimal("50000"), HOY, deudas);

        assertEquals(TipoMovimiento.GASTO, m.getTipo());
        assertEquals(7L, m.getDeudaId());
        assertEquals(99L, m.getAbonoId());
        assertEquals("Abono a deuda: Banco Demo", m.getDescripcion());
        assertTrue(m.esAbonoDeDeuda());
    }

    @Test
    @DisplayName("Editar cambia todos los datos de un movimiento normal")
    void actualizar() {
        Movimiento m = gasto("2500");
        Categoria mascotas = new Categoria(20L, "Mascotas", TipoMovimiento.GASTO, false, 1L);

        m.actualizar(TipoMovimiento.GASTO, new BigDecimal("80000"), HOY.minusDays(3),
                "Concentrado", mascotas);

        assertEquals(new BigDecimal("80000.00"), m.getMonto());
        assertEquals(HOY.minusDays(3), m.getFecha());
        assertEquals("Mascotas", m.getCategoria().getNombre());
    }

    @Test
    @DisplayName("Un gasto normal no se puede mover a la categoria Deudas")
    void actualizarADeudas() {
        Movimiento m = gasto("2500");

        assertThrows(ReglaNegocioException.class,
                () -> m.actualizar(TipoMovimiento.GASTO, new BigDecimal("2500"), HOY, null, deudas));
    }

    @Test
    @DisplayName("En un abono solo se puede cambiar la descripcion")
    void actualizarAbonoSoloDescripcion() {
        Movimiento m = abono();

        m.actualizar(TipoMovimiento.GASTO, new BigDecimal("50000"), HOY, "Cuota de octubre", deudas);
        assertEquals("Cuota de octubre", m.getDescripcion());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> m.actualizar(TipoMovimiento.GASTO, new BigDecimal("60000"), HOY, null, deudas));
        assertTrue(ex.getMessage().contains("descripcion"));
    }

    @Test
    @DisplayName("Desvincular la deuda conserva el gasto pero sin referencias")
    void desvincular() {
        Movimiento m = abono();

        m.desvincularDeuda();

        assertFalse(m.esAbonoDeDeuda());
        assertNull(m.getDeudaId());
        assertEquals(new BigDecimal("50000.00"), m.getMonto());
    }
}
