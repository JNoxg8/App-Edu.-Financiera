package edu.ucentral.zenfi.autenticacion.infraestructura.persistencia;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones")
public class SesionEntity extends PanacheEntity {

    /** SHA-256 del token en hexadecimal (64 caracteres). */
    @Column(name = "hash_token", nullable = false, unique = true, length = 64)
    public String hashToken;

    @Column(name = "cuenta_id", nullable = false)
    public Long cuentaId;

    @Column(nullable = false)
    public LocalDateTime expira;
}
