package edu.ucentral.zenfi.perfil.infraestructura;

import edu.ucentral.zenfi.perfil.aplicacion.PerfilServicio;
import edu.ucentral.zenfi.perfil.dominio.modelo.Perfil;
import edu.ucentral.zenfi.perfil.infraestructura.dto.CrearPerfilDto;
import edu.ucentral.zenfi.perfil.infraestructura.dto.PerfilRespuestaDto;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Endpoints del perfil (HU001).
 *
 * La ruta no lleva {id}: siempre se trabaja con el usuario actual.
 * Cuando exista login, el id saldra del token y la ruta no cambia.
 */
@Path("/perfil")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Perfil", description = "HU001: crear perfil")
public class PerfilRecurso {

    /** Sin login (RQ-01/RQ-02 fuera de alcance): mismo usuario fijo que deudas. */
    private static final Long USUARIO_ACTUAL = 1L;

    @Inject
    PerfilServicio servicio;

    @GET
    @Operation(summary = "Consultar el perfil",
            description = "Devuelve 404 si el perfil aun no se ha creado; "
                    + "la app usa esa respuesta para mostrar el formulario de creacion")
    @APIResponse(responseCode = "200", description = "Perfil encontrado")
    @APIResponse(responseCode = "404", description = "El perfil aun no ha sido creado")
    public PerfilRespuestaDto obtener() {
        return PerfilRespuestaDto.desde(servicio.obtener(USUARIO_ACTUAL));
    }

    @POST
    @Operation(summary = "Crear el perfil",
            description = "Registra nombre, moneda de preferencia y nivel de conocimiento. "
                    + "Solo se puede crear una vez")
    @APIResponse(responseCode = "201", description = "Perfil creado")
    @APIResponse(responseCode = "400", description = "Datos incompletos o invalidos")
    @APIResponse(responseCode = "409", description = "El perfil ya fue creado")
    public Response crear(@Valid CrearPerfilDto dto) {
        Perfil perfil = servicio.crear(USUARIO_ACTUAL, dto.nombre(), dto.moneda(), dto.nivel());
        return Response.status(Response.Status.CREATED)
                .entity(PerfilRespuestaDto.desde(perfil))
                .build();
    }
}