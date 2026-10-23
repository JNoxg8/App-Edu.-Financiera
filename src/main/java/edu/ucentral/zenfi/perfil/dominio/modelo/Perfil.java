package edu.ucentral.zenfi.perfil.dominio.modelo;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;

import java.time.LocalDateTime;

/**
 * Perfil del usuario (HU001). Java puro: sin JPA ni Quarkus.
 *
 * Mientras no exista login hay un unico perfil, con el mismo id fijo
 * que usan las deudas (usuario 1).
 */
public class Perfil {

    private static final int LONGITUD_MAXIMA_NOMBRE = 80;

    private final Long id;
    private final String nombre;
    private final Moneda moneda;
    private final NivelConocimiento nivel;
    private final LocalDateTime fechaCreacion;

    /** Reconstruye un perfil ya guardado (lo usa el adaptador de persistencia). */
    public Perfil(Long id, String nombre, Moneda moneda,
                  NivelConocimiento nivel, LocalDateTime fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.moneda = moneda;
        this.nivel = nivel;
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Crea un perfil nuevo validando los criterios de aceptacion de HU001:
     * nombre no vacio, moneda y nivel obligatorios.
     *
     * Los DTO ya validan esto con Bean Validation; aqui se repite para que
     * la regla se cumpla aunque el perfil se cree desde otro punto.
     */
    public static Perfil crear(Long id, String nombre, Moneda moneda,
                               NivelConocimiento nivel) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaNegocioException("El nombre del perfil es obligatorio");
        }
        String nombreLimpio = nombre.trim();
        if (nombreLimpio.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new ReglaNegocioException(
                    "El nombre no puede superar " + LONGITUD_MAXIMA_NOMBRE + " caracteres");
        }
        if (moneda == null) {
            throw new ReglaNegocioException("Debe seleccionar una moneda de preferencia");
        }
        if (nivel == null) {
            throw new ReglaNegocioException("Debe seleccionar un nivel de conocimiento");
        }
        return new Perfil(id, nombreLimpio, moneda, nivel, LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Moneda getMoneda() {
        return moneda;
    }

    public NivelConocimiento getNivel() {
        return nivel;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}