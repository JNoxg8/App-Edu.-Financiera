package edu.ucentral.zenfi.genericos;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Endpoint generico de la plantilla del profesor (GenericResource).
 * Sirve para verificar rapidamente que la API esta arriba.
 */
@Path("/genericos")
@Tag(name = "Genericos", description = "Informacion general del aplicativo")
public class GenericoRecurso {

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
            summary = "Consultar version del aplicativo",
            description = "Devuelve la version del aplicativo y sus autores")
    @APIResponse(responseCode = "200", description = "Informacion de la version")
    public String version() {
        return "Zenfi API version " + version
                + " - Equipo Zenfi, Universidad Central";
    }
}
