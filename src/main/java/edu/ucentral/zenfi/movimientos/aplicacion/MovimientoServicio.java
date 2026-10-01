package edu.ucentral.zenfi.movimientos.aplicacion;

import edu.ucentral.zenfi.common.excepcion.RecursoNoEncontradoException;
import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.FiltroMovimientos;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Movimiento;
import edu.ucentral.zenfi.movimientos.dominio.modelo.ResumenMovimientos;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;
import edu.ucentral.zenfi.movimientos.dominio.puerto.PagoDeudaPuerto;
import edu.ucentral.zenfi.movimientos.dominio.repositorio.MovimientoRepositorio;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Casos de uso de ingresos y gastos (HU009).
 *
 * Como en DeudaServicio, aqui no hay reglas: el servicio busca, le pide
 * al dominio que valide y guarda. Las reglas viven en Movimiento.
 *
 * Cruce con deudas: todo abono es un gasto en la categoria "Deudas".
 * La regla se cumple en un solo lugar, el evento AbonoRegistrado: no
 * importa si el abono entra por /deudas/{id}/abonos o por /movimientos,
 * siempre queda un gasto y solo uno.
 */
@ApplicationScoped
public class MovimientoServicio {

    @Inject
    MovimientoRepositorio repositorio;

    @Inject
    CategoriaServicio categorias;

    @Inject
    PagoDeudaPuerto pagoDeuda;

    /**
     * HU009: registrar un ingreso o gasto.
     *
     * Si la categoria es "Deudas" se debe indicar la deuda: se registra
     * el abono (que baja el saldo de la deuda) y el gasto lo crea el
     * evento. Todo va en una sola transaccion: si el abono excede el
     * saldo, no queda ni abono ni gasto.
     */
    @Transactional
    public Movimiento crear(Long usuarioId, TipoMovimiento tipo, BigDecimal monto,
                            LocalDate fecha, String descripcion, Long categoriaId, Long deudaId) {

        Categoria categoria = categorias.obtener(usuarioId, categoriaId);

        if (deudaId == null) {
            Movimiento movimiento = Movimiento.registrar(
                    tipo, monto, fecha, descripcion, categoria, usuarioId);
            return repositorio.crear(movimiento);
        }

        if (tipo != TipoMovimiento.GASTO || !categoria.esDeDeudas()) {
            throw new ReglaNegocioException(
                    "Solo un gasto en la categoria Deudas puede asociarse a una deuda");
        }

        Long abonoId = pagoDeuda.registrarAbono(usuarioId, deudaId, monto, fecha);

        Movimiento gasto = repositorio.buscarPorAbono(abonoId)
                .orElseThrow(() -> new IllegalStateException(
                        "No se genero el gasto del abono " + abonoId));

        if (descripcion != null && !descripcion.isBlank()) {
            gasto.cambiarDescripcion(descripcion);
            gasto = repositorio.actualizar(gasto);
        }
        return gasto;
    }

    @Transactional
    public Movimiento actualizar(Long usuarioId, Long id, TipoMovimiento tipo, BigDecimal monto,
                                 LocalDate fecha, String descripcion, Long categoriaId) {
        Movimiento movimiento = obtener(usuarioId, id);
        Categoria categoria = categorias.obtener(usuarioId, categoriaId);

        movimiento.actualizar(tipo, monto, fecha, descripcion, categoria);

        return repositorio.actualizar(movimiento);
    }

    public Movimiento obtener(Long usuarioId, Long id) {
        return repositorio.buscarPorId(id)
                .filter(m -> usuarioId.equals(m.getUsuarioId()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el movimiento con id " + id));
    }

    /** Filtrar por tipo, categoria y fechas; buscar por texto; ordenar. */
    public List<Movimiento> listar(FiltroMovimientos filtro) {
        return repositorio.listar(filtro);
    }

    public ResumenMovimientos resumen(Long usuarioId, LocalDate desde, LocalDate hasta) {
        List<Movimiento> delPeriodo = repositorio.listar(
                FiltroMovimientos.porPeriodo(usuarioId, desde, hasta));
        return ResumenMovimientos.calcular(desde, hasta, delPeriodo);
    }

    /**
     * Si el gasto es un abono, se revierte el abono en la deuda (su saldo
     * vuelve a subir) y el evento AbonoEliminado borra el gasto.
     */
    @Transactional
    public void eliminar(Long usuarioId, Long id) {
        Movimiento movimiento = obtener(usuarioId, id);

        if (movimiento.esAbonoDeDeuda()) {
            pagoDeuda.revertirAbono(movimiento.getDeudaId(), movimiento.getAbonoId());
            return;
        }
        repositorio.eliminar(id);
    }

    // ------------------------------------------------------------------
    // Reacciones a los eventos del modulo de deudas
    // (las invoca EventosDeudaObservador)
    // ------------------------------------------------------------------

    @Transactional
    public Movimiento registrarGastoDeAbono(Long usuarioId, Long deudaId, Long abonoId,
                                            String acreedor, BigDecimal monto, LocalDate fecha) {
        Movimiento gasto = Movimiento.desdeAbono(usuarioId, deudaId, abonoId, acreedor,
                monto, fecha, categorias.obtenerDeudas());
        Log.debugf("Abono %d de la deuda %d registrado como gasto", abonoId, deudaId);
        return repositorio.crear(gasto);
    }

    @Transactional
    public void eliminarGastoDeAbono(Long abonoId) {
        repositorio.eliminarPorAbono(abonoId);
    }

    /** La deuda se elimino: sus gastos se conservan pero sin referencia. */
    @Transactional
    public void desvincularDeuda(Long deudaId) {
        Integer cambiados = repositorio.desvincularDeuda(deudaId);
        Log.debugf("Deuda %d eliminada: %d gasto(s) desvinculados", deudaId, cambiados);
    }
}
