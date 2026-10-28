package edu.ucentral.zenfi.reporte;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de integracion de /reportes/mensual contra H2, cruzando
 * movimientos y presupuesto.
 *
 * Todas las clases @QuarkusTest comparten la base H2. Para tener cifras
 * exactas se usan meses que ninguna otra prueba toca: 2004-03 y 2004-04
 * (movimientos en el pasado) y un mes dentro de 3 anos (presupuesto
 * futuro). Para el mes actual solo se comprueba que las cifras cuadren.
 */
@QuarkusTest
class ReporteRecursoTest {

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

    private void crearMovimiento(String tipo, String monto, String fecha, String categoriaId) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"tipo": "%s", "monto": %s, "fecha": "%s", "categoriaId": %s}
                        """.formatted(tipo, monto, fecha, categoriaId))
                .when().post("/movimientos")
                .then().statusCode(201);
    }

    private ValidatableResponse reporte(String mes) {
        return given().when().get("/reportes/mensual?mes=" + mes).then();
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un mes mal escrito responde 400")
    void formatoInvalido() {
        reporte("2026-13")
                .statusCode(400)
                .body("error.codigo", is("VAL-400"))
                .body("error.detalles", hasItem(containsString("AAAA-MM")));
    }

    @Test
    @DisplayName("Un mes sin gastos ni presupuesto responde 200 con el reporte vacio")
    void mesVacio() {
        reporte("2004-04")
                .statusCode(200)
                .body("mes", is("2004-04"))
                .body("tieneGastos", is(false))
                .body("totalGastado", comparesEqualTo(BigDecimal.ZERO))
                .body("presupuesto", nullValue())
                .body("gastosPorCategoria", empty());
    }

    @Test
    @DisplayName("Sin presupuesto muestra el desglose por categoria; los ingresos no cuentan")
    void desglosePorCategoria() {
        String vivienda = categoria("GASTO", "Vivienda");
        String transporte = categoria("GASTO", "Transporte");
        crearMovimiento("GASTO", "150000", "2004-03-02", transporte);
        crearMovimiento("GASTO", "450000", "2004-03-05", vivienda);
        crearMovimiento("INGRESO", "2000000", "2004-03-01", categoria("INGRESO", "Salario"));

        reporte("2004-03")
                .statusCode(200)
                .body("tieneGastos", is(true))
                .body("totalGastado", comparesEqualTo(new BigDecimal("600000")))
                .body("presupuesto", nullValue())
                .body("gastosPorCategoria", hasSize(2))
                // La categoria con mas gasto va primero
                .body("gastosPorCategoria[0].categoria", is("Vivienda"))
                .body("gastosPorCategoria[0].porcentaje", comparesEqualTo(new BigDecimal("75")))
                .body("gastosPorCategoria[1].categoria", is("Transporte"))
                .body("gastosPorCategoria[1].total", comparesEqualTo(new BigDecimal("150000")))
                .body("gastosPorCategoria[1].porcentaje", comparesEqualTo(new BigDecimal("25")));
    }

    @Test
    @DisplayName("Un mes futuro con presupuesto y sin gastos muestra el limite al 0 %")
    void presupuestoSinGastos() {
        String mes = YearMonth.now().plusYears(3).toString();
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"mes": "%s", "montoLimite": 800000}
                        """.formatted(mes))
                .when().post("/presupuestos")
                .then().statusCode(201);

        reporte(mes)
                .statusCode(200)
                .body("tieneGastos", is(false))
                .body("presupuesto.montoLimite", comparesEqualTo(new BigDecimal("800000")))
                .body("presupuesto.porcentajeUtilizado", comparesEqualTo(BigDecimal.ZERO))
                .body("presupuesto.disponible", comparesEqualTo(new BigDecimal("800000")))
                .body("presupuesto.excedido", is(false));
    }

    @Test
    @DisplayName("Sin mes usa el actual y sus cifras cuadran con el resumen de movimientos")
    void mesActualCuadra() {
        YearMonth mes = YearMonth.now();
        BigDecimal gastosResumen = given()
                .when().get("/movimientos/resumen?desde=%s&hasta=%s"
                        .formatted(mes.atDay(1), mes.atEndOfMonth()))
                .then().statusCode(200)
                .extract().path("totalGastos");

        var respuesta = given().when().get("/reportes/mensual")
                .then().statusCode(200)
                .body("mes", is(mes.toString()))
                .extract();

        BigDecimal total = respuesta.path("totalGastado");
        List<Map<String, Object>> categorias = respuesta.path("gastosPorCategoria");
        BigDecimal sumaCategorias = categorias.stream()
                .map(c -> (BigDecimal) c.get("total"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, gastosResumen.compareTo(total));
        assertEquals(0, sumaCategorias.compareTo(total));

        // Si alguna prueba ya definio presupuesto para este mes, usa el mismo total
        BigDecimal gastadoPresupuesto = respuesta.path("presupuesto.totalGastado");
        if (gastadoPresupuesto != null) {
            assertEquals(0, gastadoPresupuesto.compareTo(total));
        }
    }
}
