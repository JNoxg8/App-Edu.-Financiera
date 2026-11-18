package edu.ucentral.zenfi.common.excepcion;

import edu.ucentral.zenfi.common.infraestructura.ResponseApi;
import edu.ucentral.zenfi.common.infraestructura.ResponseApiError;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.UUID;

/**
 * Red de seguridad para cualquier excepcion que no tenga un mapper propio.
 *
 * JAX-RS elige el mapper del tipo mas cercano a la excepcion: los de
 * ReglaNegocioException, validacion, JSON, etc. siguen teniendo prioridad,
 * y este solo atiende lo que quede sin manejar.
 *
 * - Restriccion unica violada en la base (p. ej. dos registros simultaneos
 *   con el mismo correo): 409, porque es un conflicto con datos existentes.
 * - Todo lo demas: 500 con un mensaje generico y un codigo de referencia.
 *   El detalle (stack trace) va solo al log: mostrarlo al cliente revelaria
 *   tablas, clases y versiones internas (OWASP: exposicion de informacion).
 */
@Provider
public class ErrorInesperadoMapper implements ExceptionMapper<Throwable> {

    static final String MENSAJE_DUPLICADO = "Ya existe un registro con esos datos";

    /** Codigo SQL estandar de "valor duplicado en una restriccion unica". */
    private static final String SQLSTATE_UNICA = "23505";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {
        return responder(ex, uriInfo.getPath());
    }

    static Response responder(Throwable ex, String path) {
        if (esRestriccionUnica(ex)) {
            Log.warnf("Restriccion de la base violada en %s: %s", path, ex.getMessage());
            ResponseApiError error = ResponseApiError.builder()
                    .mensaje(MENSAJE_DUPLICADO)
                    .codigo("NEG-409")
                    .detalles(List.of())
                    .build();
            return Response.status(Response.Status.CONFLICT)
                    .entity(ResponseApi.deError(409, "Regla de negocio", MENSAJE_DUPLICADO, path, error))
                    .build();
        }
        return respuesta500(ex, path);
    }

    static Response respuesta500(Throwable ex, String path) {
        String referencia = UUID.randomUUID().toString().substring(0, 8);
        if (ex != null) {
            Log.errorf(ex, "Error inesperado [%s] en %s", referencia, path);
        } else {
            Log.errorf("Error interno [%s] en %s", referencia, path);
        }
        String mensaje = "Ocurrio un error inesperado. Referencia: " + referencia;
        ResponseApiError error = ResponseApiError.builder()
                .mensaje(mensaje)
                .codigo("ERR-500")
                .detalles(List.of())
                .build();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(ResponseApi.deError(500, "Error interno", mensaje, path, error))
                .build();
    }

    /**
     * Hibernate envuelve la violacion en varias capas (RollbackException,
     * PersistenceException...): se recorre la cadena de causas. El limite
     * evita un ciclo infinito si alguna causa se apunta a si misma.
     *
     * Solo cuenta la restriccion UNICA (SQLState 23505 en PostgreSQL y H2).
     * Una llave foranea o un NOT NULL violados son un error del programa,
     * no del usuario, y siguen saliendo como 500.
     */
    static boolean esRestriccionUnica(Throwable ex) {
        Throwable actual = ex;
        for (int i = 0; actual != null && i < 20; i++, actual = actual.getCause()) {
            if (actual instanceof ConstraintViolationException violacion
                    && (violacion.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE
                        || SQLSTATE_UNICA.equals(violacion.getSQLState()))) {
                return true;
            }
        }
        return false;
    }
}
