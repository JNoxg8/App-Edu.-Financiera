package edu.ucentral.zenfi.autenticacion;

import edu.ucentral.zenfi.Sesiones;
import edu.ucentral.zenfi.notificaciones.dominio.repositorio.RecordatorioMetaRepositorio;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Varios usuarios, cada uno con sus datos: Ana registra deudas, metas,
 * movimientos, categorias y su presupuesto; Beto no puede verlos ni
 * modificarlos, ni siquiera conociendo sus ids (responde 404, como si no
 * existieran).
 */
@QuarkusTest
class AislamientoUsuariosRecursoTest {

    private static final String ANA = "ana@zenfi.co";
    private static final String BETO = "beto@zenfi.co";
    private static final LocalDate HOY = LocalDate.now();

    @Inject
    RecordatorioMetaRepositorio recordatorios;

    /** Cada peticion lleva a mano el token de quien la hace. */
    @BeforeEach
    void sinSesionPorDefecto() {
        RestAssured.requestSpecification = null;
    }

    private RequestSpecification como(String correo) {
        return given().header("Authorization", "Bearer " + Sesiones.token(correo))
                .contentType(ContentType.JSON);
    }

    private int crearDeuda(String correo) {
        return como(correo).body("""
                        {"acreedor": "Banco de Ana", "montoTotal": 1000000, "tasaInteresAnual": 0,
                         "numeroCuotas": 10, "fechaVencimiento": "%s"}
                        """.formatted(HOY.plusYears(1)))
                .when().post("/deudas").then().statusCode(201).extract().path("id");
    }

    private int crearMeta(String correo) {
        return como(correo).body("""
                        {"nombre": "Viaje de Ana", "montoObjetivo": 3000000,
                         "fechaLimite": "%s", "montoInicial": 0}
                        """.formatted(HOY.plusMonths(6)))
                .when().post("/metas-ahorro").then().statusCode(201).extract().path("id");
    }

    private int categoriaOtros(String correo) {
        return como(correo).when().get("/categorias?tipo=GASTO").then().statusCode(200)
                .extract().path("find { it.nombre == 'Otros' }.id");
    }

    @Test
    @DisplayName("Deudas: Beto no ve, ni consulta, ni modifica, ni abona, ni elimina la deuda de Ana")
    void deudas() {
        int deuda = crearDeuda(ANA);

        List<Integer> deBeto = como(BETO).when().get("/deudas").then().statusCode(200)
                .extract().path("id");
        assertFalse(deBeto.contains(deuda));

        como(BETO).when().get("/deudas/" + deuda).then().statusCode(404);
        como(BETO).body("""
                        {"acreedor": "Hackeada", "tasaInteresAnual": 0, "numeroCuotas": 1,
                         "fechaVencimiento": "%s"}
                        """.formatted(HOY.plusYears(1)))
                .when().put("/deudas/" + deuda).then().statusCode(404);
        como(BETO).body("""
                        {"monto": 1000, "fecha": "%s"}
                        """.formatted(HOY))
                .when().post("/deudas/%d/abonos".formatted(deuda)).then().statusCode(404);
        como(BETO).when().delete("/deudas/" + deuda).then().statusCode(404);

        // La deuda de Ana sigue intacta
        como(ANA).when().get("/deudas/" + deuda).then().statusCode(200)
                .body("acreedor", is("Banco de Ana"))
                .body("abonos", hasSize(0));
    }

    @Test
    @DisplayName("Metas: Beto no ve la meta de Ana ni toca sus aportes, su recordatorio o la meta")
    void metas() {
        int meta = crearMeta(ANA);
        como(ANA).body("""
                        {"monto": 100000, "fecha": "%s"}
                        """.formatted(HOY))
                .when().post("/metas-ahorro/%d/aportes".formatted(meta)).then().statusCode(201);

        List<Integer> deBeto = como(BETO).when().get("/metas-ahorro").then().statusCode(200)
                .extract().path("id");
        assertFalse(deBeto.contains(meta));

        como(BETO).when().get("/metas-ahorro/%d/aportes".formatted(meta)).then().statusCode(404);
        como(BETO).body("""
                        {"monto": 1000, "fecha": "%s"}
                        """.formatted(HOY))
                .when().post("/metas-ahorro/%d/aportes".formatted(meta)).then().statusCode(404);
        como(BETO).when().get("/metas-ahorro/%d/recordatorio".formatted(meta)).then().statusCode(404);
        como(BETO).body("""
                        {"frecuencia": "DIARIA", "activo": false}
                        """)
                .when().put("/metas-ahorro/%d/recordatorio".formatted(meta)).then().statusCode(404);
        como(BETO).when().delete("/metas-ahorro/" + meta).then().statusCode(404);

        List<Integer> deAna = como(ANA).when().get("/metas-ahorro").then().extract().path("id");
        assertTrue(deAna.contains(meta));
    }

