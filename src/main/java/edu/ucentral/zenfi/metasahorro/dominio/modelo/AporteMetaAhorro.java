
package edu.ucentral.zenfi.metasahorro.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AporteMetaAhorro {

    private Long id;
    private BigDecimal monto;
    private LocalDate fecha;
    private Long metaAhorroId;

    public AporteMetaAhorro() {
    }

    public AporteMetaAhorro(
            Long id,
            BigDecimal monto,
            LocalDate fecha,
            Long metaAhorroId) {
        this.id = id;
        this.monto = monto;
        this.fecha = fecha;
        this.metaAhorroId = metaAhorroId;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Long getMetaAhorroId() {
        return metaAhorroId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setMetaAhorroId(Long metaAhorroId) {
        this.metaAhorroId = metaAhorroId;
    }
}
