package edu.ucentral.zenfi.deudas.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un pago parcial hecho a una deuda.
 * Es parte interna de la Deuda: no tiene sentido por si solo.
 */
public class Abono {

    private Long id;
    private BigDecimal monto;
    private LocalDate fecha;

    public Abono() {
    }

    public Abono(Long id, BigDecimal monto, LocalDate fecha) {
        this.id = id;
        this.monto = monto;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}
