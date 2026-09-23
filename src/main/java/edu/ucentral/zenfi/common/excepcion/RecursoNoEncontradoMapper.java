package edu.ucentral.zenfi.common.excepcion;

import edu.ucentral.zenfi.common.infraestructura.ResponseApi;
import edu.ucentral.zenfi.common.infraestructura.ResponseApiError;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/** Traduce RecursoNoEncontradoException a HTTP 404 con formato ResponseApi. */
@Provider
public class RecursoNoEncontradoMapper implements ExceptionMapper<RecursoNoEncontradoException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(RecursoNoEncontradoException ex) {
        ResponseApiError error = ResponseApiError.builder()
                .mensaje(ex.getMessage())
                .codigo("REC-404")
                .detalles(List.of())
                .build();

        return Response.status(Response.Status.NOT_FOUND)
                .entity(ResponseApi.deError(
                        Response.Status.NOT_FOUND.getStatusCode(),
                        "Recurso no encontrado",
                        ex.getMessage(),
                        uriInfo.getPath(),
                        error))
                .build();
    }
}
