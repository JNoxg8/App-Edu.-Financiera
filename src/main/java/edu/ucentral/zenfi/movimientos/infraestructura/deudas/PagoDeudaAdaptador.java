package edu.ucentral.zenfi.movimientos.infraestructura.deudas;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.deudas.aplicacion.DeudaServicio;
import edu.ucentral.zenfi.deudas.dominio.modelo.Deuda;
import edu.ucentral.zenfi.movimientos.dominio.puerto.PagoDeudaPuerto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Adaptador de salida: implementa PagoDeudaPuerto usando el servicio del
 * modulo de deudas.
 *
 * Es el UNICO punto de movimientos que conoce clases de deudas. Si
 * deudas cambia, solo se ajusta esta clase.
 */
@ApplicationScoped
public class PagoDeudaAdaptador implements PagoDeudaPuerto {

    @Inject
    DeudaServicio deudas;

    @Override
    public Long registrarAbono(Long usuarioId, Long deudaId, BigDecimal monto, LocalDate fecha) {
        Deuda deuda = deudas.obtener(deudaId);   // 404 si no existe
        if (!usuarioId.equals(deuda.getUsuarioId())) {
            // Una deuda ajena se trata como inexistente
            throw new RecursoNoEncontradoException("No existe la deuda con id " + deudaId);
        }
        return deudas.abonar(deudaId, monto, fecha).getId();
    }

    @Override
    public void revertirAbono(Long deudaId, Long abonoId) {
        deudas.eliminarAbono(deudaId, abonoId);
    }
}
