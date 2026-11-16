package edu.ucentral.zenfi.panel;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de integracion de GET /panel contra H2.
 *
 * Todas las clases @QuarkusTest comparten la misma base, asi que no se
 * conocen los totales de antemano: se toma el panel antes y despues de
 * registrar datos y se compara la DIFERENCIA, que si es exacta.
 */
@QuarkusTest
class PanelRecursoTest {

    private static final String HOY = LocalDate.now().toString();

    @BeforeAll
    static void configurarNumeros() {
        RestAssured.config = RestAssuredConfig.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    private ExtractableResponse<Response> panel() {
        return given().when().get("/panel")
                .then().statusCode(200)
                .extract();
    }

    private String categoria(String tipo) {
        return given().when().get("/categorias?tipo=" + tipo)
                .then().statusCode(200)
                .extract().path("find { it.nombre == 'Otros' }.id").toString();
    }

    private void movimiento(String tipo, String monto) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "tipo": "%s", "monto": %s, "fecha": "%s",
                          "descripcion": "Panel", "categoriaId": %s }
                        """.formatted(tipo, monto, HOY, categoria(tipo)))
                .when().post("/movimientos")
                .then().statusCode(201);
    }

    private BigDecimal numero(ExtractableResponse<Response> r, String campo) {
        return r.path(campo);
    }

    @Test
    @DisplayName("Un ingreso y un gasto de hoy cambian el balance y los gastos del mes")
    void balanceYGastosDelMes() {
        var antes = panel();

        movimiento("INGRESO", "1000000");
        movimiento("GASTO", "300000");

        var despues = panel();
        assertEquals(0, new BigDecimal("700000").compareTo(
                numero(despues, "balanceGeneral").subtract(numero(antes, "balanceGeneral"))));
        assertEquals(0, new BigDecimal("300000").compareTo(
                numero(despues, "gastosMesActual").subtract(numero(antes, "gastosMesActual"))));
        assertEquals(true, despues.path("tieneMovimientos"));
        assertEquals(YearMonth.now().toString(), despues.path("mes"));
    }

    @Test
    @DisplayName("El desglose por categoria suma los gastos del mes, y el presupuesto usa ese mismo total")
    void desgloseCuadra() {
        movimiento("GASTO", "25000");

        var p = panel();
        BigDecimal gastosMes = numero(p, "gastosMesActual");
        List<Object> totales = p.path("gastosMesPorCategoria.total");
        BigDecimal suma = totales.stream()
                .map(n -> new BigDecimal(n.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, gastosMes.compareTo(suma));

        // Otras pruebas pueden haber creado el presupuesto de este mes (H2 compartida)
        Object presupuesto = p.path("presupuesto");
        if (presupuesto != null) {
            assertEquals(0, gastosMes.compareTo(numero(p, "presupuesto.totalGastado")));
        }
    }

    @Test
    @DisplayName("Una meta activa aparece con su avance y desaparece al cumplirse")
    void metaActiva() {
        int id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nombre": "Portatil panel", "montoObjetivo": 2000,
                         "fechaLimite": "%s", "montoInicial": 500}
                        """.formatted(LocalDate.now().plusMonths(3)))
                .when().post("/metas-ahorro")
                .then().statusCode(201)
                .extract().path("id");

        given().when().get("/panel")
                .then().statusCode(200)
                .body("metasActivas.find { it.id == %d }.montoAhorrado".formatted(id),
                        comparesEqualTo(new BigDecimal("500")))
                .body("metasActivas.find { it.id == %d }.porcentaje".formatted(id),
                        comparesEqualTo(new BigDecimal("25")));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"monto": 1500, "fecha": "%s"}
                        """.formatted(HOY))
                .when().post("/metas-ahorro/%d/aportes".formatted(id))
                .then().statusCode(201);

        List<?> conEseId = panel().path("metasActivas.findAll { it.id == %d }".formatted(id));
        assertEquals(0, conEseId.size());
    }

    @Test
    @DisplayName("Una deuda pendiente aparece con su saldo y desaparece al pagarse")
    void deudaPendiente() {
        int id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"acreedor": "Banco panel", "montoTotal": 600000,
                         "tasaInteresAnual": 0.00, "numeroCuotas": 6,
                         "fechaVencimiento": "%s"}
                        """.formatted(LocalDate.now().plusYears(1)))
                .when().post("/deudas")
                .then().statusCode(201)
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"monto": 200000, "fecha": "%s"}
                        """.formatted(HOY))
                .when().post("/deudas/%d/abonos".formatted(id))
                .then().statusCode(200);

        given().when().get("/panel")
                .then().statusCode(200)
                .body("deudasPendientes.find { it.id == %d }.saldoPendiente".formatted(id),
                        comparesEqualTo(new BigDecimal("400000")))
                .body("deudasPendientes.find { it.id == %d }.acreedor".formatted(id),
                        is("Banco panel"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"monto": 400000, "fecha": "%s"}
                        """.formatted(HOY))
                .when().post("/deudas/%d/abonos".formatted(id))
                .then().statusCode(200);

        given().when().get("/panel")
                .then().body("deudasPendientes.findAll { it.id == %d }".formatted(id), hasSize(0));
    }
}
