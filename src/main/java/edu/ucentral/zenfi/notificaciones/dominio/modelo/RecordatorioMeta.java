
package edu.ucentral.zenfi.notificaciones.dominio.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recordatorios_metas")
public class RecordatorioMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long metaAhorroId;

    @Column(nullable = false)
    private String frecuencia;

    @Column(nullable = false)
    private LocalDate proximoRecordatorio;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    public RecordatorioMeta() {
    }

    public RecordatorioMeta(Long metaAhorroId) {
        this.metaAhorroId = metaAhorroId;
        this.frecuencia = "SEMANAL";
        this.proximoRecordatorio = LocalDate.now().plusWeeks(1);
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getMetaAhorroId() {
        return metaAhorroId;
    }

    public String getFrecuencia() {
        return frecuencia;
    }

    public LocalDate getProximoRecordatorio() {
        return proximoRecordatorio;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void configurar(String frecuencia, LocalDate proximoRecordatorio) {
        this.frecuencia = frecuencia;
        this.proximoRecordatorio = proximoRecordatorio;
    }

    public void actualizarProximoRecordatorio(LocalDate nuevaFecha) {
        this.proximoRecordatorio = nuevaFecha;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}

