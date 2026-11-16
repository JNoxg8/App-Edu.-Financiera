package edu.ucentral.zenfi.autenticacion.infraestructura.persistencia;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas")
public class CuentaEntity extends PanacheEntity {

    /** Unico: no puede haber dos cuentas con el mismo correo. */
    @Column(nullable = false, unique = true, length = 254)
    public String correo;

    @Column(name = "hash_contrasena", nullable = false, length = 100)
    public String hashContrasena;

    @Column(name = "fecha_creacion", nullable = false)
    public LocalDateTime fechaCreacion;
}
