package edu.ucentral.zenfi.presupuesto.infraestructura.persistencia;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Tabla "presupuesto" (HU011).
 *
 * El mes se guarda en dos columnas (anio y mes) en lugar de un YearMonth:
 * son tipos simples que cualquier base de datos entiende y permiten la
 * restriccion UNIQUE que impide dos presupuestos para el mismo mes, aunque
 * lleguen dos peticiones al mismo tiempo.
 *
 * PanacheEntity ya aporta el campo "id".
 */
@Entity
@Table(name = "presupuesto", uniqueConstraints = @UniqueConstraint(
        name = "uk_presupuesto_usuario_mes",
        columnNames = {"usuario_id", "anio", "mes"}))
public class PresupuestoEntity extends PanacheEntity {

    @Column(name = "usuario_id", nullable = false)
    public Long usuarioId;

    @Column(nullable = false)
    public int anio;

    /** 1 = enero ... 12 = diciembre. */
    @Column(nullable = false)
    public int mes;

    // NUMERIC(12,2): nunca usar double para dinero
    @Column(name = "monto_limite", nullable = false, precision = 12, scale = 2)
    public BigDecimal montoLimite;
}
