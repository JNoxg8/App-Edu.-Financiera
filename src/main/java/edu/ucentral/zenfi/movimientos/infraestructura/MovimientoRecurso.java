package edu.ucentral.zenfi.movimientos.infraestructura;

import edu.ucentral.zenfi.movimientos.aplicacion.MovimientoServicio;
import edu.ucentral.zenfi.movimientos.dominio.modelo.FiltroMovimientos;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Movimiento;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import edu.ucentral.zenfi.movimientos.infraestructura.dto.ActualizarMovimientoDto;
import edu.ucentral.zenfi.movimientos.infraestructura.dto.CrearMovimientoDto;
import edu.ucentral.zenfi.movimientos.infraestructura.dto.MovimientoRespuestaDto;
import edu.ucentral.zenfi.movimientos.infraestructura.dto.ResumenRespuestaDto;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * HU009: adaptador de entrada de ingresos y gastos.
 *
 * Aqui no hay logica de negocio: solo convierte DTO -> dominio,
 * llama al servicio y convierte dominio -> DTO de respuesta.
 */
@Path("/movimientos")
@Tag(name = "Movimientos", description = "HU009: registro de ingresos y gastos por categoria")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MovimientoRecurso {

    /** Sin login (RQ-01/RQ-02 fuera de alcance): mismo usuario fijo que deudas. */
    private static final Long USUARIO_ACTUAL = 1L;

    @Inject
    MovimientoServicio servicio;

    @POST
    @Operation(summary = "Registrar un ingreso o gasto",
            description = "Si la categoria es Deudas se debe enviar deudaId: el gasto se "
                    + "registra como un abono y baja el saldo de esa deuda")
    @APIResponse(responseCode = "201", description = "Movimiento guardado correctamente")
    @APIResponse(responseCode = "400", description = "Falta un campo obligatorio o es invalido")
    @APIResponse(responseCode = "404", description = "La categoria o la deuda no existen")
    @APIResponse(responseCode = "409", description = "Categoria de otro tipo, o el abono excede el saldo")
    public Response crear(@Valid CrearMovimientoDto dto) {
        Movimiento creado = servicio.crear(USUARIO_ACTUAL, dto.tipo(), dto.monto(), dto.fecha(),
                dto.descripcion(), dto.categoriaId(), dto.deudaId());

        return Response.status(Response.Status.CREATED)
                .entity(MovimientoRespuestaDto.desde(creado))
                .build();
    }

    @GET
    @Operation(summary = "Listar, filtrar, buscar y ordenar movimientos",
            description = "Todos los filtros son opcionales y se pueden combinar")
    @APIResponse(responseCode = "200", description = "Listado de movimientos")
    @APIResponse(responseCode = "400", description = "Parametro invalido")
    @APIResponse(responseCode = "409", description = "La fecha inicial es posterior a la final")
    public List<MovimientoRespuestaDto> listar(
            @Parameter(description = "INGRESO o GASTO")
            @QueryParam("tipo")
            @Pattern(regexp = "(?i)INGRESO|GASTO", message = "El tipo debe ser INGRESO o GASTO")
            String tipo,

            @Parameter(description = "Id de la categoria")
            @QueryParam("categoriaId") Long categoriaId,

            @Parameter(description = "Texto a buscar en la descripcion o en el nombre de la categoria")
            @QueryParam("buscar")
            @Size(max = 100, message = "El texto de busqueda no puede superar 100 caracteres")
            String buscar,

            @Parameter(description = "Fecha inicial (yyyy-MM-dd), incluida")
            @QueryParam("desde") LocalDate desde,

            @Parameter(description = "Fecha final (yyyy-MM-dd), incluida")
            @QueryParam("hasta") LocalDate hasta,

            @Parameter(description = "FECHA, MONTO o CATEGORIA")
            @QueryParam("ordenarPor") @DefaultValue("FECHA")
            @Pattern(regexp = "(?i)FECHA|MONTO|CATEGORIA",
                    message = "Solo se puede ordenar por FECHA, MONTO o CATEGORIA")
            String ordenarPor,

            @Parameter(description = "ASC o DESC")
            @QueryParam("direccion") @DefaultValue("DESC")
            @Pattern(regexp = "(?i)ASC|DESC", message = "La direccion debe ser ASC o DESC")
            String direccion) {

        FiltroMovimientos filtro = new FiltroMovimientos(
                USUARIO_ACTUAL,
                tipo == null ? null : TipoMovimiento.valueOf(tipo.toUpperCase(Locale.ROOT)),
                categoriaId,
                buscar,
                desde,
                hasta,
                FiltroMovimientos.Orden.valueOf(ordenarPor.toUpperCase(Locale.ROOT)),
                FiltroMovimientos.Direccion.valueOf(direccion.toUpperCase(Locale.ROOT)));

        return servicio.listar(filtro).stream()
                .map(MovimientoRespuestaDto::desde)
                .toList();
    }

    /** Ruta fija: JAX-RS la prefiere sobre /{id}, asi que no hay conflicto. */
    @GET
    @Path("/resumen")
    @Operation(summary = "Resumen de ingresos y gastos",
            description = "Totales, balance y distribucion por categoria. Los abonos a "
                    + "deudas cuentan como gasto en la categoria Deudas")
    @APIResponse(responseCode = "200", description = "Resumen del periodo")
    @APIResponse(responseCode = "409", description = "La fecha inicial es posterior a la final")
    public ResumenRespuestaDto resumen(
            @Parameter(description = "Fecha inicial (yyyy-MM-dd). Vacio = desde el inicio")
            @QueryParam("desde") LocalDate desde,
            @Parameter(description = "Fecha final (yyyy-MM-dd). Vacio = hasta hoy")
            @QueryParam("hasta") LocalDate hasta) {
        return ResumenRespuestaDto.desde(servicio.resumen(USUARIO_ACTUAL, desde, hasta));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar un movimiento")
    @APIResponse(responseCode = "200", description = "Movimiento encontrado")
    @APIResponse(responseCode = "404", description = "El movimiento no existe")
    public MovimientoRespuestaDto obtener(@PathParam("id") Long id) {
        return MovimientoRespuestaDto.desde(servicio.obtener(USUARIO_ACTUAL, id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Editar un movimiento",
            description = "Si es un abono a una deuda solo se puede cambiar la descripcion")
    @APIResponse(responseCode = "200", description = "Movimiento actualizado")
    @APIResponse(responseCode = "400", description = "Datos invalidos")
    @APIResponse(responseCode = "404", description = "El movimiento o la categoria no existen")
    @APIResponse(responseCode = "409", description = "El cambio viola una regla de negocio")
    public MovimientoRespuestaDto actualizar(@PathParam("id") Long id,
                                             @Valid ActualizarMovimientoDto dto) {
        Movimiento actualizado = servicio.actualizar(USUARIO_ACTUAL, id, dto.tipo(), dto.monto(),
                dto.fecha(), dto.descripcion(), dto.categoriaId());
        return MovimientoRespuestaDto.desde(actualizado);
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Eliminar un movimiento",
            description = "Si es un abono a una deuda, el abono tambien se elimina y el "
                    + "saldo pendiente de la deuda vuelve a subir")
    @APIResponse(responseCode = "204", description = "Movimiento eliminado")
    @APIResponse(responseCode = "404", description = "El movimiento no existe")
    public Response eliminar(@PathParam("id") Long id) {
        servicio.eliminar(USUARIO_ACTUAL, id);
        return Response.noContent().build();
    }
}
