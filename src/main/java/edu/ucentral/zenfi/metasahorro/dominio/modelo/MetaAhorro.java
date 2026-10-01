package edu.ucentral.zenfi.metasahorro.dominio.modelo;

import jakarta.persistence.*;
        import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "metas_ahorro")
public class MetaAhorro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montoObjetivo;

    @Column(nullable = false)
    private LocalDate fechaLimite;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montoInicial;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montoAcumulado;

    @Column(nullable = false)
    private String estado;

    public MetaAhorro() {
    }

    public MetaAhorro(
            String nombre,
            BigDecimal montoObjetivo,
            LocalDate fechaLimite,
            BigDecimal montoInicial) {

        this.nombre = nombre;
        this.montoObjetivo = montoObjetivo;
        this.fechaLimite = fechaLimite;
        this.montoInicial = montoInicial;
        this.montoAcumulado = montoInicial;
        this.estado = "ACTIVA";
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getMontoObjetivo() {
        return montoObjetivo;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public BigDecimal getMontoAcumulado() {
        return montoAcumulado;
    }

    public String getEstado() {
        return estado;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setMontoObjetivo(BigDecimal montoObjetivo) {
        this.montoObjetivo = montoObjetivo;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
    }

    public void setMontoAcumulado(BigDecimal montoAcumulado) {
        this.montoAcumulado = montoAcumulado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
