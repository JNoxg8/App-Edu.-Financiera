package edu.ucentral.zenfi.common.infraestructura;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Formato estandar de respuesta de la API, tomado de la plantilla
 * gestortecno del profesor.
 *
 * Todos los errores (400, 404, 409) salen con esta misma forma, asi el
 * cliente Flutter solo necesita saber leer una estructura:
 *
 * {
 *   "mensaje":   "El abono excede el saldo pendiente, que es 100000.00",
 *   "codigo":    "Regla de negocio",
 *   "timestamp": "2026-09-22T15:04:05Z",
 *   "path":      "/deudas/1/abonos",
 *   "status":    409,
 *   "success":   false,
 *   "error": {
 *     "mensaje":  "El abono excede el saldo pendiente, que es 100000.00",
 *     "codigo":   "NEG-409",
 *     "detalles": []
 *   }
 * }
 *
 * Diferencia con la plantilla: el campo se llama "success" (en la
 * plantilla aparece como "succes", que es un error de escritura).
 */
@Data
@NoArgsConstructor
public class ResponseApi {

    private String mensaje;
    private String codigo;
    private String timestamp;
    private String path;
    private int status;
    private boolean success;
    private ResponseApiError error;

    public ResponseApi(String mensaje, String codigo) {
        this.mensaje = mensaje;
        this.codigo = codigo;
        this.timestamp = Instant.now().toString();
    }

    /**
     * Arma una respuesta de error completa. La usan los ExceptionMapper
     * de common/excepcion para no repetir el mismo codigo en cada uno.
     */
    public static ResponseApi deError(int status, String codigo, String mensaje,
                                      String path, ResponseApiError error) {
        ResponseApi respuesta = new ResponseApi(mensaje, codigo);
        respuesta.setStatus(status);
        respuesta.setPath(path);
        respuesta.setSuccess(false);
        respuesta.setError(error);
        return respuesta;
    }
}
