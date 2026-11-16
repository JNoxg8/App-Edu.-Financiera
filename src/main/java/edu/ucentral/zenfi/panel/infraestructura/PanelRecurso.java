package edu.ucentral.zenfi.panel.infraestructura;

import edu.ucentral.zenfi.panel.aplicacion.PanelServicio;
import edu.ucentral.zenfi.panel.infraestructura.dto.PanelRespuestaDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;

/** RQ-15: adaptador de entrada del panel principal. Sin logica de negocio. */
@Path("/panel")
@Tag(name = "Panel principal", description = "RQ-15: resumen del estado financiero")
@Produces(MediaType.APPLICATION_JSON)
public class PanelRecurso {

    /** Sin login (RQ-01/RQ-02 fuera de alcance): mismo usuario fijo que deudas. */
    private static final Long USUARIO_ACTUAL = 1L;

    @Inject
    PanelServicio servicio;

    @GET
    @Operation(
            summary = "Panel principal",
            description = "Balance general (ingresos - gastos de siempre), gastos del mes actual, "
                    + "avance de las metas activas y deudas pendientes con su saldo")
    @APIResponse(responseCode = "200", description = "Panel del usuario")
    public PanelRespuestaDto panel() {
        return PanelRespuestaDto.desde(servicio.generar(USUARIO_ACTUAL, LocalDate.now()));
    }
}
