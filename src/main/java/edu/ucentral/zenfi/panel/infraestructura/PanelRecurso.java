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
import edu.ucentral.zenfi.common.seguridad.UsuarioActual;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

/** RQ-15: adaptador de entrada del panel principal. Sin logica de negocio. */
@Path("/panel")
@Tag(name = "Panel principal", description = "RQ-15: resumen del estado financiero")
@Produces(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "token")
public class PanelRecurso {

    /** Usuario de la peticion: lo identifica FiltroAutenticacion con el token. */
    @Inject
    UsuarioActual usuarioActual;

    @Inject
    PanelServicio servicio;

    @GET
    @Operation(
            summary = "Panel principal",
            description = "Balance general (ingresos - gastos de siempre), gastos del mes actual, "
                    + "avance de las metas activas y deudas pendientes con su saldo")
    @APIResponse(responseCode = "200", description = "Panel del usuario")
    public PanelRespuestaDto panel() {
        return PanelRespuestaDto.desde(servicio.generar(usuarioActual.id(), LocalDate.now()));
    }
}
