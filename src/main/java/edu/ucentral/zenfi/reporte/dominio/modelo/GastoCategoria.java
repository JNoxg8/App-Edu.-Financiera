package edu.ucentral.zenfi.reporte.dominio.modelo;

import java.math.BigDecimal;

/**
 * Lo gastado en una categoria durante el mes (HU025).
 *
 * El reporte tiene su propio tipo en vez de usar el de movimientos: asi
 * el dominio del reporte no depende de como movimientos arma su resumen.
 *
 * @param porcentaje parte del total gastado del mes que representa esta
 *                   categoria (0 a 100)
 */
public record GastoCategoria(
        Long categoriaId,
        String categoria,
        BigDecimal total,
        BigDecimal porcentaje,
        int cantidadMovimientos
) {
}
