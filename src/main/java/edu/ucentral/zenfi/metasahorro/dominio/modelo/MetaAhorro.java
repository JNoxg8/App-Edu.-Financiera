package edu.ucentral.zenfi.metasahorro.dominio.modelo;

import jakarta.persistence.*;
        import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "metas_ahorro")
public class MetaAhorro {

    public static final String ACTIVA = "ACTIVA";
    public static final String COMPLETADA = "COMPLETADA";
    public static final String VENCIDA = "VENCIDA";

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

    /**
     * Dueno de la meta (id de su cuenta). Admite null solo para que la
     * columna se pueda agregar a una tabla con datos; al arrancar, las metas
     * antiguas se asignan al usuario 1 (ver MigracionDatosSinUsuario).
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

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
        this.estado = ACTIVA;
    }

    /**
     * HU004: COMPLETADA si lo ahorrado alcanza el objetivo; VENCIDA si ya
     * paso la fecha limite sin alcanzarlo (el mismo dia limite todavia se
     * puede aportar); si no, ACTIVA.
     *
     * Se recalcula en cada consulta porque el paso del tiempo vence una meta
     * aunque nadie la modifique.
     */
    public void recalcularEstado(LocalDate hoy) {
        if (montoAcumulado.compareTo(montoObjetivo) >= 0) {
            estado = COMPLETADA;
        } else if (hoy.isAfter(fechaLimite)) {
            estado = VENCIDA;
        } else {
            estado = ACTIVA;
        }
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

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    /** true si la meta es del usuario indicado. */
    public boolean perteneceA(Long usuario) {
        return usuario != null && usuario.equals(usuarioId);
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
