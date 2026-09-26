package edu.ucentral.zenfi.deudas;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test de INTEGRACION: levanta la aplicacion completa y le pega
 * por HTTP, igual que lo hara Flutter.
 *
 * Usa H2 en memoria (perfil %test), asi que no toca finanzas_db
 * ni necesita que PostgreSQL este corriendo.
 */
@QuarkusTest
class DeudaRecursoTest {

    private static final String VENCIMIENTO = LocalDate.now().plusYears(2).toString();

    /**
     * Por defecto RestAssured lee los numeros JSON como float o double,
     * lo que hace fragiles las comparaciones de dinero. Aqui se le pide
     * que los lea como BigDecimal.
     */
    @BeforeAll
    static void configurarNumeros() {
        RestAssured.config = RestAssuredConfig.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    private String jsonDeuda(String acreedor, String monto, String tasa, int cuotas) {
        return """
                {
                  "acreedor": "%s",
                  "montoTotal": %s,
                  "tasaInteresAnual": %s,
                  "numeroCuotas": %d,
                  "fechaVencimiento": "%s"
                }
                """.formatted(acreedor, monto, tasa, cuotas, VENCIMIENTO);
    }

    /** Crea una deuda y devuelve su id como texto, para usarlo en la URL. */
    private String crearDeuda(String acreedor, String monto, String tasa, int cuotas) {
        return given()
                .contentType(ContentType.JSON)
                .body(jsonDeuda(acreedor, monto, tasa, cuotas))
                .when()
                .post("/deudas")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
                .toString();
    }

    @Test
    @DisplayName("POST /deudas crea la deuda y calcula los valores derivados")
    void crearDeuda() {
        given()
                .contentType(ContentType.JSON)
                .body(jsonDeuda("Banco Test", "5000000", "24.00", 24))
                .when()
                .post("/deudas")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("acreedor", is("Banco Test"))
                .body("cuotaMensual", notNullValue())
                .body("totalAPagar", notNullValue())
                .body("costoIntereses", notNullValue())
                .body("saldoPendiente", notNullValue())
                .body("pagada", is(false))
                .body("abonos.size()", is(0));
    }

    @Test
    @DisplayName("POST /deudas rechaza datos invalidos con 400 y detalle por campo")
    void crearDeudaInvalida() {
        String invalido = """
                {
                  "acreedor": "",
                  "montoTotal": -100,
                  "tasaInteresAnual": 24.00,
                  "numeroCuotas": 0,
                  "fechaVencimiento": "2020-01-01"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(invalido)
                .when()
                .post("/deudas")
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("mensaje", containsString("validacion"))
                .body("error.codigo", is("VAL-400"))
                .body("error.detalles.size()", greaterThan(0));
    }

    @Test
    @DisplayName("POST /deudas/{id}/abonos descuenta del saldo pendiente")
    void registrarAbono() {
        // Sin intereses: 1.200.000 en 12 cuotas => total a pagar 1.200.000
        String id = crearDeuda("Banco Abonos", "1200000", "0.00", 12);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "monto": 200000, "fecha": "%s" }
                        """.formatted(LocalDate.now()))
                .when()
                .post("/deudas/" + id + "/abonos")
                .then()
                .statusCode(200)
                .body("totalAbonado", comparesEqualTo(new BigDecimal("200000.00")))
                .body("saldoPendiente", comparesEqualTo(new BigDecimal("1000000.00")))
                .body("abonos.size()", is(1));
    }

    @Test
    @DisplayName("Un abono que cubre el total deja la deuda pagada")
    void abonoTotal() {
        String id = crearDeuda("Banco Total", "500000", "0.00", 5);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "monto": 500000 }
                        """)
                .when()
                .post("/deudas/" + id + "/abonos")
                .then()
                .statusCode(200)
                .body("pagada", is(true))
                .body("saldoPendiente", comparesEqualTo(new BigDecimal("0.00")))
                .body("porcentajePagado", comparesEqualTo(new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("Un abono mayor al saldo devuelve 409")
    void abonoExcedeSaldo() {
        String id = crearDeuda("Banco Exceso", "100000", "0.00", 1);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "monto": 99999999 }
                        """)
                .when()
                .post("/deudas/" + id + "/abonos")
                .then()
                .statusCode(409)
                .body("success", is(false))
                .body("error.codigo", is("NEG-409"))
                .body("mensaje", containsString("excede"));
    }

    @Test
    @DisplayName("GET /deudas/{id} inexistente devuelve 404")
    void deudaInexistente() {
        given()
                .when()
                .get("/deudas/999999")
                .then()
                .statusCode(404)
                .body("error.codigo", is("REC-404"));
    }

    @Test
    @DisplayName("DELETE /deudas/{id} elimina la deuda")
    void eliminarDeuda() {
        String id = crearDeuda("Banco Borrar", "500000", "0.00", 5);

        given().when().delete("/deudas/" + id).then().statusCode(204);
        given().when().get("/deudas/" + id).then().statusCode(404);
    }

    @Test
    @DisplayName("GET /deudas devuelve el listado del usuario")
    void listarDeudas() {
        crearDeuda("Banco Listado", "300000", "0.00", 3);

        given()
                .when()
                .get("/deudas")
                .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    @DisplayName("PUT /deudas/{id} actualiza los datos editables")
    void actualizarDeuda() {
        String id = crearDeuda("Banco Original", "1200000", "0.00", 12);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "acreedor": "Banco Editado",
                          "tasaInteresAnual": 0.00,
                          "numeroCuotas": 24,
                          "fechaVencimiento": "%s"
                        }
                        """.formatted(VENCIMIENTO))
                .when()
                .put("/deudas/" + id)
                .then()
                .statusCode(200)
                .body("acreedor", is("Banco Editado"))
                .body("numeroCuotas", is(24))
                // 1.200.000 / 24 = 50.000
                .body("cuotaMensual", comparesEqualTo(new BigDecimal("50000.00")))
                // El capital no cambia
                .body("montoTotal", comparesEqualTo(new BigDecimal("1200000.00")));
    }

    @Test
    @DisplayName("PUT sobre una deuda inexistente devuelve 404")
    void actualizarInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "acreedor": "Banco X",
                          "tasaInteresAnual": 10.00,
                          "numeroCuotas": 12,
                          "fechaVencimiento": "%s"
                        }
                        """.formatted(VENCIMIENTO))
                .when()
                .put("/deudas/999999")
                .then()
                .statusCode(404)
                .body("error.codigo", is("REC-404"));
    }
}
