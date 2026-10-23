package edu.ucentral.zenfi.deudas.dominio.evento;

/** Evento de dominio: se elimino una deuda junto con sus abonos. */
public record DeudaEliminada(Long deudaId) {
}
