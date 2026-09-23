package edu.ucentral.zenfi.common.infraestructura;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Detalle tecnico del error: codigo interno (VAL-400, REC-404, NEG-409)
 * y, para los errores de validacion, la lista de campos invalidos.
 */
@Data
@Builder
public class ResponseApiError {
    private String mensaje;
    private String codigo;
    private List<String> detalles;
}
