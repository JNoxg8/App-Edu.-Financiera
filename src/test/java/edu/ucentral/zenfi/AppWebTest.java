package edu.ucentral.zenfi;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * La app web de Flutter (src/main/resources/META-INF/resources) se sirve
 * desde el mismo servidor en /zenfi/. Es publica: el inicio de sesion
 * ocurre dentro de la app, y la API sigue exigiendo el token.
 */
@QuarkusTest
class AppWebTest {

    @BeforeEach
    void sinSesion() {
        RestAssured.requestSpecification = null;
    }

    @Test
    @DisplayName("La pagina de la app y su codigo cargan sin token")
    void appWebPublica() {
        given().when().get("/")
                .then().statusCode(200)
                .body(containsString("<title>Zenfi</title>"))
                .body(containsString("<base href=\"/zenfi/\">"));
        given().when().get("/main.dart.js").then().statusCode(200);
        given().when().get("/flutter_bootstrap.js").then().statusCode(200);
    }

    @Test
    @DisplayName("La API sigue protegida aunque la app web sea publica")
    void apiProtegida() {
        given().when().get("/deudas").then().statusCode(401);
    }
}
