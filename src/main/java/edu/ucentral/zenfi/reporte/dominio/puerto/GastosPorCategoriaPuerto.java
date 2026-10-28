package edu.ucentral.zenfi.reporte.dominio.puerto;

import edu.ucentral.zenfi.reporte.dominio.modelo.GastoCategoria;

import java.time.YearMonth;
import java.util.List;

/**
 * Puerto de salida: los gastos de un mes agrupados por categoria.
 * Lo implementa un adaptador que lee el modulo de movimientos.
 */
public interface GastosPorCategoriaPuerto {

    /** Ordenados de la categoria con mas gasto a la de menos. Vacio si no hubo gastos. */
    List<GastoCategoria> gastosPorCategoria(Long usuarioId, YearMonth mes);
}
