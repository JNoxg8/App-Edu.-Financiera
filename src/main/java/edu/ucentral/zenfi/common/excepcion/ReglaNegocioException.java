package edu.ucentral.zenfi.common.excepcion;

/**
 * Se lanza cuando los datos son validos pero violan una regla del negocio.
 * Ejemplo: abonar mas de lo que se debe. Se traduce a HTTP 409.
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
