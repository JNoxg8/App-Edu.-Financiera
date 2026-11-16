package edu.ucentral.zenfi.metasahorro;

import org.junit.jupiter.api.BeforeEach;
import edu.ucentral.zenfi.Sesiones;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Pruebas de integracion de metas de ahorro y sus aportes (HU004, HU021)
 * contra H2. Cada prueba crea su propia meta, asi no dependen del orden.
 */
@QuarkusTest
class AporteMetaAhorroRecursoTest {

    /** La API exige token: cada prueba usa la cuenta principal de pruebas. */
    @BeforeEach
    void iniciarSesion() {
        Sesiones.usarCuentaPrincipal();
    }

    private static final LocalDate HOY = LocalDate.now();

    @Inject
    MetaAhorroRepositorio metas;

    @BeforeAll
    static void configurarNumeros() {
        RestAssured.config = RestAssuredConfig.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    private int crearMeta(String objetivo, String inicial) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nombre": "Viaje", "montoObjetivo": %s,
                         "fechaLimite": "%s", "montoInicial": %s}
                        """.formatted(objetivo, HOY.plusMonths(6), inicial))
                .when().post("/metas-ahorro")
                .then().statusCode(201)
                .extract().path("id");
    }

    private io.restassured.response.ValidatableResponse aportar(int metaId, String cuerpo) {
        return given()
                .contentType(ContentType.JSON)
                .body(cuerpo)
                .when().post("/metas-ahorro/%d/aportes".formatted(metaId))
                .then();
    }

    private String estado(int metaId) {
        return given().when().get("/metas-ahorro")
                .then().statusCode(200)
                .extract().path("find { it.id == %d }.estado".formatted(metaId));
    }

    @Test
    @DisplayName("El monto inicial queda como primer aporte del historial")
    void montoInicialEnHistorial() {
        int id = crearMeta("1000", "200");

        given().when().get("/metas-ahorro/%d/aportes".formatted(id))
                .then().statusCode(200)
                .body("", hasSize(1))
                .body("[0].monto", comparesEqualTo(new BigDecimal("200")))
                .body("[0].fecha", is(HOY.toString()));
    }

    @Test
    @DisplayName("Un aporte sin fecha o con monto en cero responde 400 y no se guarda")
    void aporteInvalido() {
        int id = crearMeta("1000", "0");

        aportar(id, """
                {"monto": 100}
                """)
                .statusCode(400)
                .body("error.codigo", is("VAL-400"));

        aportar(id, """
                {"monto": 0, "fecha": "%s"}
                """.formatted(HOY))
                .statusCode(400);

        given().when().get("/metas-ahorro/%d/aportes".formatted(id))
                .then().body("", hasSize(0));
    }

    @Test
    @DisplayName("Un aporte con fecha futura responde 409")
    void fechaFutura() {
        int id = crearMeta("1000", "0");

        aportar(id, """
                {"monto": 100, "fecha": "%s"}
                """.formatted(HOY.plusDays(1)))
                .statusCode(409)
                .body("error.codigo", is("NEG-409"));
    }

    @Test
    @DisplayName("Al alcanzar el objetivo la meta queda COMPLETADA y ya no acepta aportes")
    void completarMeta() {
        int id = crearMeta("1000", "200");

        aportar(id, """
                {"monto": 900, "fecha": "%s"}
                """.formatted(HOY))
                .statusCode(409);   // supera lo pendiente (800)

        aportar(id, """
                {"monto": 800, "fecha": "%s"}
                """.formatted(HOY))
                .statusCode(201);

        assertEstado(id, "COMPLETADA");

        aportar(id, """
                {"monto": 1, "fecha": "%s"}
                """.formatted(HOY))
                .statusCode(409);
    }

    @Test
    @DisplayName("Eliminar un aporte recalcula el acumulado y la meta vuelve a ACTIVA")
    void eliminarAporte() {
        int id = crearMeta("1000", "200");
        int aporteId = aportar(id, """
                {"monto": 800, "fecha": "%s"}
                """.formatted(HOY))
                .statusCode(201)
                .extract().path("id");

        given().when().delete("/metas-ahorro/%d/aportes/%d".formatted(id, aporteId))
                .then().statusCode(204);

        given().when().get("/metas-ahorro")
                .then().body("find { it.id == %d }.montoAcumulado".formatted(id),
                        comparesEqualTo(new BigDecimal("200")));
        assertEstado(id, "ACTIVA");
    }

    @Test
    @DisplayName("Si pasa la fecha limite sin alcanzar el objetivo, la meta queda VENCIDA y no acepta aportes")
    void metaVencida() {
        int id = crearMeta("1000", "200");
        int aporteId = aportar(id, """
                {"monto": 100, "fecha": "%s"}
                """.formatted(HOY))
                .statusCode(201)
                .extract().path("id");

        vencer(id);

        assertEstado(id, "VENCIDA");

        aportar(id, """
                {"monto": 100, "fecha": "%s"}
                """.formatted(HOY))
                .statusCode(409);

        // Borrar un aporte no la reactiva: la fecha limite ya paso
        given().when().delete("/metas-ahorro/%d/aportes/%d".formatted(id, aporteId))
                .then().statusCode(204);
        assertEstado(id, "VENCIDA");
    }

    @Test
    @DisplayName("Eliminar una meta borra sus aportes y su recordatorio (HU021)")
    void eliminarMeta() {
        int id = crearMeta("1000", "200");

        given().when().delete("/metas-ahorro/%d".formatted(id))
                .then().statusCode(204);

        given().when().get("/metas-ahorro/%d/aportes".formatted(id))
                .then().statusCode(404);
        given().when().get("/metas-ahorro/%d/recordatorio".formatted(id))
                .then().statusCode(404);
    }

    @Test
    @DisplayName("Una meta que no existe responde 404 (REC-404) en todos sus endpoints")
    void metaInexistente() {
        given().when().delete("/metas-ahorro/999999")
                .then().statusCode(404).body("error.codigo", is("REC-404"));
        given().when().get("/metas-ahorro/999999/aportes")
                .then().statusCode(404);
        given().when().get("/metas-ahorro/999999/recordatorio")
                .then().statusCode(404);
        aportar(999999, """
                {"monto": 100, "fecha": "%s"}
                """.formatted(HOY))
                .statusCode(404);
    }

    private void assertEstado(int metaId, String esperado) {
        org.junit.jupiter.api.Assertions.assertEquals(esperado, estado(metaId));
    }

    /** Simula que ya paso la fecha limite (la API no permite crear metas vencidas). */
    private void vencer(int metaId) {
        QuarkusTransaction.requiringNew().run(() ->
                metas.buscarPorId((long) metaId).orElseThrow()
                        .setFechaLimite(HOY.minusDays(1)));
    }
}
