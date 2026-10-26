package edu.ucentral.zenfi.presupuesto.infraestructura.dto;

import edu.ucentral.zenfi.presupuesto.dominio.modelo.EstadoPresupuesto;

import java.math.BigDecimal;

/**
 * Presupuesto que se devuelve a Flutter, con lo gastado ya calculado.
 * "excedido" le indica a la app cuando mostrar el porcentaje como
 * advertencia, sin que tenga que comparar montos por su cuenta.
 */
public record PresupuestoRespuestaDto(
        Long id,
        String mes,
        BigDecimal montoLimite,
        BigDecimal totalGastado,
        BigDecimal porcentajeUtilizado,
        BigDecimal disponible,
        boolean excedido
) {
    public static PresupuestoRespuestaDto desde(EstadoPresupuesto e) {
        return new PresupuestoRespuestaDto(
                e.presupuesto().getId(),
                e.presupuesto().getMes().toString(),
                e.presupuesto().getMontoLimite(),
                e.totalGastado(),
                e.porcentajeUtilizado(),
                e.disponible(),
                e.excedido());
    }
}
