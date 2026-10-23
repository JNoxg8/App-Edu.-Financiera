package edu.ucentral.zenfi.movimientos.infraestructura;

import edu.ucentral.zenfi.movimientos.aplicacion.CategoriaServicio;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import edu.ucentral.zenfi.movimientos.infraestructura.dto.ActualizarCategoriaDto;
import edu.ucentral.zenfi.movimientos.infraestructura.dto.CategoriaRespuestaDto;
import edu.ucentral.zenfi.movimientos.infraestructura.dto.CrearCategoriaDto;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Locale;

/**
 * HU009: categorias de ingresos y gastos.
 * Adaptador de entrada: sin logica de negocio, solo traduce HTTP.
 */
@Path("/categorias")
@Tag(name = "Categorias", description = "HU009: categorias predefinidas y personalizadas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoriaRecurso {

    /** Sin login (RQ-01/RQ-02 fuera de alcance): mismo usuario fijo que deudas. */
    private static final Long USUARIO_ACTUAL = 1L;

    @Inject
    CategoriaServicio servicio;

    @GET
    @Operation(summary = "Listar categorias",
            description = "Predefinidas primero y despues las personalizadas del usuario. "
                    + "Se puede filtrar por tipo")
    @APIResponse(responseCode = "200", description = "Listado de categorias")
    @APIResponse(responseCode = "400", description = "Tipo invalido")
    public List<CategoriaRespuestaDto> listar(
            @Parameter(description = "INGRESO o GASTO. Vacio trae ambas")
            @QueryParam("tipo")
            @Pattern(regexp = "(?i)INGRESO|GASTO", message = "El tipo debe ser INGRESO o GASTO")
            String tipo) {

        TipoMovimiento tipoFiltro = tipo == null ? null : TipoMovimiento.valueOf(tipo.toUpperCase(Locale.ROOT));
        return servicio.listar(USUARIO_ACTUAL, tipoFiltro).stream()
                .map(CategoriaRespuestaDto::desde)
                .toList();
    }

    @POST
    @Operation(summary = "Crear una categoria personalizada",
            description = "El nombre no puede estar vacio ni repetirse dentro del mismo tipo "
                    + "(sin importar mayusculas ni tildes)")
    @APIResponse(responseCode = "201", description = "Categoria creada")
    @APIResponse(responseCode = "400", description = "Datos incompletos o invalidos")
    @APIResponse(responseCode = "409", description = "Ya existe una categoria con ese nombre")
    public Response crear(@Valid CrearCategoriaDto dto) {
        Categoria creada = servicio.crear(USUARIO_ACTUAL, dto.nombre(), dto.tipo());
        return Response.status(Response.Status.CREATED)
                .entity(CategoriaRespuestaDto.desde(creada))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Renombrar una categoria personalizada")
    @APIResponse(responseCode = "200", description = "Categoria actualizada")
    @APIResponse(responseCode = "400", description = "Nombre vacio o demasiado largo")
    @APIResponse(responseCode = "404", description = "La categoria no existe")
    @APIResponse(responseCode = "409", description = "Es predefinida o el nombre ya existe")
    public CategoriaRespuestaDto renombrar(@PathParam("id") Long id,
                                           @Valid ActualizarCategoriaDto dto) {
        return CategoriaRespuestaDto.desde(servicio.renombrar(USUARIO_ACTUAL, id, dto.nombre()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar una categoria personalizada",
            description = "Solo si no tiene movimientos asociados")
    @APIResponse(responseCode = "204", description = "Categoria eliminada")
    @APIResponse(responseCode = "404", description = "La categoria no existe")
    @APIResponse(responseCode = "409", description = "Es predefinida o tiene movimientos")
    public Response eliminar(@PathParam("id") Long id) {
        servicio.eliminar(USUARIO_ACTUAL, id);
        return Response.noContent().build();
    }
}
