package edu.ucentral.zenfi.perfil;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Pruebas de integracion contra H2.
 *
 * Como solo existe UN perfil, el orden importa: primero se comprueba que
 * no existe, luego se valida, se crea, se consulta y se intenta duplicar.
 * Por eso se usa @TestMethodOrder.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerfilRecursoTest {

    private static final String PERFIL_VALIDO = """
            {"nombre": "Ana Gomez", "moneda": "COP", "nivel": "BASICO"}
            """;

    @Test
    @Order(1)
    @DisplayName("Antes de crearlo, GET /perfil responde 404")
    void perfilInexistente() {
        given()
                .when().get("/perfil")
                .then()
                .statusCode(404)
                .body("error.codigo", is("REC-404"));
    }

    @Test
    @Order(2)
    @DisplayName("Con campos vacios responde 400 y no guarda el perfil")
    void camposObligatorios() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"nombre\": \"\"}")
                .when().post("/perfil")
                .then()
                .statusCode(400)
                .body("error.codigo", is("VAL-400"))
                .body("error.detalles", hasSize(3));

        given().when().get("/perfil").then().statusCode(404);
    }

    @Test
    @Order(3)
    @DisplayName("Una moneda inexistente responde 400 con formato ResponseApi")
    void monedaInvalida() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"nombre\": \"Ana\", \"moneda\": \"XYZ\", \"nivel\": \"BASICO\"}")
                .when().post("/perfil")
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("error.codigo", is("JSON-400"))
                .body("error.detalles[0]", containsString("COP, USD, EUR"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /perfil crea el perfil y devuelve 201")
    void crearPerfil() {
        given()
                .contentType(ContentType.JSON)
                .body(PERFIL_VALIDO)
                .when().post("/perfil")
                .then()
                .statusCode(201)
                .body("id", is(1))
                .body("nombre", is("Ana Gomez"))
                .body("moneda", is("COP"))
                .body("simboloMoneda", is("$"))
                .body("nivel", is("BASICO"))
                .body("fechaCreacion", notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("Despues de crearlo, GET /perfil lo devuelve")
    void consultarPerfil() {
        given()
                .when().get("/perfil")
                .then()
                .statusCode(200)
                .body("nombre", is("Ana Gomez"))
                .body("nivel", is("BASICO"));
    }

    @Test
    @Order(6)
    @DisplayName("Crear el perfil por segunda vez responde 409")
    void perfilDuplicado() {
        given()
                .contentType(ContentType.JSON)
                .body(PERFIL_VALIDO)
                .when().post("/perfil")
                .then()
                .statusCode(409)
                .body("error.codigo", is("NEG-409"))
                .body("mensaje", containsString("ya fue creado"));
    }
}