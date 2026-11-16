
package edu.ucentral.zenfi.notificaciones.dominio.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column
    private Long metaAhorroId;

    /** Dueno de la notificacion (el mismo de la meta que la genero). */
    @Column(name = "usuario_id")
    private Long usuarioId;

    public Notificacion() {
    }

    public Notificacion(String titulo, String mensaje, Long metaAhorroId, Long usuarioId) {
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.metaAhorroId = metaAhorroId;
        this.usuarioId = usuarioId;
        this.leida = false;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Long getMetaAhorroId() {
        return metaAhorroId;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public boolean perteneceA(Long usuario) {
        return usuario != null && usuario.equals(usuarioId);
    }

    public void desvincularMeta() {
    this.metaAhorroId = null;
    }

    public void marcarComoLeida() {
        this.leida = true;
    }
}

