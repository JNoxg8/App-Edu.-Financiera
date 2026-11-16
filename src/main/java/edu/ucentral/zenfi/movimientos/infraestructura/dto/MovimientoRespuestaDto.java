package edu.ucentral.zenfi.movimientos.infraestructura.dto;

import edu.ucentral.zenfi.movimientos.dominio.modelo.Movimiento;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimiento que se devuelve a Flutter.
 *
 * abonoDeuda = true le indica a la app que ese gasto viene de una deuda:
 * debe bloquear monto, fecha y categoria en el formulario de edicion.
 * aporteMeta = true significa lo mismo para un aporte a una meta de ahorro.
 */
public record MovimientoRespuestaDto(
        Long id,
        TipoMovimiento tipo,
        BigDecimal monto,
        LocalDate fecha,
        String descripcion,
        CategoriaRespuestaDto categoria,
        Long deudaId,
        boolean abonoDeuda,
        Long metaAhorroId,
        boolean aporteMeta
) {
    public static MovimientoRespuestaDto desde(Movimiento m) {
        return new MovimientoRespuestaDto(
                m.getId(),
                m.getTipo(),
                m.getMonto(),
                m.getFecha(),
                m.getDescripcion(),
                CategoriaRespuestaDto.desde(m.getCategoria()),
                m.getDeudaId(),
                m.esAbonoDeDeuda(),
                m.getMetaAhorroId(),
                m.esAporteAMeta());
    }
}
