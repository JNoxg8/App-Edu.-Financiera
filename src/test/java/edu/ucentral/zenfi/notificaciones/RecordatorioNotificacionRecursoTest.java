package edu.ucentral.zenfi.notificaciones;

import org.junit.jupiter.api.BeforeEach;
import edu.ucentral.zenfi.Sesiones;
import edu.ucentral.zenfi.metasahorro.dominio.repositorio.MetaAhorroRepositorio;
import edu.ucentral.zenfi.notificaciones.aplicacion.ProcesadorRecordatorios;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.RecordatorioMetaRepositorio;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integracion de recordatorios y notificaciones contra H2.
 *
 * La fecha del recordatorio y la fecha limite de la meta se mueven al
 * pasado directamente en la base, para simular el paso del tiempo.
 */
@QuarkusTest
class RecordatorioNotificacionRecursoTest {

    /** La API exige token: cada prueba usa la cuenta principal de pruebas. */
    @BeforeEach
    void iniciarSesion() {
        Sesiones.usarCuentaPrincipal();
    }

    private static final LocalDate HOY = LocalDate.now();

    @Inject
    RecordatorioMetaRepositorio recordatorios;

    @Inject
    MetaAhorroRepositorio metas;

    @Inject
    ProcesadorRecordatorios procesador;

    private int crearMeta() {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nombre": "Moto", "montoObjetivo": 5000,
                         "fechaLimite": "%s", "montoInicial": 0}
                        """.formatted(HOY.plusMonths(6)))
                .when().post("/metas-ahorro")
                .then().statusCode(201)
                .extract().path("id");
    }

    private io.restassured.response.ValidatableResponse configurar(int metaId, String frecuencia, boolean activo) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"frecuencia": "%s", "activo": %s}
                        """.formatted(frecuencia, activo))
                .when().put("/metas-ahorro/%d/recordatorio".formatted(metaId))
                .then();
    }

    private String proximo(int metaId) {
        return given().when().get("/metas-ahorro/%d/recordatorio".formatted(metaId))
                .then().statusCode(200)
                .extract().path("proximoRecordatorio");
    }

    private List<Integer> notificacionesDe(int metaId) {
        return given().when().get("/notificaciones")
                .then().statusCode(200)
                .extract().path("findAll { it.metaAhorroId == %d }.id".formatted(metaId));
    }

    private void moverProximoA(int metaId, LocalDate fecha) {
        QuarkusTransaction.requiringNew().run(() ->
                recordatorios.buscarPorMetaAhorroId((long) metaId).orElseThrow()
                        .actualizarProximoRecordatorio(fecha));
    }

    @Test
    @DisplayName("Toda meta nueva tiene un recordatorio semanal activo")
    void recordatorioPorDefecto() {
        int id = crearMeta();

        given().when().get("/metas-ahorro/%d/recordatorio".formatted(id))
                .then().statusCode(200)
                .body("frecuencia", is("SEMANAL"))
                .body("activo", is(true))
                .body("proximoRecordatorio", is(HOY.plusWeeks(1).toString()));
    }

    @Test
    @DisplayName("Apagar el recordatorio no corre la fecha; cambiar la frecuencia si la recalcula")
    void configurarRecordatorio() {
        int id = crearMeta();
        String inicial = proximo(id);

        configurar(id, "SEMANAL", false)
                .statusCode(200)
                .body("activo", is(false))
                .body("proximoRecordatorio", is(inicial));

        configurar(id, "DIARIA", true)
                .statusCode(200)
                .body("activo", is(true))
                .body("proximoRecordatorio", is(HOY.plusDays(1).toString()));
    }

    @Test
    @DisplayName("Una frecuencia invalida responde 400 y no cambia nada")
    void frecuenciaInvalida() {
        int id = crearMeta();

        configurar(id, "ANUAL", true)
                .statusCode(400)
                .body("error.codigo", is("VAL-400"));

        given().when().get("/metas-ahorro/%d/recordatorio".formatted(id))
                .then().body("frecuencia", is("SEMANAL"));
    }

    @Test
    @DisplayName("Un recordatorio vencido genera UNA notificacion al listar y avanza su fecha")
    void generaNotificacion() {
        int id = crearMeta();
        moverProximoA(id, HOY.minusDays(1));

        List<Integer> primera = notificacionesDe(id);
        assertEquals(1, primera.size());
        assertTrue(LocalDate.parse(proximo(id)).isAfter(HOY));

        // Volver a listar no duplica la notificacion
        assertEquals(1, notificacionesDe(id).size());

        given().when().get("/notificaciones")
                .then().body("find { it.metaAhorroId == %d }.titulo".formatted(id),
                        is("Recordatorio de ahorro"));
    }

    @Test
    @DisplayName("Marcar como leida saca la notificacion de las no leidas; una inexistente responde 404")
    void marcarLeida() {
        int id = crearMeta();
        moverProximoA(id, HOY);
        int notificacionId = notificacionesDe(id).get(0);

        given().when().patch("/notificaciones/%d/leida".formatted(notificacionId))
                .then().statusCode(200)
                .body("leida", is(true));

        given().when().get("/notificaciones/no-leidas")
                .then().statusCode(200)
                .body("findAll { it.id == %d }".formatted(notificacionId), hasSize(0));

        given().when().patch("/notificaciones/999999/leida")
                .then().statusCode(404)
                .body("error.codigo", is("REC-404"));
    }

    @Test
    @DisplayName("El recordatorio de una meta vencida se desactiva sin notificar")
    void metaVencidaNoNotifica() {
        int id = crearMeta();
        QuarkusTransaction.requiringNew().run(() ->
                metas.buscarPorId((long) id).orElseThrow().setFechaLimite(HOY.minusDays(1)));
        moverProximoA(id, HOY.minusDays(1));

        assertEquals(0, notificacionesDe(id).size());
        given().when().get("/metas-ahorro/%d/recordatorio".formatted(id))
                .then().body("activo", is(false));
    }

    @Test
    @DisplayName("Una meta sin recordatorio (creada antes de esta funcion) recibe el recordatorio por defecto")
    void creaRecordatoriosFaltantes() {
        int id = crearMeta();
        QuarkusTransaction.requiringNew().run(() ->
                recordatorios.eliminarPorMetaAhorroId((long) id));
        given().when().get("/metas-ahorro/%d/recordatorio".formatted(id))
                .then().statusCode(404);

        procesador.crearRecordatoriosFaltantes();

        given().when().get("/metas-ahorro/%d/recordatorio".formatted(id))
                .then().statusCode(200)
                .body("frecuencia", is("SEMANAL"));
    }
}
