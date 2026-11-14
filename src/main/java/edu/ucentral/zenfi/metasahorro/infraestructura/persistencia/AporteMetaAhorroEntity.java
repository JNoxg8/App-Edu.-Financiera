
package edu.ucentral.zenfi.metasahorro.infraestructura.persistencia;

import edu.ucentral.zenfi.metasahorro.dominio.modelo.MetaAhorro;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "aportes_meta_ahorro")
public class AporteMetaAhorroEntity extends PanacheEntity {

    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal monto;

    @Column(nullable = false)
    public LocalDate fecha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meta_ahorro_id", nullable = false)
    public MetaAhorro metaAhorro;
}
