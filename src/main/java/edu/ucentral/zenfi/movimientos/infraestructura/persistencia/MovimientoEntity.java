package edu.ucentral.zenfi.movimientos.infraestructura.persistencia;

import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tabla "movimiento": ingresos y gastos del usuario (HU009).
 *
 * La categoria es una llave foranea real (@ManyToOne) porque vive en el
 * mismo modulo y se necesita para ordenar y buscar por su nombre.
 *
 * deuda_id y abono_id, en cambio, son columnas simples: la deuda es otro
 * modulo, y asi borrar una deuda no choca con esta tabla (igual que
 * DeudaEntity guarda usuario_id sin @ManyToOne).
 */
@Entity
@Table(name = "movimiento", indexes = {
        @Index(name = "ix_movimiento_usuario_fecha", columnList = "usuario_id, fecha"),
        @Index(name = "ix_movimiento_deuda", columnList = "deuda_id")
})
public class MovimientoEntity extends PanacheEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    public TipoMovimiento tipo;

    // NUMERIC(12,2): nunca usar double para dinero
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal monto;

    @Column(nullable = false)
    public LocalDate fecha;

    @Column(length = 255)
    public String descripcion;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false)
    public CategoriaEntity categoria;

    @Column(name = "usuario_id", nullable = false)
    public Long usuarioId;

    @Column(name = "deuda_id")
    public Long deudaId;

    /** Unico: un abono genera exactamente un gasto. */
    @Column(name = "abono_id", unique = true)
    public Long abonoId;
}
