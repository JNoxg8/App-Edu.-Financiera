package edu.ucentral.zenfi.metasahorro.dominio.evento;

/**
 * Evento de dominio: se elimino un aporte (directamente o porque se
 * elimino su meta). Movimientos lo escucha para borrar el gasto que lo
 * reflejaba.
 */
public record AporteEliminado(Long metaAhorroId, Long aporteId) {
}
