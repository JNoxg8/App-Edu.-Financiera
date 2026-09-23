package edu.ucentral.zenfi.common.excepcion;

import edu.ucentral.zenfi.common.infraestructura.ResponseApi;
import edu.ucentral.zenfi.common.infraestructura.ResponseApiError;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/** Traduce ReglaNegocioException a HTTP 409 con formato ResponseApi. */
@Provider
public class ReglaNegocioMapper implements ExceptionMapper<ReglaNegocioException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ReglaNegocioException ex) {
        ResponseApiError error = ResponseApiError.builder()
                .mensaje(ex.getMessage())
                .codigo("NEG-409")
                .detalles(List.of())
                .build();

        return Response.status(Response.Status.CONFLICT)
                .entity(ResponseApi.deError(
                        Response.Status.CONFLICT.getStatusCode(),
                        "Regla de negocio",
                        ex.getMessage(),
                        uriInfo.getPath(),
                        error))
                .build();
    }
}
