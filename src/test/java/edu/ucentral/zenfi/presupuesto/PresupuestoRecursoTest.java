package edu.ucentral.zenfi.presupuesto;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Pruebas de integracion de /presupuestos contra H2, incluido el cruce
 * con /movimientos.
 *
 * Los movimientos no pueden tener fecha futura, asi que se trabaja en el
 * mes actual, donde otras clases @QuarkusTest tambien registran gastos
 * (todas comparten la misma base H2). Para no depender de esos datos, el
 * limite se define como "lo ya gastado + 1.000.000": asi el disponible y
 * el exceso se pueden comprobar con valores exactos.
 *
 * El orden importa (se crea el presupuesto, luego se agregan gastos), por
 * eso se usa @TestMethodOrder.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PresupuestoRecursoTest {

    private static final YearMonth MES = YearMonth.now();
    private static final String HOY = LocalDate.now().toString();
    private static final BigDecimal MARGEN = new BigDecimal("1000000.00");

    /** Gastado en el mes justo antes de crear el presupuesto. */
    private static BigDecimal gastadoInicial;

    /** Id del gasto que se elimina en la ultima prueba. */
    private static String gastoExtraId;

    @BeforeAll
    static void configurarNumeros() {
        RestAssured.config = RestAssuredConfig.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private static String presupuesto(String mes, String monto) {
        return """
                {"mes": "%s", "montoLimite": %s}
                """.formatted(mes, monto);
    }

    private String categoria(String tipo, String nombre) {
        return given().when().get("/categorias?tipo=" + tipo)
                .then().statusCode(200)
                .extract().path("find { it.nombre == '%s' }.id".formatted(nombre)).toString();
    }

    private String crearMovimiento(String tipo, String monto, String fecha, String categoriaId) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"tipo": "%s", "monto": %s, "fecha": "%s", "categoriaId": %s}
                        """.formatted(tipo, monto, fecha, categoriaId))
                .when().post("/movimientos")
                .then().statusCode(201)
                .extract().path("id").toString();
    }

    /** Lo gastado en el mes segun el resumen de movimientos (HU009). */
    private BigDecimal gastadoEnElMes() {
        return given()
                .when().get("/movimientos/resumen?desde=%s&hasta=%s"
                        .formatted(MES.atDay(1), MES.atEndOfMonth()))
                .then().statusCode(200)
                .extract().path("totalGastos");
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Un mes sin presupuesto responde 404")
    void sinPresupuesto() {
        given()
                .when().get("/presupuestos?mes=" + MES)
                .then()
                .statusCode(404)
                .body("error.codigo", is("REC-404"));
    }

    @Test
    @Order(2)
    @DisplayName("Sin mes ni monto responde 400 con el detalle de cada campo")
    void camposObligatorios() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when().post("/presupuestos")
                .then()
                .statusCode(400)
                .body("error.codigo", is("VAL-400"))
                .body("error.detalles", hasItem(containsString("mes")))
                .body("error.detalles", hasItem(containsString("montoLimite")));
    }

    @Test
    @Order(3)
    @DisplayName("Un mes mal escrito o un monto en cero responden 400")
    void formatoInvalido() {
        given()
                .contentType(ContentType.JSON)
                .body(presupuesto("2030-13", "1000"))
                .when().post("/presupuestos")
                .then()
                .statusCode(400)
                .body("error.detalles", hasItem(containsString("AAAA-MM")));

        given()
                .contentType(ContentType.JSON)
                .body(presupuesto(MES.toString(), "0"))
                .when().post("/presupuestos")
                .then()
                .statusCode(400)
                .body("error.detalles", hasItem(containsString("mayor a cero")));

        given()
                .when().get("/presupuestos?mes=octubre")
                .then()
                .statusCode(400)
                .body("error.codigo", is("VAL-400"));
    }

    @Test
    @Order(4)
    @DisplayName("Un mes que ya paso responde 409")
    void mesPasado() {
        given()
                .contentType(ContentType.JSON)
                .body(presupuesto(MES.minusMonths(1).toString(), "1000000"))
                .when().post("/presupuestos")
                .then()
                .statusCode(409)
                .body("error.codigo", is("NEG-409"));
    }

    // ------------------------------------------------------------------
    // Crear y consultar
    // ------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("POST /presupuestos crea el presupuesto (201) con lo ya gastado en el mes")
    void crearPresupuesto() {
        gastadoInicial = gastadoEnElMes();
        BigDecimal limite = gastadoInicial.add(MARGEN);

        given()
                .contentType(ContentType.JSON)
                .body(presupuesto(MES.toString(), limite.toPlainString()))
                .when().post("/presupuestos")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("mes", is(MES.toString()))
                .body("montoLimite", comparesEqualTo(limite))
                .body("totalGastado", comparesEqualTo(gastadoInicial))
                .body("disponible", comparesEqualTo(MARGEN))
                .body("excedido", is(false));
    }

    @Test
    @Order(6)
    @DisplayName("Un segundo presupuesto para el mismo mes responde 409")
    void presupuestoDuplicado() {
        given()
                .contentType(ContentType.JSON)
                .body(presupuesto(MES.toString(), "500000"))
                .when().post("/presupuestos")
                .then()
                .statusCode(409)
                .body("error.codigo", is("NEG-409"))
                .body("mensaje", containsString("Ya existe"));
    }

    @Test
    @Order(7)
    @DisplayName("Solo suma los gastos del mes: ignora ingresos y gastos de otros meses")
    void sumaGastosDelMes() {
        String alimentacion = categoria("GASTO", "Alimentación");
        crearMovimiento("GASTO", "750000", HOY, alimentacion);
        crearMovimiento("INGRESO", "2000000", HOY, categoria("INGRESO", "Salario"));
        crearMovimiento("GASTO", "90000", MES.minusMonths(1).atDay(1).toString(), alimentacion);

        given()
                .when().get("/presupuestos?mes=" + MES)
                .then()
                .statusCode(200)
                .body("totalGastado", comparesEqualTo(gastadoInicial.add(new BigDecimal("750000"))))
                .body("disponible", comparesEqualTo(new BigDecimal("250000.00")))
                .body("excedido", is(false));
    }

    @Test
    @Order(8)
    @DisplayName("Al superar el limite el porcentaje pasa de 100 y queda excedido")
    void limiteExcedido() {
        gastoExtraId = crearMovimiento("GASTO", "400000", HOY, categoria("GASTO", "Alimentación"));

        given()
                .when().get("/presupuestos?mes=" + MES)
                .then()
                .statusCode(200)
                .body("disponible", comparesEqualTo(new BigDecimal("-150000.00")))
                .body("porcentajeUtilizado", greaterThan(new BigDecimal("100")))
                .body("excedido", is(true));
    }

    @Test
    @Order(9)
    @DisplayName("Al eliminar un gasto se recalcula solo; sin ?mes se consulta el mes actual")
    void recalculaAlEliminar() {
        given().when().delete("/movimientos/" + gastoExtraId).then().statusCode(204);

        given()
                .when().get("/presupuestos")
                .then()
                .statusCode(200)
                .body("mes", is(MES.toString()))
                .body("disponible", comparesEqualTo(new BigDecimal("250000.00")))
                .body("excedido", is(false));
    }
}
