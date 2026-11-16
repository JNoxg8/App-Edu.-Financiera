package edu.ucentral.zenfi.autenticacion.infraestructura;

import edu.ucentral.zenfi.autenticacion.aplicacion.AutenticacionServicio;
import edu.ucentral.zenfi.common.excepcion.NoAutenticadoMapper;
import edu.ucentral.zenfi.common.seguridad.UsuarioActual;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import java.util.Optional;
import java.util.Set;

/**
 * Se ejecuta antes de cada endpoint: lee el token del encabezado
 * "Authorization: Bearer <token>", averigua de que usuario es y lo deja
 * en UsuarioActual. Sin un token valido responde 401 y el endpoint no se
 * ejecuta.
 *
 * Solo quedan abiertos el registro, el inicio de sesion y la version de
 * la API. Swagger (/q/...) y la app web no pasan por aqui.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class FiltroAutenticacion implements ContainerRequestFilter {

    private static final Set<String> RUTAS_PUBLICAS = Set.of("auth/registro", "auth/login");
    private static final String PREFIJO_BEARER = "Bearer ";

    @Inject
    AutenticacionServicio autenticacion;

    @Inject
    UsuarioActual usuarioActual;

    @Override
    public void filter(ContainerRequestContext peticion) {
        String ruta = peticion.getUriInfo().getPath().replaceFirst("^/", "");
        if ("OPTIONS".equals(peticion.getMethod())
                || RUTAS_PUBLICAS.contains(ruta)
                || ruta.startsWith("genericos")) {
            return;
        }

        Optional<Long> usuario = Optional.ofNullable(peticion.getHeaderString(HttpHeaders.AUTHORIZATION))
                .filter(valor -> valor.startsWith(PREFIJO_BEARER))
                .map(valor -> valor.substring(PREFIJO_BEARER.length()).trim())
                .flatMap(autenticacion::usuarioDe);

        if (usuario.isEmpty()) {
            peticion.abortWith(NoAutenticadoMapper.respuesta(
                    "Debe iniciar sesion: falta el token o ya no es valido", "/" + ruta));
            return;
        }
        usuarioActual.establecer(usuario.get());
    }
}
