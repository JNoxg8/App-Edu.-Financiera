
package edu.ucentral.zenfi.metasahorro.infraestructura.recurso;

import edu.ucentral.zenfi.metasahorro.aplicacion.AporteMetaAhorroServicio;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.AporteMetaAhorroResponse;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.CrearAporteMetaAhorroRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/metas-ahorro/{metaAhorroId}/aportes")
@Tag(name = "Aportes de metas de ahorro", description = "Registro e historial de aportes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AporteMetaAhorroRecurso {

    @Inject
    AporteMetaAhorroServicio servicio;

    @POST
    @Operation(
            summary = "Registrar aporte",
            description = "Registra un aporte a una meta de ahorro"
    )
    public Response registrar(
            @PathParam("metaAhorroId") Long metaAhorroId,
            @Valid CrearAporteMetaAhorroRequest request) {

        AporteMetaAhorroResponse respuesta =
                servicio.registrar(metaAhorroId, request);

        return Response.status(Response.Status.CREATED)
                .entity(respuesta)
                .build();
    }

    @GET
    @Operation(
            summary = "Consultar aportes",
            description = "Consulta el historial de aportes de una meta"
    )
    public List<AporteMetaAhorroResponse> listar(
            @PathParam("metaAhorroId") Long metaAhorroId) {

        return servicio.listar(metaAhorroId);
    }

    @DELETE
    @Path("/{aporteId}")
    @Operation(
            summary = "Eliminar aporte",
            description = "Elimina un aporte y actualiza el acumulado de la meta"
    )
    public Response eliminar(
            @PathParam("metaAhorroId") Long metaAhorroId,
            @PathParam("aporteId") Long aporteId) {

        servicio.eliminar(metaAhorroId, aporteId);

        return Response.noContent().build();
    }
}
