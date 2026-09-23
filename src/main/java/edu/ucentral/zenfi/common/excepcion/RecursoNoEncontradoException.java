package edu.ucentral.zenfi.common.excepcion;

/** Se lanza cuando se pide un recurso que no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
