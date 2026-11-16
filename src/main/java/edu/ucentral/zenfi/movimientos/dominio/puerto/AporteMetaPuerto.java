package edu.ucentral.zenfi.movimientos.dominio.puerto;

/**
 * Puerto de salida hacia el modulo de metas de ahorro.
 *
 * Igual que PagoDeudaPuerto: movimientos declara QUE necesita (revertir
 * un aporte) sin saber COMO lo hace metas. Lo implementa
 * AporteMetaAdaptador llamando a AporteMetaAhorroServicio.
 */
public interface AporteMetaPuerto {

    /** Elimina el aporte: el ahorro acumulado de la meta vuelve a bajar. */
    void revertirAporte(Long metaAhorroId, Long aporteId);
}
