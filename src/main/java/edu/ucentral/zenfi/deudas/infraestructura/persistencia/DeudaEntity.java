package edu.ucentral.zenfi.deudas.infraestructura.persistencia;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA: el mapeo a la tabla.
 *
 * Vive en infraestructura, NO en dominio, porque @Entity y @Table
 * son detalles de la base de datos. El dominio no debe conocerlos.
 *
 * PanacheEntity ya aporta el campo "id".
 */
@Entity
@Table(name = "deuda")
public class DeudaEntity extends PanacheEntity {

    @Column(nullable = false, length = 120)
    public String acreedor;

    // NUMERIC(12,2): nunca usar double para dinero
    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    public BigDecimal montoTotal;

    @Column(name = "tasa_interes_anual", nullable = false, precision = 6, scale = 2)
    public BigDecimal tasaInteresAnual;

    @Column(name = "numero_cuotas", nullable = false)
    public int numeroCuotas;

    @Column(name = "fecha_vencimiento", nullable = false)
    public LocalDate fechaVencimiento;

    // Se referencia al usuario por id, no con @ManyToOne:
    // el usuario es otro agregado y asi los modulos quedan independientes.
    @Column(name = "usuario_id", nullable = false)
    public Long usuarioId;

    @OneToMany(
            mappedBy = "deuda",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    public List<AbonoEntity> abonos = new ArrayList<>();
}
