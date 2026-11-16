
package edu.ucentral.zenfi.metasahorro.infraestructura.recurso;

import edu.ucentral.zenfi.metasahorro.aplicacion.MetaAhorroServicio;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.CrearMetaAhorroRequest;
import edu.ucentral.zenfi.metasahorro.infraestructura.dto.MetaAhorroResponse;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import edu.ucentral.zenfi.common.seguridad.UsuarioActual;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

@Path("/metas-ahorro")
@Tag(name = "Metas de ahorro", description = "Gestion de metas de ahorro")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "token")
public class MetaAhorroRecurso {

    /** Usuario de la peticion: lo identifica FiltroAutenticacion con el token. */
    @Inject
    UsuarioActual usuarioActual;

    @Inject
    MetaAhorroServicio servicio;

    @POST
    @Operation(
            summary = "Crear meta de ahorro",
            description = "Crea una nueva meta de ahorro"
    )
    public Response crear(@Valid CrearMetaAhorroRequest request) {

        MetaAhorroResponse respuesta = servicio.crear(usuarioActual.id(), request);

        return Response.status(Response.Status.CREATED)
                .entity(respuesta)
                .build();
    }

    @GET
    @Operation(
            summary = "Listar metas de ahorro",
            description = "Consulta todas las metas de ahorro registradas"
    )
    public List<MetaAhorroResponse> listar() {
        return servicio.listar(usuarioActual.id());
    }

    @DELETE
    @Path("/{id}")
    @Operation(
            summary = "Eliminar meta de ahorro",
            description = "Elimina una meta y sus registros asociados"
    )
    public Response eliminar(@PathParam("id") Long id) {
        servicio.eliminar(usuarioActual.id(), id);
        return Response.noContent().build();
    }
}


