package edu.ucentral.zenfi.movimientos.dominio.puerto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Puerto de salida hacia el modulo de deudas.
 *
 * Movimientos declara QUE necesita (abonar y revertir un abono) sin
 * saber COMO lo hace deudas. El adaptador PagoDeudaAdaptador lo
 * implementa llamando a DeudaServicio. En los tests se simula con
 * @InjectMock, sin tocar el modulo de deudas.
 */
public interface PagoDeudaPuerto {

    /**
     * Registra un abono en la deuda del usuario y devuelve el id del abono.
     * Lanza 404 si la deuda no existe o no es del usuario, y 409 si el
     * abono excede el saldo o la deuda ya esta pagada.
     */
    Long registrarAbono(Long usuarioId, Long deudaId, BigDecimal monto, LocalDate fecha);

    /** Elimina el abono: el saldo pendiente de la deuda vuelve a subir. */
    void revertirAbono(Long deudaId, Long abonoId);
}
