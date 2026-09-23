package edu.ucentral.zenfi.genericos;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Reemplaza el GreetingResourceTest de la plantilla, que probaba un
 * endpoint /hello que no existe y por eso fallaba.
 */
@QuarkusTest
class GenericoRecursoTest {

    @Test
    @DisplayName("GET /genericos/version responde la version del aplicativo")
    void version() {
        given()
                .when()
                .get("/genericos/version")
                .then()
                .statusCode(200)
                .body(containsString("Zenfi API"));
    }
}
