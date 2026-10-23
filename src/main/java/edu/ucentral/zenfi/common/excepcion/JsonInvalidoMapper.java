package edu.ucentral.zenfi.common.excepcion;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import edu.ucentral.zenfi.common.infraestructura.ResponseApi;
import edu.ucentral.zenfi.common.infraestructura.ResponseApiError;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Errores al convertir el JSON de entrada: un valor de enum inexistente
 * ("moneda": "XYZ"), texto donde se esperaba un numero, etc.
 *
 * Estos errores ocurren ANTES de Bean Validation, al leer el JSON. Sin
 * este mapper, Quarkus responderia con su formato por defecto y no con
 * ResponseApi.
 */
@Provider
public class JsonInvalidoMapper implements ExceptionMapper<MismatchedInputException> {

    private static final String MENSAJE = "El cuerpo de la peticion tiene un formato invalido";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(MismatchedInputException ex) {
        String campo = ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
        if (campo.isEmpty()) {
            campo = "cuerpo";
        }

        String detalle;
        if (ex instanceof InvalidFormatException invalido
                && invalido.getTargetType() != null
                && invalido.getTargetType().isEnum()) {
            String permitidos = Arrays.stream(invalido.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            detalle = campo + ": el valor '" + invalido.getValue()
                    + "' no es valido. Valores permitidos: " + permitidos;
        } else {
            detalle = campo + ": formato invalido";
        }

        Log.warnf("JSON invalido en %s: %s", uriInfo.getPath(), detalle);

        ResponseApiError error = ResponseApiError.builder()
                .mensaje(MENSAJE)
                .codigo("JSON-400")
                .detalles(List.of(detalle))
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