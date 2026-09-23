package edu.ucentral.zenfi.common.excepcion;

import edu.ucentral.zenfi.common.infraestructura.ResponseApi;
import edu.ucentral.zenfi.common.infraestructura.ResponseApiError;
import io.quarkus.logging.Log;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * Traduce los errores de @Valid a una respuesta 400 con el detalle
 * campo por campo, en vez del mensaje tecnico por defecto.
 *
 * Equivale al ValidationExceptionMapper de la plantilla del profesor,
 * pero registra el detalle con el logger de Quarkus en vez de
 * System.out, para que respete la configuracion de logs.
 */
@Provider
public class ValidacionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final String MENSAJE = "Error de validacion en los datos de entrada";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException ex) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(violacion -> {
                    String campo = violacion.getPropertyPath().toString();
                    int punto = campo.lastIndexOf('.');
                    if (punto >= 0) {
                        campo = campo.substring(punto + 1);
                    }
                    return campo + ": " + violacion.getMessage();
                })
                .sorted()
                .toList();

        Log.warnf("Validacion fallida en %s: %s", uriInfo.getPath(), detalles);

        ResponseApiError error = ResponseApiError.builder()
                .mensaje(MENSAJE)
                .codigo("VAL-400")
                .detalles(detalles)
                .build();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ResponseApi.deError(
                        Response.Status.BAD_REQUEST.getStatusCode(),
                        "Peticion invalida",
                        MENSAJE,
                        uriInfo.getPath(),
                        error))
                .build();
    }
}
