package edu.ucentral.zenfi.movimientos.infraestructura.eventos;

import edu.ucentral.zenfi.metasahorro.dominio.evento.AporteEliminado;
import edu.ucentral.zenfi.metasahorro.dominio.evento.AporteRegistrado;
import edu.ucentral.zenfi.movimientos.aplicacion.MovimientoServicio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Adaptador de entrada: escucha los eventos del modulo de metas de ahorro
 * y mantiene los gastos sincronizados con los aportes (igual que
 * EventosDeudaObservador con los abonos).
 *
 *   AporteRegistrado -> se crea un gasto en la categoria "Metas de ahorro"
 *   AporteEliminado  -> se borra ese gasto (tambien al eliminar la meta)
 *
 * @Observes es sincrono: corre en la misma transaccion que el aporte. Si
 * crear el gasto falla, el aporte tampoco queda guardado.
 */
@ApplicationScoped
public class EventosMetaObservador {

    @Inject
    MovimientoServicio servicio;

    void alRegistrarAporte(@Observes AporteRegistrado evento) {
        servicio.registrarGastoDeAporte(evento.usuarioId(), evento.metaAhorroId(),
                evento.aporteId(), evento.nombreMeta(), evento.monto(), evento.fecha());
    }

    void alEliminarAporte(@Observes AporteEliminado evento) {
        servicio.eliminarGastoDeAporte(evento.aporteId());
    }
}
