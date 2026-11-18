package edu.ucentral.zenfi.common.excepcion;

import edu.ucentral.zenfi.Sesiones;
import edu.ucentral.zenfi.autenticacion.dominio.modelo.Cuenta;
import edu.ucentral.zenfi.autenticacion.dominio.repositorio.CuentaRepositorio;
import edu.ucentral.zenfi.common.infraestructura.ResponseApi;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los errores que no tienen un mapper propio tambien salen con el formato
 * ResponseApi, y un error interno no revela detalles tecnicos.
 */
@QuarkusTest
class ErroresGlobalesTest {

    @Inject
    CuentaRepositorio cuentas;

    @BeforeEach
    void autenticar() {
        Sesiones.usarCuentaPrincipal();
    }

    @Test
    @DisplayName("Metodo HTTP no permitido: 405 con formato ResponseApi")
    void metodoNoPermitido() {
        given().when().delete("/panel")
                .then().statusCode(405)
                .body("success", is(false))
                .body("status", is(405))
                .body("error.codigo", is("HTTP-405"));
    }

    @Test
    @DisplayName("JSON mal escrito: 400 con formato ResponseApi")
    void jsonMalEscrito() {
        given().contentType(ContentType.JSON).body("{\"nombre\": ")
                .when().post("/deudas")
                .then().statusCode(400)
                .body("success", is(false))
                .body("status", is(400));
    }

    @Test
    @DisplayName("Correo duplicado que llega hasta la base (registros simultaneos): 409, no 500")
    void restriccionUnicaDeLaBase() {
        String correo = "duplicado-" + UUID.randomUUID() + "@zenfi.test";
        Cuenta cuenta = new Cuenta(null, correo, "hash", LocalDateTime.now());
        QuarkusTransaction.requiringNew().run(() -> cuentas.crear(cuenta));

        // Se salta la verificacion del servicio para simular la carrera
        RuntimeException error = assertThrows(RuntimeException.class, () ->
                QuarkusTransaction.requiringNew().run(() ->
                        cuentas.crear(new Cuenta(null, correo, "hash", LocalDateTime.now()))));

        Response respuesta = ErrorInesperadoMapper.responder(error, "/auth/registro");
        assertEquals(409, respuesta.getStatus());
        assertEquals("NEG-409", ((ResponseApi) respuesta.getEntity()).getError().getCodigo());
    }

    @Test
    @DisplayName("Error inesperado: 500 con una referencia y sin el detalle tecnico")
    void errorInesperado() {
        Response respuesta = ErrorInesperadoMapper.responder(
                new IllegalStateException("tabla secreta_interna no existe"), "/deudas");

        ResponseApi cuerpo = (ResponseApi) respuesta.getEntity();
        assertEquals(500, respuesta.getStatus());
        assertEquals("ERR-500", cuerpo.getError().getCodigo());
        assertFalse(cuerpo.getMensaje().contains("secreta_interna"));
        assertTrue(cuerpo.getMensaje().startsWith("Ocurrio un error inesperado. Referencia: "));
    }
}
