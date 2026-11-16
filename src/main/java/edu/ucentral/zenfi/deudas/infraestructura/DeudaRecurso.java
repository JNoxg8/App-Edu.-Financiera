package edu.ucentral.zenfi.deudas.infraestructura;

import jakarta.ws.rs.PUT;
import edu.ucentral.zenfi.deudas.infraestructura.dto.ActualizarDeudaDto;
import edu.ucentral.zenfi.deudas.aplicacion.DeudaServicio;
import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;
import edu.ucentral.zenfi.deudas.infraestructura.dto.CrearAbonoDto;
import edu.ucentral.zenfi.deudas.infraestructura.dto.CrearDeudaDto;
import edu.ucentral.zenfi.deudas.infraestructura.dto.DeudaRespuestaDto;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;


import java.util.List;
import edu.ucentral.zenfi.common.seguridad.UsuarioActual;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

/**
 * RQ-07: adaptador de entrada. Traduce HTTP a llamadas al servicio.
 *
 * Aqui no hay logica de negocio: solo convierte DTO -> dominio,
 * llama al servicio y convierte dominio -> DTO de respuesta.
 */
@Path("/deudas")
@Tag(name = "Deudas", description = "RQ-07: registro y seguimiento de deudas activas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "token")
public class DeudaRecurso {

    /** Usuario de la peticion: lo identifica FiltroAutenticacion con el token. */
    @Inject
    UsuarioActual usuarioActual;

    @Inject
    DeudaServicio servicio;

    @POST
    @Operation(
            summary = "Registrar una deuda activa",
            description = "Guarda la deuda y devuelve la cuota mensual, el total a pagar "
                    + "y el saldo pendiente calculados automaticamente")
    @APIResponse(responseCode = "201", description = "Deuda registrada")
    @APIResponse(responseCode = "400", description = "Datos de entrada invalidos")
    public Response crear(@Valid CrearDeudaDto dto) {
        Deuda deuda = new Deuda(
                null,
                dto.acreedor(),
                dto.montoTotal(),
                dto.tasaInteresAnual(),
                dto.numeroCuotas(),
                dto.fechaVencimiento(),
                usuarioActual.id(),
                null);

        Deuda creada = servicio.crear(deuda);

        return Response.status(Response.Status.CREATED)
                .entity(DeudaRespuestaDto.desde(creada))
                .build();
    }

    @GET
    @Operation(
            summary = "Listar las deudas del usuario",
            description = "Devuelve cada deuda con su saldo pendiente actualizado")
    @APIResponse(responseCode = "200", description = "Listado de deudas")
    public List<DeudaRespuestaDto> listar() {
        return servicio.listar(usuarioActual.id()).stream()
                .map(DeudaRespuestaDto::desde)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Consultar una deuda",
            description = "Incluye el detalle de los abonos registrados")
    @APIResponse(responseCode = "200", description = "Deuda encontrada")
    @APIResponse(responseCode = "404", description = "La deuda no existe")
    public DeudaRespuestaDto obtener(@PathParam("id") Long id) {
        return DeudaRespuestaDto.desde(servicio.obtener(usuarioActual.id(), id));
    }

    @POST
    @Path("/{id}/abonos")
    @Operation(
            summary = "Registrar un abono",
            description = "El saldo pendiente se recalcula automaticamente")
    @APIResponse(responseCode = "200", description = "Abono registrado")
    @APIResponse(responseCode = "400", description = "Datos de entrada invalidos")
    @APIResponse(responseCode = "404", description = "La deuda no existe")
    @APIResponse(responseCode = "409", description = "El abono excede el saldo o la deuda ya esta pagada")
    public DeudaRespuestaDto abonar(@PathParam("id") Long id, @Valid CrearAbonoDto dto) {
        Deuda actualizada = servicio.registrarAbono(usuarioActual.id(), id, dto.monto(), dto.fecha());
        return DeudaRespuestaDto.desde(actualizada);
    }

    @DELETE
    @Path("/{id}/abonos/{abonoId}")
    @Operation(
            summary = "Eliminar un abono",
            description = "El saldo pendiente vuelve a subir y el gasto asociado "
                    + "en movimientos (HU009) tambien se elimina")
    @APIResponse(responseCode = "200", description = "Abono eliminado")
    @APIResponse(responseCode = "404", description = "La deuda o el abono no existen")
    public DeudaRespuestaDto eliminarAbono(@PathParam("id") Long id,
                                           @PathParam("abonoId") Long abonoId) {
        return DeudaRespuestaDto.desde(servicio.eliminarAbono(usuarioActual.id(), id, abonoId));
    }

    @PUT
    @Path("/{id}")
    @Operation(
            summary = "Modificar una deuda registrada",
            description = "RQ-22: permite cambiar acreedor, tasa, numero de cuotas "
                    + "y fecha de vencimiento. El saldo pendiente se recalcula solo.")
    @APIResponse(responseCode = "200", description = "Deuda actualizada")
    @APIResponse(responseCode = "400", description = "Datos de entrada invalidos")
    @APIResponse(responseCode = "404", description = "La deuda no existe")
    @APIResponse(responseCode = "409", description = "El nuevo total quedaria por debajo de lo abonado")
    public DeudaRespuestaDto actualizar(@PathParam("id") Long id,
                                        @Valid ActualizarDeudaDto dto) {
        Deuda actualizada = servicio.actualizar(
                usuarioActual.id(),
                id,
                dto.acreedor(),
                dto.tasaInteresAnual(),
                dto.numeroCuotas(),
                dto.fechaVencimiento());

        return DeudaRespuestaDto.desde(actualizada);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar una deuda")
    @APIResponse(responseCode = "204", description = "Deuda eliminada")
    @APIResponse(responseCode = "404", description = "La deuda no existe")
    public Response eliminar(@PathParam("id") Long id) {
        servicio.eliminar(usuarioActual.id(), id);
        return Response.noContent().build();
    }
}
