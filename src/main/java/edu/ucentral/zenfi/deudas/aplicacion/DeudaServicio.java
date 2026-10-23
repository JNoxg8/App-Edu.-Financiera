package edu.ucentral.zenfi.deudas.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.deudas.dominio.evento.AbonoEliminado;
import edu.ucentral.zenfi.deudas.dominio.evento.AbonoRegistrado;
import edu.ucentral.zenfi.deudas.dominio.evento.DeudaEliminada;
import edu.ucentral.zenfi.deudas.dominio.modelo.Abono;
import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;
import edu.ucentral.zenfi.deudas.dominio.repositorio.DeudaRepositorio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Capa de aplicacion: coordina los casos de uso.
 *
 * Fijate que aqui NO hay calculos financieros. El servicio solo
 * orquesta: busca la deuda, le pide al dominio que valide, y guarda.
 * Los calculos viven en la clase Deuda.
 *
 * HU009: cuando cambian los abonos se publica un evento (AbonoRegistrado,
 * AbonoEliminado, DeudaEliminada). Este modulo no sabe quien los escucha;
 * hoy los escucha movimientos para reflejar los abonos como gastos. Los
 * observadores corren en la misma transaccion: si fallan, el abono
 * tampoco se guarda.
 */
@ApplicationScoped
public class DeudaServicio {

    @Inject
    DeudaRepositorio repositorio;

    @Inject
    Event<AbonoRegistrado> abonoRegistrado;

    @Inject
    Event<AbonoEliminado> abonoEliminado;

    @Inject
    Event<DeudaEliminada> deudaEliminada;

    /** RQ-07: registrar una deuda activa. */
    public Deuda crear(Deuda deuda) {
        return repositorio.crear(deuda);
    }

    /** RQ-22: modificar una deuda registrada. */
    public Deuda actualizar(Long id, String acreedor, BigDecimal tasa,
                            int numeroCuotas, LocalDate fechaVencimiento) {

        Deuda deuda = obtener(id);   // lanza 404 si no existe

        // El dominio valida la coherencia antes de aplicar los cambios
        deuda.actualizarDatos(acreedor, tasa, numeroCuotas, fechaVencimiento);

        repositorio.actualizar(deuda);
        return obtener(id);
    }

    /** Lista las deudas del usuario, cada una con su saldo calculado. */
    public List<Deuda> listar(Long usuarioId) {
        return repositorio.listarPorUsuario(usuarioId);
    }

    /** Busca una deuda o lanza 404 si no existe. */
    public Deuda obtener(Long id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la deuda con id " + id));
    }

    /**
     * RQ-07: registrar un abono. El saldo pendiente queda actualizado
     * automaticamente porque se recalcula en cada consulta.
     */
    @Transactional
    public Deuda registrarAbono(Long deudaId, BigDecimal monto, LocalDate fecha) {
        abonar(deudaId, monto, fecha);
        return obtener(deudaId);
    }

    /**
     * Igual que registrarAbono, pero devuelve el abono creado (con su id).
     * Lo usa movimientos para enlazar el gasto con el abono.
     */
    @Transactional
    public Abono abonar(Long deudaId, BigDecimal monto, LocalDate fecha) {
        Deuda deuda = obtener(deudaId);

        Abono abono = new Abono(null, monto, fecha != null ? fecha : LocalDate.now());

        // El dominio valida: monto positivo, no exceder el saldo, deuda no pagada
        deuda.agregarAbono(abono);

        repositorio.agregarAbono(deudaId, abono);

        abonoRegistrado.fire(new AbonoRegistrado(deudaId, abono.getId(), deuda.getUsuarioId(),
                deuda.getAcreedor(), abono.getMonto(), abono.getFecha()));
        return abono;
    }

    /** Elimina un abono: el saldo pendiente vuelve a subir en ese valor. */
    @Transactional
    public Deuda eliminarAbono(Long deudaId, Long abonoId) {
        Deuda deuda = obtener(deudaId);

        deuda.quitarAbono(abonoId);   // lanza 404 si el abono no es de esta deuda

        repositorio.eliminarAbono(deudaId, abonoId);
        abonoEliminado.fire(new AbonoEliminado(deudaId, abonoId));
        return obtener(deudaId);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repositorio.eliminar(id)) {
            throw new RecursoNoEncontradoException("No existe la deuda con id " + id);
        }
        deudaEliminada.fire(new DeudaEliminada(id));
    }
}
