package edu.ucentral.zenfi.autenticacion.aplicacion;

import edu.ucentral.zenfi.autenticacion.dominio.modelo.Cuenta;
import edu.ucentral.zenfi.autenticacion.dominio.puerto.CifradoContrasenas;
import edu.ucentral.zenfi.autenticacion.dominio.repositorio.CuentaRepositorio;
import edu.ucentral.zenfi.autenticacion.dominio.repositorio.SesionRepositorio;
import edu.ucentral.zenfi.common.excepcion.NoAutenticadoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Casos de uso de acceso: crear la cuenta, iniciar y cerrar sesion, y
 * saber a que usuario pertenece un token.
 *
 * El token es un valor aleatorio de 256 bits (SecureRandom): imposible de
 * adivinar. El cliente lo envia en cada peticion como
 * "Authorization: Bearer <token>" y en la base solo se guarda su SHA-256.
 */
@ApplicationScoped
public class AutenticacionServicio {

    /** Cuanto dura una sesion sin volver a iniciarla. */
    static final Duration DURACION_SESION = Duration.ofDays(30);

    /** Mismo mensaje si falla el correo o la contrasena: no revela que cuentas existen. */
    static final String CREDENCIALES_INVALIDAS = "Correo o contrasena incorrectos";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Inject
    CuentaRepositorio cuentas;

    @Inject
    SesionRepositorio sesiones;

    @Inject
    CifradoContrasenas cifrado;

    /** Token entregado al cliente junto con su cuenta. */
    public record SesionIniciada(String token, Long usuarioId, String correo) {
    }

    @Transactional
    public SesionIniciada registrar(String correo, String contrasena) {
        String limpio = Cuenta.normalizarCorreo(correo);
        Cuenta.validarContrasena(contrasena);
        if (cuentas.buscarPorCorreo(limpio).isPresent()) {
            throw new ReglaNegocioException("Ya existe una cuenta con el correo " + limpio);
        }
        Cuenta cuenta = cuentas.crear(
                new Cuenta(null, limpio, cifrado.cifrar(contrasena), LocalDateTime.now()));
        return abrirSesion(cuenta);
    }

    @Transactional
    public SesionIniciada iniciarSesion(String correo, String contrasena) {
        if (correo == null || correo.isBlank() || contrasena == null) {
            throw new NoAutenticadoException(CREDENCIALES_INVALIDAS);
        }
        String limpio = correo.trim().toLowerCase(java.util.Locale.ROOT);
        Cuenta cuenta = cuentas.buscarPorCorreo(limpio)
                .filter(c -> cifrado.coincide(contrasena, c.getHashContrasena()))
                .orElseThrow(() -> new NoAutenticadoException(CREDENCIALES_INVALIDAS));
        return abrirSesion(cuenta);
    }

    @Transactional
    public void cerrarSesion(String token) {
        if (token != null && !token.isBlank()) {
            sesiones.eliminar(hash(token));
        }
    }

    /** Usuario al que pertenece el token, si la sesion existe y no ha vencido. */
    public Optional<Long> usuarioDe(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return sesiones.cuentaDeSesionVigente(hash(token), LocalDateTime.now());
    }

    /**
     * Borra las sesiones vencidas: ya no sirven para entrar, pero sin esta
     * limpieza la tabla crece sin limite. Una sola sentencia DELETE.
     */
    @Transactional
    public long limpiarSesionesVencidas() {
        return sesiones.eliminarVencidas(LocalDateTime.now());
    }

    public Cuenta cuenta(Long usuarioId) {
        return cuentas.buscarPorId(usuarioId)
                .orElseThrow(() -> new NoAutenticadoException("La cuenta ya no existe"));
    }

    private SesionIniciada abrirSesion(Cuenta cuenta) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sesiones.crear(hash(token), cuenta.getId(), LocalDateTime.now().plus(DURACION_SESION));
        return new SesionIniciada(token, cuenta.getId(), cuenta.getCorreo());
    }

    /**
     * SHA-256 basta para el token (no para contrasenas): el token ya es
     * aleatorio y largo, asi que no se puede adivinar probando valores.
     */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
