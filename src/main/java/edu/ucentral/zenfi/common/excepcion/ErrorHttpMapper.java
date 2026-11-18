package edu.ucentral.zenfi.common.excepcion;

import edu.ucentral.zenfi.common.infraestructura.ResponseApi;
import edu.ucentral.zenfi.common.infraestructura.ResponseApiError;
import io.quarkus.logging.Log;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/**
 * Errores HTTP que lanza el propio framework: metodo no permitido (405),
 * tipo de contenido no soportado (415), JSON mal escrito (400), etc.
 * Conserva el codigo HTTP original, pero responde con el formato
 * ResponseApi para que el cliente Flutter lea siempre la misma estructura.
 */
@Provider
public class ErrorHttpMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException ex) {
        return responder(ex.getResponse().getStatus(), uriInfo.getPath());
    }

    static Response responder(int status, String path) {
        if (status >= 500) {
            // El detalle tecnico de un 5xx no se le muestra al cliente
            return ErrorInesperadoMapper.respuesta500(null, path);
        }
        String mensaje = switch (status) {
            case 400 -> "La peticion tiene un formato invalido";
            case 404 -> "El recurso solicitado no existe";
            case 405 -> "Metodo HTTP no permitido para esta ruta";
            case 406 -> "No se puede responder en el formato solicitado";
            case 415 -> "Tipo de contenido no soportado: envie JSON";
            default -> "No se pudo procesar la peticion";
        };
        Log.warnf("Error HTTP %d en %s", status, path);

        ResponseApiError error = ResponseApiError.builder()
                .mensaje(mensaje)
                .codigo("HTTP-" + status)
                .detalles(List.of())
                .build();
        return Response.status(status)
                .entity(ResponseApi.deError(status, "Peticion invalida", mensaje, path, error))
                .build();
    }
}
