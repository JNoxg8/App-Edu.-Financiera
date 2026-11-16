package edu.ucentral.zenfi.common.excepcion;

import edu.ucentral.zenfi.common.infraestructura.ResponseApi;
import edu.ucentral.zenfi.common.infraestructura.ResponseApiError;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/** Traduce NoAutenticadoException a HTTP 401 con formato ResponseApi. */
@Provider
public class NoAutenticadoMapper implements ExceptionMapper<NoAutenticadoException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(NoAutenticadoException ex) {
        return respuesta(ex.getMessage(), uriInfo.getPath());
    }

    /** Tambien lo usa el filtro de autenticacion, que no pasa por los mappers. */
    public static Response respuesta(String mensaje, String path) {
        ResponseApiError error = ResponseApiError.builder()
                .mensaje(mensaje)
                .codigo("AUT-401")
                .detalles(List.of())
                .build();
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(ResponseApi.deError(
                        Response.Status.UNAUTHORIZED.getStatusCode(),
                        "No autenticado",
                        mensaje,
                        path,
                        error))
                .build();
    }
}
