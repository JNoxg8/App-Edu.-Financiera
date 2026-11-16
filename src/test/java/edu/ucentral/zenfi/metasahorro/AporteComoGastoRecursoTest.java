package edu.ucentral.zenfi.metasahorro;

import org.junit.jupiter.api.BeforeEach;
import edu.ucentral.zenfi.Sesiones;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.EstadoPresupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.modelo.Presupuesto;
import edu.ucentral.zenfi.presupuesto.dominio.puerto.GastosMensualesPuerto;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integracion metas de ahorro <-> movimientos: cada aporte es un gasto en
 * la categoria "Metas de ahorro", igual que cada abono es un gasto en
 * "Deudas". Por eso descuenta del presupuesto (HU011), aparece en el
 * reporte (HU025) y en el panel (HU015).
 *
 * Todas las clases @QuarkusTest comparten la base H2: se compara la
 * DIFERENCIA antes y despues del aporte, y cada meta tiene un nombre unico.
 */
@QuarkusTest
class AporteComoGastoRecursoTest {

    /** La API exige token: cada prueba usa la cuenta principal de pruebas. */
    @BeforeEach
    void iniciarSesion() {
        Sesiones.usarCuentaPrincipal();
    }

    private static final LocalDate HOY = LocalDate.now();
    private static final YearMonth MES = YearMonth.now();

    @Inject
    GastosMensualesPuerto gastosMensuales;

