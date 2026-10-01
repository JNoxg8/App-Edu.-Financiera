package edu.ucentral.zenfi.movimientos;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/** Test de INTEGRACION de /categorias con H2 en memoria. */
@QuarkusTest
class CategoriaRecursoTest {

    private String crear(String nombre, String tipo) {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        { "nombre": "%s", "tipo": "%s" }
                        """.formatted(nombre, tipo))
                .when()
                .post("/categorias")
                .then()
                .statusCode(201)
                .extract()
                .path("id")
                .toString();
    }

    @Test
    @DisplayName("GET /categorias?tipo=GASTO trae las predefinidas de HU009 y Deudas")
    void predefinidas() {
        given()
                .when()
                .get("/categorias?tipo=GASTO")
                .then()
                .statusCode(200)
                .body("nombre", hasItems("Alimentación", "Transporte", "Entretenimiento", "Otros", "Deudas"))
                .body("tipo", everyItem(is("GASTO")))
                .body("find { it.nombre == 'Deudas' }.requiereDeuda", is(true))
                .body("find { it.nombre == 'Transporte' }.requiereDeuda", is(false));
    }

    @Test
    @DisplayName("POST /categorias crea una personalizada")
    void crearPersonalizada() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "nombre": "Mascotas", "tipo": "GASTO" }
                        """)
                .when()
                .post("/categorias")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("nombre", is("Mascotas"))
                .body("predefinida", is(false));
    }

    @Test
    @DisplayName("POST /categorias con nombre vacio devuelve 400")
    void nombreVacio() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "nombre": "  ", "tipo": "GASTO" }
                        """)
                .when()
                .post("/categorias")
                .then()
                .statusCode(400)
                .body("error.codigo", is("VAL-400"));
    }

    @Test
    @DisplayName("POST /categorias con nombre repetido (sin tildes ni mayusculas) devuelve 409")
    void nombreRepetido() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "nombre": "ALIMENTACION", "tipo": "GASTO" }
                        """)
                .when()
                .post("/categorias")
                .then()
                .statusCode(409)
                .body("error.codigo", is("NEG-409"))
                .body("mensaje", containsString("Ya existe"));
    }

    @Test
    @DisplayName("PUT /categorias/{id} renombra y DELETE la elimina")
    void renombrarYEliminar() {
        String id = crear("Gimnasio", "GASTO");

        given()
                .contentType(ContentType.JSON)
                .body("""
                        { "nombre": "Deporte" }
                        """)
                .when()
                .put("/categorias/" + id)
                .then()
                .statusCode(200)
                .body("nombre", is("Deporte"));

        given().when().delete("/categorias/" + id).then().statusCode(204);
    }

    @Test
    @DisplayName("Una categoria predefinida no se puede eliminar (409)")
    void predefinidaNoSeElimina() {
        String id = given().when().get("/categorias?tipo=GASTO")
                .then().extract().path("find { it.nombre == 'Transporte' }.id").toString();

        given().when().delete("/categorias/" + id)
                .then()
                .statusCode(409)
                .body("mensaje", containsString("predefinida"));
    }

    @Test
    @DisplayName("GET /categorias?tipo=XYZ devuelve 400")
    void tipoInvalido() {
        given().when().get("/categorias?tipo=XYZ")
                .then()
                .statusCode(400)
                .body("error.codigo", is("VAL-400"));
    }
}
