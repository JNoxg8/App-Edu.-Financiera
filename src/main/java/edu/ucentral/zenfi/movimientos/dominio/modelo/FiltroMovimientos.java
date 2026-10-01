package edu.ucentral.zenfi.movimientos.dominio.modelo;

import edu.ucentral.zenfi.common.excepcion.ReglaNegocioException;

import java.time.LocalDate;

/**
 * Criterios para listar movimientos: filtrar, buscar y ordenar.
 * Todos los filtros son opcionales (null = sin filtrar por ese campo).
 *
 * @param texto busca en la descripcion y en el nombre de la categoria
 */
public record FiltroMovimientos(
        Long usuarioId,
        TipoMovimiento tipo,
        Long categoriaId,
        String texto,
        LocalDate desde,
        LocalDate hasta,
        Orden orden,
        Direccion direccion
) {

    /** Campo por el que se ordena el listado. */
    public enum Orden { FECHA, MONTO, CATEGORIA }

    public enum Direccion { ASC, DESC }

    public FiltroMovimientos {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException(
                    "La fecha inicial (" + desde + ") no puede ser posterior a la final (" + hasta + ")");
        }
        texto = (texto == null || texto.isBlank()) ? null : texto.trim();
        orden = (orden == null) ? Orden.FECHA : orden;
        direccion = (direccion == null) ? Direccion.DESC : direccion;
    }

    /** Solo por usuario y rango de fechas: lo usa el resumen. */
    public static FiltroMovimientos porPeriodo(Long usuarioId, LocalDate desde, LocalDate hasta) {
        return new FiltroMovimientos(usuarioId, null, null, null, desde, hasta, null, null);
    }
}
