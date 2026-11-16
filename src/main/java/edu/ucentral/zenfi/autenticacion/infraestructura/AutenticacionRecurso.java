package edu.ucentral.zenfi.autenticacion.infraestructura;

import edu.ucentral.zenfi.autenticacion.aplicacion.AutenticacionServicio;
import edu.ucentral.zenfi.autenticacion.aplicacion.AutenticacionServicio.SesionIniciada;
import edu.ucentral.zenfi.autenticacion.dominio.modelo.Cuenta;
import edu.ucentral.zenfi.autenticacion.infraestructura.dto.CredencialesDto;
import edu.ucentral.zenfi.common.seguridad.UsuarioActual;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Cuentas y sesiones. Registro e inicio de sesion devuelven el token que
 * la app debe enviar en las demas peticiones.
 */
@Path("/auth")
@Tag(name = "Autenticacion", description = "Crear cuenta, iniciar y cerrar sesion")
@SecurityScheme(securitySchemeName = "token", type = SecuritySchemeType.HTTP, scheme = "bearer",
        description = "Token de /auth/login o /auth/registro")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AutenticacionRecurso {

    @Inject
    AutenticacionServicio servicio;

    @Inject
    UsuarioActual usuarioActual;

    public record SesionRespuestaDto(String token, Long usuarioId, String correo) {
        static SesionRespuestaDto desde(SesionIniciada s) {
            return new SesionRespuestaDto(s.token(), s.usuarioId(), s.correo());
        }
    }

    public record CuentaRespuestaDto(Long usuarioId, String correo) {
    }

    @POST
    @Path("/registro")
    @Operation(summary = "Crear una cuenta",
            description = "Correo unico y contrasena de 8 a 72 caracteres. Devuelve el token de la sesion")
    @APIResponse(responseCode = "201", description = "Cuenta creada y sesion iniciada")
    @APIResponse(responseCode = "409", description = "El correo ya tiene cuenta o los datos no son validos")
    public Response registrar(@Valid CredencialesDto dto) {
        SesionIniciada sesion = servicio.registrar(dto.correo(), dto.contrasena());
        return Response.status(Response.Status.CREATED)
                .entity(SesionRespuestaDto.desde(sesion))
                .build();
    }

    @POST
    @Path("/login")
    @Operation(summary = "Iniciar sesion", description = "Devuelve un token valido por 30 dias")
    @APIResponse(responseCode = "200", description = "Sesion iniciada")
    @APIResponse(responseCode = "401", description = "Correo o contrasena incorrectos")
    public SesionRespuestaDto iniciarSesion(@Valid CredencialesDto dto) {
        return SesionRespuestaDto.desde(servicio.iniciarSesion(dto.correo(), dto.contrasena()));
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.WILDCARD)   // no lleva cuerpo
    @Operation(summary = "Cerrar sesion", description = "El token deja de servir")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "token")
    @APIResponse(responseCode = "204", description = "Sesion cerrada")
    public Response cerrarSesion(@HeaderParam(HttpHeaders.AUTHORIZATION) String autorizacion) {
        servicio.cerrarSesion(autorizacion.substring("Bearer ".length()).trim());
        return Response.noContent().build();
    }

    @GET
    @Path("/yo")
    @Operation(summary = "Cuenta de la sesion actual")
    @org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement(name = "token")
    public CuentaRespuestaDto yo() {
        Cuenta cuenta = servicio.cuenta(usuarioActual.id());
        return new CuentaRespuestaDto(cuenta.getId(), cuenta.getCorreo());
    }
}
