package edu.ucentral.zenfi.panel.dominio.modelo;

import java.math.BigDecimal;

/**
 * Lo gastado en una categoria durante el mes y su porcentaje sobre el
 * total gastado. Alimenta el grafico de dona del panel.
 */
public record GastoCategoria(String categoria, BigDecimal total, BigDecimal porcentaje) {
}
