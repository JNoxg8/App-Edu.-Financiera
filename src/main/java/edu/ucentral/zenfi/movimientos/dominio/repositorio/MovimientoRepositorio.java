package edu.ucentral.zenfi.movimientos.dominio.repositorio;

import edu.ucentral.zenfi.movimientos.dominio.modelo.FiltroMovimientos;
import edu.ucentral.zenfi.movimientos.dominio.modelo.Movimiento;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de los movimientos. Lo implementa MovimientoPanache.
 * Gracias a esta interfaz el servicio se prueba con @InjectMock.
 */
public interface MovimientoRepositorio {

    Movimiento crear(Movimiento movimiento);

    Movimiento actualizar(Movimiento movimiento);

    Optional<Movimiento> buscarPorId(Long id);

    Optional<Movimiento> buscarPorAbono(Long abonoId);

    /** Filtra, busca y ordena segun el filtro. */
    List<Movimiento> listar(FiltroMovimientos filtro);

    long contarPorCategoria(Long categoriaId);

    boolean eliminar(Long id);

    boolean eliminarPorAbono(Long abonoId);

    /** Quita la referencia a la deuda de todos sus gastos. Devuelve cuantos cambio. */
    int desvincularDeuda(Long deudaId);
}