    @Test
    @DisplayName("Movimientos y categorias: Beto no ve ni modifica los de Ana")
    void movimientosYCategorias() {
        int categoria = como(ANA).body("""
                        {"nombre": "Mascotas de Ana", "tipo": "GASTO"}
                        """)
                .when().post("/categorias").then().statusCode(201).extract().path("id");
        int movimiento = como(ANA).body("""
                        {"tipo": "GASTO", "monto": 50000, "fecha": "%s",
                         "descripcion": "Gasto de Ana", "categoriaId": %d}
                        """.formatted(HOY, categoria))
                .when().post("/movimientos").then().statusCode(201).extract().path("id");

        List<Integer> movimientosDeBeto = como(BETO).when().get("/movimientos").then()
                .statusCode(200).extract().path("id");
        assertFalse(movimientosDeBeto.contains(movimiento));
        como(BETO).when().get("/movimientos/" + movimiento).then().statusCode(404);
        como(BETO).body("""
                        {"tipo": "GASTO", "monto": 1, "fecha": "%s", "categoriaId": %d}
                        """.formatted(HOY, categoriaOtros(BETO)))
                .when().put("/movimientos/" + movimiento).then().statusCode(404);
        como(BETO).when().delete("/movimientos/" + movimiento).then().statusCode(404);

        List<Integer> categoriasDeBeto = como(BETO).when().get("/categorias").then()
                .extract().path("id");
        assertFalse(categoriasDeBeto.contains(categoria));
        como(BETO).body("""
                        {"nombre": "Robada"}
                        """)
                .when().put("/categorias/" + categoria).then().statusCode(404);
        como(BETO).when().delete("/categorias/" + categoria).then().statusCode(404);
    }

    @Test
    @DisplayName("Presupuesto, perfil y panel: cada usuario ve solo lo suyo")
    void presupuestoPerfilYPanel() {
        String mes = YearMonth.now().toString();
        como(ANA).body("""
                        {"mes": "%s", "montoLimite": 2000000}
                        """.formatted(mes))
                .when().post("/presupuestos").then().statusCode(201);
        como(ANA).body("""
                        {"nombre": "Ana", "moneda": "COP", "nivel": "BASICO"}
                        """)
                .when().post("/perfil");

        como(BETO).when().get("/presupuestos?mes=" + mes).then().statusCode(404);
        como(BETO).when().get("/perfil").then().statusCode(404);
        // Beto puede crear su propio presupuesto del mismo mes
        como(BETO).body("""
                        {"mes": "%s", "montoLimite": 500000}
                        """.formatted(mes))
                .when().post("/presupuestos").then().statusCode(201);

        // El tipo numerico depende de la configuracion de RestAssured: se compara como BigDecimal
        Object limiteAna = como(ANA).when().get("/panel").then().statusCode(200)
                .extract().path("presupuesto.montoLimite");
        Object limiteBeto = como(BETO).when().get("/panel").then().statusCode(200)
                .extract().path("presupuesto.montoLimite");
        assertTrue(new BigDecimal("2000000").compareTo(new BigDecimal(limiteAna.toString())) == 0);
        assertTrue(new BigDecimal("500000").compareTo(new BigDecimal(limiteBeto.toString())) == 0);
    }

    @Test
    @DisplayName("Notificaciones: el recordatorio de Ana solo le llega a Ana")
    void notificaciones() {
        int meta = crearMeta(ANA);
        QuarkusTransaction.requiringNew().run(() ->
                recordatorios.buscarPorMetaAhorroId((long) meta).orElseThrow()
                        .actualizarProximoRecordatorio(HOY.minusDays(1)));

        int notificacion = como(ANA).when().get("/notificaciones").then().statusCode(200)
                .extract().path("find { it.metaAhorroId == %d }.id".formatted(meta));

        List<Integer> deBeto = como(BETO).when().get("/notificaciones").then().statusCode(200)
                .extract().path("id");
        assertFalse(deBeto.contains(notificacion));
        como(BETO).when().patch("/notificaciones/%d/leida".formatted(notificacion))
                .then().statusCode(404);
    }
}
