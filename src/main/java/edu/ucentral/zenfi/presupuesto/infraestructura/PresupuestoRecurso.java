package edu.ucentral.zenfi.presupuesto.infraestructura;

import edu.ucentral.zenfi.presupuesto.aplicacion.PresupuestoServicio;
import edu.ucentral.zenfi.presupuesto.infraestructura.dto.CrearPresupuestoDto;
import edu.ucentral.zenfi.presupuesto.infraestructura.dto.PresupuestoRespuestaDto;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.YearMonth;

/**
 * RQ-11: adaptador de entrada. Traduce HTTP a llamadas al servicio.
 *
 * Aqui no hay logica de negocio: solo convierte el texto "AAAA-MM" en
 * YearMonth, llama al servicio y convierte dominio -> DTO de respuesta.
 */
@Path("/presupuestos")
@Tag(name = "Presupuesto", description = "RQ-11: presupuesto mensual")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PresupuestoRecurso {

    /** Sin login (RQ-01/RQ-02 fuera de alcance): mismo usuario fijo que deudas. */
    private static final Long USUARIO_ACTUAL = 1L;

    @Inject
    PresupuestoServicio servicio;

    @POST
    @Operation(
            summary = "Definir el presupuesto de un mes",
            description = "Solo uno por mes, para el mes actual o uno futuro. Devuelve lo "
                    + "gastado en el mes y el porcentaje utilizado")
    @APIResponse(responseCode = "201", description = "Presupuesto creado")
    @APIResponse(responseCode = "400", description = "Datos de entrada invalidos")
    @APIResponse(responseCode = "409", description = "El mes ya tiene presupuesto o ya paso")
    public Response crear(@Valid CrearPresupuestoDto dto) {
        PresupuestoRespuestaDto creado = PresupuestoRespuestaDto.desde(
                servicio.crear(USUARIO_ACTUAL, YearMonth.parse(dto.mes()), dto.montoLimite()));

        return Response.status(Response.Status.CREATED)
                .entity(creado)
                .build();
    }

    @GET
    @Operation(
            summary = "Consultar el presupuesto de un mes",
            description = "Calcula lo gastado sumando los movimientos de tipo gasto del mes "
                    + "(incluidos los abonos a deudas) y el porcentaje utilizado")
    @APIResponse(responseCode = "200", description = "Presupuesto con lo gastado")
    @APIResponse(responseCode = "400", description = "El mes tiene un formato invalido")
    @APIResponse(responseCode = "404", description = "El mes no tiene presupuesto")
    public PresupuestoRespuestaDto consultar(
            @Parameter(description = "Mes en formato AAAA-MM. Si se omite, el mes actual")
            @QueryParam("mes")
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "El mes debe tener el formato AAAA-MM")
            String mes) {
        YearMonth consultado = (mes == null) ? YearMonth.now() : YearMonth.parse(mes);
        return PresupuestoRespuestaDto.desde(servicio.consultar(USUARIO_ACTUAL, consultado));
    }
}
