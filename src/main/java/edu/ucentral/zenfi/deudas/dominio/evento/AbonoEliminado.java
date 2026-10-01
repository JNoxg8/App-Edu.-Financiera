package edu.ucentral.zenfi.deudas.dominio.evento;

/** Evento de dominio: se elimino un abono y el saldo de la deuda volvio a subir. */
public record AbonoEliminado(Long deudaId, Long abonoId) {
}
