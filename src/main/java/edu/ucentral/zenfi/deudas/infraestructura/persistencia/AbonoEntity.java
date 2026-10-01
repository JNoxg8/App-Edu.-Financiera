package edu.ucentral.zenfi.deudas.infraestructura.persistencia;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "abono_deuda")
public class AbonoEntity extends PanacheEntity {

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal monto;

    @Column(nullable = false)
    public LocalDate fecha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deuda_id", nullable = false)
    public DeudaEntity deuda;
}
