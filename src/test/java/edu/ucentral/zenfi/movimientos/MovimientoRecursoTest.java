package edu.ucentral.zenfi.movimientos;

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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test de INTEGRACION de /movimientos, incluido el cruce con /deudas.
 *
 * Todas las clases @QuarkusTest comparten la misma base H2, asi que
 * cada prueba usa sus propias fechas o textos para no depender de los
 * datos que dejan las demas.
 */
@QuarkusTest
class MovimientoRecursoTest {

    private static final String HOY = LocalDate.now().toString();
    private static final String VENCIMIENTO = LocalDate.now().plusYears(2).toString();

    @BeforeAll
    static void configurarNumeros() {
        RestAssured.config = RestAssuredConfig.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private String categoria(String tipo, String nombre) {
        return given().when().get("/categorias?tipo=" + tipo)
                .then().statusCode(200)
                .extract().path("find { it.nombre == '%s' }.id".formatted(nombre)).toString();
    }

    private String json(String tipo, String monto, String fecha, String descripcion,
                        String categoriaId, String deudaId) {
        return """
                {
                  "tipo": "%s",
                  "monto": %s,
                  "fecha": "%s",
                  "descripcion": %s,
                  "categoriaId": %s,
                  "deudaId": %s
                }
                """.formatted(tipo, monto, fecha,
                descripcion == null ? "null" : "\"" + descripcion + "\"",
                categoriaId, deudaId == null ? "null" : deudaId);
    }

    private String crearMovimiento(String tipo, String monto, String fecha,
                                   String descripcion, String categoriaId) {
        return given()
                .contentType(ContentType.JSON)
                .body(json(tipo, monto, fecha, descripcion, categoriaId, null))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(201)
                .extract().path("id").toString();
    }

    private String crearDeuda(String acreedor, String monto) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "acreedor": "%s",
                          "montoTotal": %s,
                          "tasaInteresAnual": 0.00,
                          "numeroCuotas": 10,
                          "fechaVencimiento": "%s"
                        }
                        """.formatted(acreedor, monto, VENCIMIENTO))
                .when()
                .post("/deudas")
                .then()
                .statusCode(201)
                .extract().path("id").toString();
    }

    // ------------------------------------------------------------------
    // HU009: registrar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /movimientos registra un gasto y responde 201")
    void registrarGasto() {
        given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "45000", HOY, "Mercado", categoria("GASTO", "Alimentación"), null))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("tipo", is("GASTO"))
                .body("monto", comparesEqualTo(new BigDecimal("45000.00")))
                .body("categoria.nombre", is("Alimentación"))
                .body("abonoDeuda", is(false));
    }

    @Test
    @DisplayName("La descripcion es opcional")
    void sinDescripcion() {
        given()
                .contentType(ContentType.JSON)
                .body(json("INGRESO", "2000000", HOY, null, categoria("INGRESO", "Salario"), null))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(201)
                .body("descripcion", nullValue());
    }

    @Test
    @DisplayName("Campos obligatorios vacios: 400 con detalle y no se registra")
    void camposObligatorios() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "monto": 0, "descripcion": "Sin datos" }
                        """)
                .when()
                .post("/movimientos")
                .then()
                .statusCode(400)
                .body("error.codigo", is("VAL-400"))
                .body("error.detalles", hasItem(containsString("tipo")))
                .body("error.detalles", hasItem(containsString("monto")))
                .body("error.detalles", hasItem(containsString("fecha")))
                .body("error.detalles", hasItem(containsString("categoriaId")));

        given().when().get("/movimientos?buscar=Sin datos")
                .then().statusCode(200).body("size()", is(0));
    }

    @Test
    @DisplayName("Un ingreso con categoria de gastos devuelve 409")
    void categoriaDeOtroTipo() {
        given()
                .contentType(ContentType.JSON)
                .body(json("INGRESO", "1000", HOY, null, categoria("GASTO", "Transporte"), null))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(409)
                .body("error.codigo", is("NEG-409"));
    }

    @Test
    @DisplayName("Se puede registrar con una categoria personalizada recien creada")
    void conCategoriaPersonalizada() {
        String id = given()
                .contentType(ContentType.JSON)
                .body("""
                        { "nombre": "Freelance", "tipo": "INGRESO" }
                        """)
                .when().post("/categorias")
                .then().statusCode(201).extract().path("id").toString();

        given()
                .contentType(ContentType.JSON)
                .body(json("INGRESO", "350000", HOY, "Logo para cliente", id, null))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(201)
                .body("categoria.nombre", is("Freelance"));

        // Con movimientos ya no se puede borrar la categoria
        given().when().delete("/categorias/" + id).then().statusCode(409);
    }

    // ------------------------------------------------------------------
    // Filtrar, buscar, ordenar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Filtra por tipo, categoria y fechas, y ordena por monto")
    void filtrarYOrdenar() {
        String transporte = categoria("GASTO", "Transporte");
        crearMovimiento("GASTO", "3000", "2001-02-10", "Bus", transporte);
        crearMovimiento("GASTO", "25000", "2001-02-11", "Taxi", transporte);
        crearMovimiento("GASTO", "12000", "2001-02-12", "Gasolina", transporte);
        crearMovimiento("GASTO", "99000", "2001-02-12", "Cine", categoria("GASTO", "Entretenimiento"));

        given()
                .when()
                .get("/movimientos?tipo=gasto&categoriaId=" + transporte
                        + "&desde=2001-02-01&hasta=2001-02-28&ordenarPor=monto&direccion=desc")
                .then()
                .statusCode(200)
                .body("size()", is(3))
                .body("[0].descripcion", is("Taxi"))
                .body("[1].descripcion", is("Gasolina"))
                .body("[2].descripcion", is("Bus"))
                .body("categoria.nombre", everyItem(is("Transporte")));
    }

    @Test
    @DisplayName("Por defecto ordena por fecha, del mas reciente al mas antiguo")
    void ordenPorDefecto() {
        String otros = categoria("GASTO", "Otros");
        crearMovimiento("GASTO", "1000", "2002-05-01", "primero", otros);
        crearMovimiento("GASTO", "1000", "2002-05-20", "ultimo", otros);

        given()
                .when()
                .get("/movimientos?desde=2002-05-01&hasta=2002-05-31")
                .then()
                .statusCode(200)
                .body("[0].descripcion", is("ultimo"))
                .body("[1].descripcion", is("primero"));
    }

    @Test
    @DisplayName("Busca por texto en la descripcion sin importar mayusculas")
    void buscar() {
        crearMovimiento("GASTO", "18000", HOY, "Almuerzo con Zapatico", categoria("GASTO", "Alimentación"));

        given()
                .when()
                .get("/movimientos?buscar=zapATICO")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].descripcion", is("Almuerzo con Zapatico"));
    }

    @Test
    @DisplayName("Un parametro de orden invalido devuelve 400")
    void ordenInvalido() {
        given().when().get("/movimientos?ordenarPor=color")
                .then()
                .statusCode(400)
                .body("error.codigo", is("VAL-400"));
    }

    @Test
    @DisplayName("Un rango de fechas invertido devuelve 409")
    void rangoInvertido() {
        given().when().get("/movimientos?desde=2020-12-31&hasta=2020-01-01")
                .then()
                .statusCode(409);
    }

    // ------------------------------------------------------------------
    // Editar y eliminar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PUT /movimientos/{id} edita el movimiento")
    void editar() {
        String id = crearMovimiento("GASTO", "5000", HOY, "Cafe", categoria("GASTO", "Alimentación"));

        given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "7000", HOY, "Cafe y pan", categoria("GASTO", "Otros"), null))
                .when()
                .put("/movimientos/" + id)
                .then()
                .statusCode(200)
                .body("monto", comparesEqualTo(new BigDecimal("7000.00")))
                .body("descripcion", is("Cafe y pan"))
                .body("categoria.nombre", is("Otros"));
    }

    @Test
    @DisplayName("DELETE /movimientos/{id} elimina el movimiento")
    void eliminar() {
        String id = crearMovimiento("GASTO", "5000", HOY, "Borrar", categoria("GASTO", "Otros"));

        given().when().delete("/movimientos/" + id).then().statusCode(204);
        given().when().get("/movimientos/" + id)
                .then().statusCode(404).body("error.codigo", is("REC-404"));
    }

    // ------------------------------------------------------------------
    // Cruce con deudas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un abono hecho en /deudas aparece como gasto en la categoria Deudas")
    void abonoDesdeDeudasEsGasto() {
        String deuda = crearDeuda("Banco Cruce", "1000000");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "monto": 150000, "fecha": "%s" }
                        """.formatted(HOY))
                .when().post("/deudas/" + deuda + "/abonos")
                .then().statusCode(200);

        given()
                .when()
                .get("/movimientos?buscar=Banco Cruce")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].tipo", is("GASTO"))
                .body("[0].categoria.nombre", is("Deudas"))
                .body("[0].monto", comparesEqualTo(new BigDecimal("150000.00")))
                .body("[0].deudaId.toString()", is(deuda))
                .body("[0].abonoDeuda", is(true));
    }

    @Test
    @DisplayName("Un gasto en Deudas desde /movimientos abona la deuda y baja su saldo")
    void gastoEnDeudasAbona() {
        String deuda = crearDeuda("Banco Movimiento", "1000000");

        given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "200000", HOY, "Cuota uno", categoria("GASTO", "Deudas"), deuda))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(201)
                .body("categoria.nombre", is("Deudas"))
                .body("descripcion", is("Cuota uno"))
                .body("abonoDeuda", is(true));

        given().when().get("/deudas/" + deuda)
                .then()
                .statusCode(200)
                .body("totalAbonado", comparesEqualTo(new BigDecimal("200000.00")))
                .body("saldoPendiente", comparesEqualTo(new BigDecimal("800000.00")));

        // Un solo gasto por abono: no se duplica
        given().when().get("/movimientos?buscar=Cuota uno")
                .then().body("size()", is(1));
    }

    @Test
    @DisplayName("Un gasto en Deudas que excede el saldo devuelve 409 y no deja nada guardado")
    void gastoEnDeudasExcedeSaldo() {
        String deuda = crearDeuda("Banco Excede", "100000");

        given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "999999", HOY, "Excede", categoria("GASTO", "Deudas"), deuda))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(409)
                .body("mensaje", containsString("excede"));

        given().when().get("/movimientos?buscar=Banco Excede").then().body("size()", is(0));
        given().when().get("/deudas/" + deuda)
                .then().body("totalAbonado", comparesEqualTo(new BigDecimal("0.00")));
    }

    @Test
    @DisplayName("Un gasto en Deudas sin deudaId devuelve 409")
    void deudasSinDeuda() {
        given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "1000", HOY, null, categoria("GASTO", "Deudas"), null))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(409)
                .body("mensaje", containsString("deuda"));
    }

    @Test
    @DisplayName("Un gasto con una deuda inexistente devuelve 404")
    void deudaInexistente() {
        given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "1000", HOY, null, categoria("GASTO", "Deudas"), "999999"))
                .when()
                .post("/movimientos")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Eliminar el gasto de un abono devuelve el saldo a la deuda")
    void eliminarGastoRevierteAbono() {
        String deuda = crearDeuda("Banco Revertir", "500000");
        String movimiento = given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "100000", HOY, null, categoria("GASTO", "Deudas"), deuda))
                .when().post("/movimientos")
                .then().statusCode(201).extract().path("id").toString();

        given().when().delete("/movimientos/" + movimiento).then().statusCode(204);

        given().when().get("/movimientos/" + movimiento).then().statusCode(404);
        given().when().get("/deudas/" + deuda)
                .then()
                .body("saldoPendiente", comparesEqualTo(new BigDecimal("500000.00")))
                .body("abonos.size()", is(0));
    }

    @Test
    @DisplayName("En el gasto de un abono no se puede cambiar el monto (409)")
    void editarMontoDeAbono() {
        String deuda = crearDeuda("Banco Editar", "500000");
        String deudas = categoria("GASTO", "Deudas");
        String movimiento = given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "100000", HOY, null, deudas, deuda))
                .when().post("/movimientos")
                .then().statusCode(201).extract().path("id").toString();

        given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "1", HOY, null, deudas, null))
                .when().put("/movimientos/" + movimiento)
                .then().statusCode(409);

        given()
                .contentType(ContentType.JSON)
                .body(json("GASTO", "100000", HOY, "Nota nueva", deudas, null))
                .when().put("/movimientos/" + movimiento)
                .then().statusCode(200).body("descripcion", is("Nota nueva"));
    }

    @Test
    @DisplayName("Al eliminar la deuda sus gastos se conservan pero desvinculados")
    void eliminarDeudaConservaGastos() {
        String deuda = crearDeuda("Banco Eliminada", "500000");
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "monto": 50000 }
                        """)
                .when().post("/deudas/" + deuda + "/abonos")
                .then().statusCode(200);

        given().when().delete("/deudas/" + deuda).then().statusCode(204);

        given().when().get("/movimientos?buscar=Banco Eliminada")
                .then()
                .body("size()", is(1))
                .body("[0].deudaId", nullValue())
                .body("[0].abonoDeuda", is(false));
    }

    // ------------------------------------------------------------------
    // Resumen
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /movimientos/resumen calcula totales y reparte por categoria")
    void resumen() {
        crearMovimiento("INGRESO", "1000000", "2003-07-01", null, categoria("INGRESO", "Salario"));
        crearMovimiento("GASTO", "300000", "2003-07-05", null, categoria("GASTO", "Vivienda"));
        crearMovimiento("GASTO", "100000", "2003-07-06", null, categoria("GASTO", "Transporte"));

        given()
                .when()
                .get("/movimientos/resumen?desde=2003-07-01&hasta=2003-07-31")
                .then()
                .statusCode(200)
                .body("totalIngresos", comparesEqualTo(new BigDecimal("1000000.00")))
                .body("totalGastos", comparesEqualTo(new BigDecimal("400000.00")))
                .body("balance", comparesEqualTo(new BigDecimal("600000.00")))
                .body("porcentajeGastado", comparesEqualTo(new BigDecimal("40.00")))
                .body("gastosPorCategoria[0].categoria", is("Vivienda"))
                .body("gastosPorCategoria[0].porcentaje", comparesEqualTo(new BigDecimal("75.00")));
    }

    @Test
    @DisplayName("GET /movimientos devuelve el listado del usuario")
    void listar() {
        crearMovimiento("GASTO", "1000", HOY, null, categoria("GASTO", "Salud"));

        given().when().get("/movimientos")
                .then().statusCode(200).body("size()", greaterThan(0));
    }
}
