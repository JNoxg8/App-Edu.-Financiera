package edu.ucentral.zenfi.perfil.infraestructura.persistencia;

import edu.ucentral.zenfi.perfil.dominio.modelo.Moneda;
import edu.ucentral.zenfi.perfil.dominio.modelo.NivelConocimiento;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Tabla "perfil".
 *
 * Extiende PanacheEntityBase (y no PanacheEntity) porque el id NO se
 * autogenera: se asigna el id fijo del usuario actual (1), el mismo que
 * guardan las deudas en usuario_id. Con una secuencia no habria garantia
 * de que el perfil quedara con el id 1.
 */
@Entity
@Table(name = "perfil")
public class PerfilEntity extends PanacheEntityBase {

    @Id
    public Long id;

    @Column(nullable = false, length = 80)
    public String nombre;

    /** STRING guarda "COP" y no un numero: reordenar el enum no altera los datos. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    public Moneda moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    public NivelConocimiento nivel;

    @Column(name = "fecha_creacion", nullable = false)
    public LocalDateTime fechaCreacion;
}