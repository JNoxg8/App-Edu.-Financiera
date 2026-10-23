package edu.ucentral.zenfi.movimientos.infraestructura.eventos;

import edu.ucentral.zenfi.deudas.dominio.evento.AbonoEliminado;
import edu.ucentral.zenfi.deudas.dominio.evento.AbonoRegistrado;
import edu.ucentral.zenfi.deudas.dominio.evento.DeudaEliminada;
import edu.ucentral.zenfi.movimientos.aplicacion.MovimientoServicio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Adaptador de entrada: escucha los eventos del modulo de deudas
 * (patron Observer) y mantiene los gastos sincronizados con los abonos.
 *
 *   AbonoRegistrado -> se crea un gasto en la categoria "Deudas"
 *   AbonoEliminado  -> se borra ese gasto
 *   DeudaEliminada  -> sus gastos se conservan, pero sin referencia
 *
 * @Observes es sincrono: corre en el mismo hilo y en la misma
 * transaccion que el abono. Si crear el gasto falla, el abono tampoco
 * queda guardado, y nunca hay un abono sin su gasto.
 */
@ApplicationScoped
public class EventosDeudaObservador {

    @Inject
    MovimientoServicio servicio;

    void alRegistrarAbono(@Observes AbonoRegistrado evento) {
        servicio.registrarGastoDeAbono(evento.usuarioId(), evento.deudaId(), evento.abonoId(),
                evento.acreedor(), evento.monto(), evento.fecha());
    }

    void alEliminarAbono(@Observes AbonoEliminado evento) {
        servicio.eliminarGastoDeAbono(evento.abonoId());
    }

    void alEliminarDeuda(@Observes DeudaEliminada evento) {
        servicio.desvincularDeuda(evento.deudaId());
    }
}