    @BeforeAll
    static void configurarNumeros() {
        RestAssured.config = RestAssuredConfig.config()
                .jsonConfig(JsonConfig.jsonConfig()
                        .numberReturnType(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
    }

    private int crearMeta(String nombre, String inicial) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"nombre": "%s", "montoObjetivo": 5000000,
                         "fechaLimite": "%s", "montoInicial": %s}
                        """.formatted(nombre, HOY.plusMonths(6), inicial))
                .when().post("/metas-ahorro")
                .then().statusCode(201)
                .extract().path("id");
    }

    private int aportar(int metaId, String monto) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {"monto": %s, "fecha": "%s"}
                        """.formatted(monto, HOY))
                .when().post("/metas-ahorro/%d/aportes".formatted(metaId))
                .then().statusCode(201)
                .extract().path("id");
    }

    private int categoriaMetas() {
        return given().when().get("/categorias?tipo=GASTO")
                .then().statusCode(200)
                .body("find { it.nombre == 'Metas de ahorro' }.automatica", is(true))
                .extract().path("find { it.nombre == 'Metas de ahorro' }.id");
    }

    /** Gastos en "Metas de ahorro" cuya descripcion menciona la meta. */
    private List<Map<String, Object>> gastosDeLaMeta(String nombreMeta) {
        return given()
                .queryParam("categoriaId", categoriaMetas())
                .queryParam("buscar", nombreMeta)
                .when().get("/movimientos")
                .then().statusCode(200)
                .extract().path("");
    }

    private BigDecimal numero(Object valor) {
        return new BigDecimal(valor.toString());
    }

    @Test
    @DisplayName("Caso real: con un presupuesto de 2.000.000, un aporte de 500.000 deja 1.500.000 disponibles")
    void aporteDescuentaDelPresupuesto() {
        // El presupuesto usa el total de gastos del mes (HU011); se arma con
        // ese mismo puerto para no depender de otras pruebas que crean el
        // presupuesto del mes actual en la base compartida
        Long usuario = Sesiones.usuarioId(Sesiones.PRINCIPAL);
        Presupuesto presupuesto = Presupuesto.crear(usuario, MES, new BigDecimal("2000000"), MES);
        EstadoPresupuesto antes = presupuesto.estado(gastosMensuales.totalGastos(usuario, MES));
        BigDecimal reporteAntes = numero(given().get("/reportes/mensual").path("totalGastado"));
        BigDecimal panelAntes = numero(given().get("/panel").path("gastosMesActual"));

        aportar(crearMeta("Moto caso real", "0"), "500000");

        EstadoPresupuesto despues = presupuesto.estado(gastosMensuales.totalGastos(usuario, MES));
        assertEquals(0, new BigDecimal("500000").compareTo(
                antes.disponible().subtract(despues.disponible())));

        // Mismo efecto en el reporte (HU025) y en el panel (HU015)
        BigDecimal reporteDespues = numero(given().get("/reportes/mensual").path("totalGastado"));
        BigDecimal panelDespues = numero(given().get("/panel").path("gastosMesActual"));
        assertEquals(0, new BigDecimal("500000").compareTo(reporteDespues.subtract(reporteAntes)));
        assertEquals(0, new BigDecimal("500000").compareTo(panelDespues.subtract(panelAntes)));
        given().get("/reportes/mensual").then()
                .body("gastosPorCategoria.find { it.categoria == 'Metas de ahorro' }", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    @DisplayName("El gasto del aporte aparece en Movimientos marcado como aporte de la meta")
    void gastoEnMovimientos() {
        int metaId = crearMeta("Viaje gasto visible", "0");
        aportar(metaId, "120000");

        List<Map<String, Object>> gastos = gastosDeLaMeta("Viaje gasto visible");
        assertEquals(1, gastos.size());
        Map<String, Object> gasto = gastos.get(0);
        assertEquals(0, new BigDecimal("120000").compareTo(numero(gasto.get("monto"))));
        assertEquals(true, gasto.get("aporteMeta"));
        assertEquals(metaId, gasto.get("metaAhorroId"));
        assertEquals("Aporte a meta: Viaje gasto visible", gasto.get("descripcion"));
    }

    @Test
    @DisplayName("El monto inicial de la meta NO cuenta como gasto (ya estaba ahorrado)")
    void montoInicialNoEsGasto() {
        crearMeta("Casa inicial", "300000");

        assertEquals(0, gastosDeLaMeta("Casa inicial").size());
    }

    @Test
    @DisplayName("Eliminar el aporte desde Metas borra su gasto")
    void eliminarAporteBorraGasto() {
        int metaId = crearMeta("Portatil borrar aporte", "0");
        int aporteId = aportar(metaId, "80000");
        assertEquals(1, gastosDeLaMeta("Portatil borrar aporte").size());

        given().when().delete("/metas-ahorro/%d/aportes/%d".formatted(metaId, aporteId))
                .then().statusCode(204);

        assertEquals(0, gastosDeLaMeta("Portatil borrar aporte").size());
    }

    @Test
    @DisplayName("Eliminar el gasto desde Movimientos elimina el aporte y baja el ahorro de la meta")
    void eliminarGastoRevierteAporte() {
        int metaId = crearMeta("Bici borrar gasto", "0");
        aportar(metaId, "90000");
        Object gastoId = gastosDeLaMeta("Bici borrar gasto").get(0).get("id");

        given().when().delete("/movimientos/" + gastoId).then().statusCode(204);

        given().when().get("/metas-ahorro/%d/aportes".formatted(metaId))
                .then().statusCode(200).body("", hasSize(0));
        given().when().get("/metas-ahorro")
                .then().body("find { it.id == %d }.montoAcumulado".formatted(metaId),
                        comparesEqualTo(BigDecimal.ZERO));
        assertEquals(0, gastosDeLaMeta("Bici borrar gasto").size());
    }

    @Test
    @DisplayName("Eliminar la meta borra los gastos de sus aportes (el ahorro vuelve a estar disponible)")
    void eliminarMetaBorraGastos() {
        int metaId = crearMeta("Celular borrar meta", "0");
        aportar(metaId, "50000");
        aportar(metaId, "70000");
        assertEquals(2, gastosDeLaMeta("Celular borrar meta").size());

        given().when().delete("/metas-ahorro/" + metaId).then().statusCode(204);

        assertEquals(0, gastosDeLaMeta("Celular borrar meta").size());
    }

    @Test
    @DisplayName("No se puede registrar a mano un gasto en Metas de ahorro (409)")
    void categoriaAutomatica() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"tipo": "GASTO", "monto": 1000, "fecha": "%s", "categoriaId": %d}
                        """.formatted(HOY, categoriaMetas()))
                .when().post("/movimientos")
                .then().statusCode(409)
                .body("error.codigo", is("NEG-409"));
    }

    @Test
    @DisplayName("En el gasto de un aporte se puede cambiar la descripcion, pero no el monto")
    void editarGastoDeAporte() {
        int metaId = crearMeta("Tablet editar", "0");
        aportar(metaId, "60000");
        Object gastoId = gastosDeLaMeta("Tablet editar").get(0).get("id");
        String cuerpo = """
                {"tipo": "GASTO", "monto": %s, "fecha": "%s",
                 "descripcion": "%s", "categoriaId": %d}
                """;

        given().contentType(ContentType.JSON)
                .body(cuerpo.formatted("60000", HOY, "Tablet editar - ahorro de noviembre", categoriaMetas()))
                .when().put("/movimientos/" + gastoId)
                .then().statusCode(200)
                .body("descripcion", is("Tablet editar - ahorro de noviembre"));

        given().contentType(ContentType.JSON)
                .body(cuerpo.formatted("1", HOY, "Tablet editar", categoriaMetas()))
                .when().put("/movimientos/" + gastoId)
                .then().statusCode(409);
    }
}
