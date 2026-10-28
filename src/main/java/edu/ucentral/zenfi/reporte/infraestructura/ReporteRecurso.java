package edu.ucentral.zenfi.reporte.infraestructura;

import edu.ucentral.zenfi.reporte.aplicacion.ReporteServicio;
import edu.ucentral.zenfi.reporte.infraestructura.dto.ReporteMensualRespuestaDto;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.YearMonth;

/**
 * RQ-25: adaptador de entrada del reporte mensual. Sin logica de negocio.
 *
 * A diferencia de GET /presupuestos, aqui un mes sin presupuesto responde
 * 200: el reporte igual muestra los gastos por categoria.
 */
@Path("/reportes")
@Tag(name = "Reporte", description = "RQ-25: reporte mensual de gastos vs presupuesto")
@Produces(MediaType.APPLICATION_JSON)
public class ReporteRecurso {

    /** Sin login (RQ-01/RQ-02 fuera de alcance): mismo usuario fijo que deudas. */
    private static final Long USUARIO_ACTUAL = 1L;

    @Inject
    ReporteServicio servicio;

    @GET
    @Path("/mensual")
    @Operation(
            summary = "Reporte mensual de gastos vs presupuesto",
            description = "Limite, total gastado, diferencia y porcentaje utilizado, mas el "
                    + "desglose de gastos por categoria. Si el mes no tiene presupuesto, "
                    + "'presupuesto' llega en null")
    @APIResponse(responseCode = "200", description = "Reporte del mes")
    @APIResponse(responseCode = "400", description = "El mes tiene un formato invalido")
    public ReporteMensualRespuestaDto mensual(
            @Parameter(description = "Mes en formato AAAA-MM. Si se omite, el mes actual")
            @QueryParam("mes")
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "El mes debe tener el formato AAAA-MM")
            String mes) {
        YearMonth consultado = (mes == null) ? YearMonth.now() : YearMonth.parse(mes);
        return ReporteMensualRespuestaDto.desde(servicio.generar(USUARIO_ACTUAL, consultado));
    }
}
