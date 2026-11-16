package edu.ucentral.zenfi.autenticacion;

import edu.ucentral.zenfi.autenticacion.infraestructura.persistencia.CuentaEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de integracion de /auth y del filtro que exige el token. */
@QuarkusTest
class AutenticacionRecursoTest {

    /** Estas pruebas manejan el token a mano: sin encabezado por defecto. */
    @BeforeEach
    void sinSesion() {
        RestAssured.requestSpecification = null;
    }

    private String credenciales(String correo, String contrasena) {
        return """
                {"correo": "%s", "contrasena": "%s"}
                """.formatted(correo, contrasena);
    }

    private String registrar(String correo, String contrasena) {
        return given().contentType(ContentType.JSON).body(credenciales(correo, contrasena))
                .when().post("/auth/registro")
                .then().statusCode(201)
                .body("token", notNullValue())
                .body("correo", is(correo.trim().toLowerCase()))
                .extract().path("token");
    }

    @Test
    @DisplayName("Registro: el correo se guarda en minusculas y se puede iniciar sesion con el")
    void registroEInicioDeSesion() {
        String token = registrar("  Camila.Ruiz@Mail.com ", "clave-segura-1");

        given().header("Authorization", "Bearer " + token)
                .when().get("/auth/yo")
                .then().statusCode(200)
                .body("correo", is("camila.ruiz@mail.com"));

        given().contentType(ContentType.JSON).body(credenciales("camila.ruiz@mail.com", "clave-segura-1"))
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    @DisplayName("No se puede registrar dos veces el mismo correo, ni una contrasena corta o un correo invalido")
    void registroInvalido() {
        registrar("repetido@mail.com", "clave-segura-1");

        given().contentType(ContentType.JSON).body(credenciales("REPETIDO@mail.com", "otra-clave-1"))
                .when().post("/auth/registro")
                .then().statusCode(409);
        given().contentType(ContentType.JSON).body(credenciales("corta@mail.com", "1234567"))
                .when().post("/auth/registro")
                .then().statusCode(409);
        given().contentType(ContentType.JSON).body(credenciales("sin-arroba", "clave-segura-1"))
                .when().post("/auth/registro")
                .then().statusCode(409);
    }

    @Test
    @DisplayName("Contrasena incorrecta o correo inexistente: 401 con el mismo mensaje")
    void credencialesIncorrectas() {
        registrar("david@mail.com", "clave-segura-1");

        String malaClave = given().contentType(ContentType.JSON)
                .body(credenciales("david@mail.com", "clave-equivocada"))
                .when().post("/auth/login")
                .then().statusCode(401)
                .body("error.codigo", is("AUT-401"))
                .extract().path("mensaje");
        String noExiste = given().contentType(ContentType.JSON)
                .body(credenciales("nadie@mail.com", "clave-segura-1"))
                .when().post("/auth/login")
                .then().statusCode(401)
                .extract().path("mensaje");

        // Mismo mensaje: la respuesta no revela que correos tienen cuenta
        assertTrue(malaClave.equals(noExiste));
    }

    @Test
    @DisplayName("Sin token o con un token inventado, la API responde 401; la version es publica")
    void endpointsProtegidos() {
        given().when().get("/deudas").then().statusCode(401).body("error.codigo", is("AUT-401"));
        given().header("Authorization", "Bearer inventado")
                .when().get("/panel").then().statusCode(401);
        given().header("Authorization", "Basic abc")
                .when().get("/metas-ahorro").then().statusCode(401);
        given().when().get("/genericos/version").then().statusCode(200);
    }

    @Test
    @DisplayName("Al cerrar sesion el token deja de servir")
    void cerrarSesion() {
        String token = registrar("elena@mail.com", "clave-segura-1");
        given().header("Authorization", "Bearer " + token)
                .when().get("/deudas").then().statusCode(200);

        given().header("Authorization", "Bearer " + token)
                .when().post("/auth/logout").then().statusCode(204);

        given().header("Authorization", "Bearer " + token)
                .when().get("/deudas").then().statusCode(401);
    }

    @Test
    @DisplayName("La contrasena se guarda cifrada con BCrypt, nunca en texto plano")
    void contrasenaCifrada() {
        registrar("fabio@mail.com", "clave-segura-1");

        String hash = QuarkusTransaction.requiringNew().call(() ->
                CuentaEntity.<CuentaEntity>find("correo", "fabio@mail.com").firstResult().hashContrasena);

        assertNotEquals("clave-segura-1", hash);
        assertTrue(hash.startsWith("$2"), "debe ser un hash BCrypt: " + hash);
    }
}
