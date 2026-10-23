package edu.ucentral.zenfi.deudas.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.deudas.dominio.modelo.Abono;
import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;
import edu.ucentral.zenfi.deudas.dominio.repositorio.DeudaRepositorio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Capa de aplicacion: coordina los casos de uso.
 *
 * Fijate que aqui NO hay calculos financieros. El servicio solo
 * orquesta: busca la deuda, le pide al dominio que valide, y guarda.
 * Los calculos viven en la clase Deuda.
 */
@ApplicationScoped
public class DeudaServicio {

    @Inject
    DeudaRepositorio repositorio;

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
    public Deuda registrarAbono(Long deudaId, BigDecimal monto, LocalDate fecha) {
        Deuda deuda = obtener(deudaId);

        Abono abono = new Abono(null, monto, fecha != null ? fecha : LocalDate.now());

        // El dominio valida: monto positivo, no exceder el saldo, deuda no pagada
        deuda.agregarAbono(abono);

        repositorio.agregarAbono(deudaId, abono);
        return obtener(deudaId);
    }

    public void eliminar(Long id) {
        if (!repositorio.eliminar(id)) {
            throw new RecursoNoEncontradoException("No existe la deuda con id " + id);
        }
    }
}
