package edu.ucentral.zenfi.common.excepcion;

/**
 * Se lanza cuando la peticion no trae un token valido, o cuando el correo
 * o la contrasena no coinciden. Se traduce a HTTP 401.
 */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
