package edu.ucentral.zenfi.deudas;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.deudas.dominio.modelo.Abono;
import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test UNITARIO puro: no lleva @QuarkusTest.
 *
 * No levanta la aplicacion ni la base de datos, por eso corre en
 * milisegundos. Esto es posible porque la clase Deuda no depende
 * de JPA ni de Quarkus: ese es el beneficio concreto de la
 * arquitectura hexagonal.
 */
class DeudaTest {

    private Deuda deudaSinInteres(String monto, int cuotas) {
        return new Deuda(1L, "Banco Demo", new BigDecimal(monto),
                BigDecimal.ZERO, cuotas, LocalDate.now().plusYears(1), 1L, null);
    }

    private Deuda deudaConInteres(String monto, String tasa, int cuotas) {
        return new Deuda(1L, "Banco Demo", new BigDecimal(monto),
                new BigDecimal(tasa), cuotas, LocalDate.now().plusYears(1), 1L, null);
    }

    @Test
    @DisplayName("Sin intereses la cuota es el capital dividido entre las cuotas")
    void cuotaSinInteres() {
        Deuda deuda = deudaSinInteres("1200000", 12);

        assertEquals(new BigDecimal("100000.00"), deuda.cuotaMensual());
        assertEquals(new BigDecimal("1200000.00"), deuda.totalAPagar());
        assertEquals(new BigDecimal("0.00"), deuda.costoIntereses());
    }

    @Test
    @DisplayName("Con intereses el total a pagar supera el capital")
    void cuotaConInteres() {
        Deuda deuda = deudaConInteres("5000000", "24.00", 24);

        assertTrue(deuda.cuotaMensual().compareTo(new BigDecimal("208333.33")) > 0,
                "La cuota con interes debe superar la cuota sin interes");
        assertTrue(deuda.costoIntereses().signum() > 0,
                "El costo de intereses debe ser positivo");
        assertEquals(deuda.totalAPagar(),
                deuda.getMontoTotal().add(deuda.costoIntereses()));
    }

    @Test
    @DisplayName("El saldo pendiente descuenta los abonos")
    void saldoPendiente() {
        Deuda deuda = deudaSinInteres("1000000", 10);
        deuda.agregarAbono(new Abono(null, new BigDecimal("300000"), LocalDate.now()));
        deuda.agregarAbono(new Abono(null, new BigDecimal("200000"), LocalDate.now()));

        assertEquals(new BigDecimal("500000.00"), deuda.totalAbonado());
        assertEquals(new BigDecimal("500000.00"), deuda.saldoPendiente());
        assertEquals(new BigDecimal("50.00"), deuda.porcentajePagado());
        assertFalse(deuda.estaPagada());
    }

    @Test
    @DisplayName("La deuda queda pagada cuando el saldo llega a cero")
    void deudaPagada() {
        Deuda deuda = deudaSinInteres("500000", 5);
        deuda.agregarAbono(new Abono(null, new BigDecimal("500000"), LocalDate.now()));

        assertTrue(deuda.estaPagada());
        assertEquals(new BigDecimal("0.00"), deuda.saldoPendiente());
        assertEquals(new BigDecimal("100.00"), deuda.porcentajePagado());
    }

    @Test
    @DisplayName("Rechaza un abono mayor al saldo pendiente")
    void abonoExcedeSaldo() {
        Deuda deuda = deudaSinInteres("100000", 1);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> deuda.agregarAbono(
                        new Abono(null, new BigDecimal("999999"), LocalDate.now())));

        assertTrue(ex.getMessage().contains("excede"));
    }

    @Test
    @DisplayName("Rechaza abonos de monto cero o negativo")
    void abonoInvalido() {
        Deuda deuda = deudaSinInteres("100000", 1);

        assertThrows(ReglaNegocioException.class,
                () -> deuda.agregarAbono(new Abono(null, BigDecimal.ZERO, LocalDate.now())));
        assertThrows(ReglaNegocioException.class,
                () -> deuda.agregarAbono(
                        new Abono(null, new BigDecimal("-100"), LocalDate.now())));
    }

    @Test
    @DisplayName("Rechaza abonar a una deuda ya pagada")
    void abonoSobreDeudaPagada() {
        Deuda deuda = new Deuda(1L, "Banco", new BigDecimal("100000"),
                BigDecimal.ZERO, 1, LocalDate.now().plusMonths(1), 1L,
                List.of(new Abono(1L, new BigDecimal("100000"), LocalDate.now())));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> deuda.agregarAbono(
                        new Abono(null, new BigDecimal("1000"), LocalDate.now())));

        assertTrue(ex.getMessage().contains("pagada"));
    }

    @Test
    @DisplayName("Actualizar cambia los datos y recalcula la cuota")
    void actualizarDatos() {
        Deuda deuda = deudaSinInteres("1200000", 12);
        LocalDate nuevaFecha = LocalDate.now().plusYears(3);

        deuda.actualizarDatos("Banco Nuevo", BigDecimal.ZERO, 24, nuevaFecha);

        assertEquals("Banco Nuevo", deuda.getAcreedor());
        assertEquals(24, deuda.getNumeroCuotas());
        assertEquals(nuevaFecha, deuda.getFechaVencimiento());
        // 1.200.000 / 24 = 50.000
        assertEquals(new BigDecimal("50000.00"), deuda.cuotaMensual());
    }

    @Test
    @DisplayName("Actualizar no modifica el monto total ni los abonos")
    void actualizarConservaCapitalYAbonos() {
        Deuda deuda = deudaSinInteres("1000000", 10);
        deuda.agregarAbono(new Abono(null, new BigDecimal("300000"), LocalDate.now()));

        deuda.actualizarDatos("Otro Banco", new BigDecimal("15.00"), 20,
                LocalDate.now().plusYears(2));

        assertEquals(new BigDecimal("1000000"), deuda.getMontoTotal());
        assertEquals(new BigDecimal("300000.00"), deuda.totalAbonado());
    }

    @Test
    @DisplayName("Sin intereses el redondeo de la cuota no pierde centavos")
    void totalSinInteresNoPierdeCentavos() {
        // 100.000 / 3 = 33.333,33 por cuota; 33.333,33 * 3 = 99.999,99.
        // El total a pagar debe seguir siendo el capital completo.
        Deuda deuda = deudaSinInteres("100000", 3);

        assertEquals(new BigDecimal("33333.33"), deuda.cuotaMensual());
        assertEquals(new BigDecimal("100000.00"), deuda.totalAPagar());
        assertEquals(new BigDecimal("0.00"), deuda.costoIntereses());
    }

    @Test
    @DisplayName("Actualizar una deuda ya pagada no la deja con saldo negativo")
    void actualizarDeudaPagada() {
        Deuda pagada = new Deuda(1L, "Banco", new BigDecimal("100000"),
                BigDecimal.ZERO, 1, LocalDate.now().plusYears(1), 1L,
                List.of(new Abono(1L, new BigDecimal("100000"), LocalDate.now())));

        // Pasar de 1 a 3 cuotas antes fallaba por el centavo perdido en
        // el redondeo (99.999,99 < 100.000 abonados).
        pagada.actualizarDatos("Banco", BigDecimal.ZERO, 3,
                LocalDate.now().plusYears(1));

        assertEquals(3, pagada.getNumeroCuotas());
        assertTrue(pagada.estaPagada());
        assertEquals(new BigDecimal("0.00"), pagada.saldoPendiente());
    }

}

