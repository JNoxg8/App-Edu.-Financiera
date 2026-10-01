package edu.ucentral.zenfi.movimientos.infraestructura.dto;

import edu.ucentral.zenfi.movimientos.dominio.modelo.Categoria;
import edu.ucentral.zenfi.movimientos.dominio.modelo.TipoMovimiento;

/**
 * Categoria que se devuelve a Flutter.
 *
 * requiereDeuda = true (solo "Deudas") le indica a la app que, al
 * elegirla, debe mostrar el selector de la deuda a la que se abona.
 */
public record CategoriaRespuestaDto(
        Long id,
        String nombre,
        TipoMovimiento tipo,
        boolean predefinida,
        boolean requiereDeuda
) {
    public static CategoriaRespuestaDto desde(Categoria c) {
        return new CategoriaRespuestaDto(
                c.getId(),
                c.getNombre(),
                c.getTipo(),
                c.isPredefinida(),
                c.esDeDeudas());
    }
}
