package edu.ucentral.zenfi;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;

/**
 * Cuentas para las pruebas de integracion. La API exige un token, asi que
 * cada clase @QuarkusTest llama a usarCuentaPrincipal() antes de cada prueba:
 * todas las peticiones de RestAssured salen con el token de esa cuenta.
 *
 * Los tokens se guardan para no registrar la misma cuenta dos veces: todas
 * las clases comparten la misma aplicacion y la misma base H2.
 */
public final class Sesiones {

    public static final String PRINCIPAL = "pruebas@zenfi.co";
    public static final String CONTRASENA = "clave-de-pruebas";

    private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();
    private static final Map<String, Long> IDS = new ConcurrentHashMap<>();

    private Sesiones() {
    }

    /** Todas las peticiones siguientes usan la cuenta principal de pruebas. */
    public static void usarCuentaPrincipal() {
        usar(PRINCIPAL);
    }

    public static void usar(String correo) {
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer " + token(correo))
                .build();
    }

    /** Token de la cuenta: la registra la primera vez y luego reutiliza el token. */
    public static String token(String correo) {
        return TOKENS.computeIfAbsent(correo, Sesiones::registrarOEntrar);
    }

    public static Long usuarioId(String correo) {
        token(correo);
        return IDS.get(correo);
    }

    private static String registrarOEntrar(String correo) {
        String cuerpo = """
                {"correo": "%s", "contrasena": "%s"}
                """.formatted(correo, CONTRASENA);
        Response registro = given().contentType(ContentType.JSON).body(cuerpo)
                .when().post("/auth/registro");
        Response sesion = registro.statusCode() == 201
                ? registro
                : given().contentType(ContentType.JSON).body(cuerpo)
                        .when().post("/auth/login")
                        .then().statusCode(200).extract().response();
        IDS.put(correo, ((Number) sesion.path("usuarioId")).longValue());
        return sesion.path("token");
    }
}
