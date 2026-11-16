package edu.ucentral.zenfi.autenticacion.dominio.modelo;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Cuenta de acceso de un usuario: su correo y el HASH de su contrasena
 * (nunca la contrasena en texto plano). Java puro: sin JPA ni Quarkus.
 *
 * El id de la cuenta es el "usuarioId" que guardan las deudas, los
 * movimientos, las metas, etc.
 */
public class Cuenta {

    public static final int LONGITUD_MINIMA_CONTRASENA = 8;

    /** BCrypt solo usa los primeros 72 bytes: mas largo no aporta seguridad. */
    public static final int LONGITUD_MAXIMA_CONTRASENA = 72;

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Long id;
    private final String correo;
    private final String hashContrasena;
    private final LocalDateTime fechaCreacion;

    public Cuenta(Long id, String correo, String hashContrasena, LocalDateTime fechaCreacion) {
        this.id = id;
        this.correo = correo;
        this.hashContrasena = hashContrasena;
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * El correo se guarda en minusculas y sin espacios: "Camila@Mail.com " y
     * "camila@mail.com" son la misma cuenta.
     */
    public static String normalizarCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ReglaNegocioException("El correo es obligatorio");
        }
        String limpio = correo.trim().toLowerCase(Locale.ROOT);
        if (!CORREO.matcher(limpio).matches()) {
            throw new ReglaNegocioException("El correo no tiene un formato valido");
        }
        return limpio;
    }

    public static void validarContrasena(String contrasena) {
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA_CONTRASENA) {
            throw new ReglaNegocioException(
                    "La contrasena debe tener al menos " + LONGITUD_MINIMA_CONTRASENA + " caracteres");
        }
        if (contrasena.length() > LONGITUD_MAXIMA_CONTRASENA) {
            throw new ReglaNegocioException(
                    "La contrasena no puede superar " + LONGITUD_MAXIMA_CONTRASENA + " caracteres");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
